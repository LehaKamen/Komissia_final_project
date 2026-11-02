package com.example.pokeplanner

import androidx.lifecycle.SavedStateHandle
import com.example.pokeplanner.data.local.CatalogIndexEntity
import com.example.pokeplanner.model.PokemonStatus
import com.example.pokeplanner.ui.catalog.MainVm
import com.example.pokeplanner.work.SyncController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainVmTest {
    @Test
    fun unmatchedQueryProducesEmptyScreenWithoutErrorOrLoading() = withControllerTest {
        val fixture = RepositoryFixture()
        fixture.index.value = listOf(CatalogIndexEntity(25, "Pikachu"))
        val settings = FakeSettings()
        val vm = MainVm(SavedStateHandle(), fixture.repository, settings, controller(fixture, settings))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect { } }
        vm.search("missing")
        advanceTimeBy(351)
        runCurrent()
        assertTrue(vm.state.value.items.isEmpty())
        assertFalse(vm.state.value.loading)
        assertNull(vm.state.value.error)
    }

    @get:Rule val main = MainDispatcherRule()
    private val controllers = mutableListOf<SyncController>()

    private fun controller(fixture: RepositoryFixture, settings: FakeSettings) =
        SyncController(fixture.repository, settings, main.dispatcher).also { controllers += it }

    private fun withControllerTest(block: suspend kotlinx.coroutines.test.TestScope.() -> Unit) =
        runTest(main.dispatcher) {
            try {
                block()
            } finally {
                controllers.forEach(SyncController::close)
            }
        }

    @After fun close() {
        controllers.forEach(SyncController::close)
    }

    @Test
    fun initialFailureThenRetryEmitsLoadingErrorLoadingSuccess() = withControllerTest {
        val fixture = RepositoryFixture()
        val settings = FakeSettings()
        val sync = controller(fixture, settings)
        val vm = MainVm(SavedStateHandle(), fixture.repository, settings, sync)
        val emissions = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.state.map {
                when {
                    it.loading -> "loading"
                    it.error != null -> "error"
                    it.items.isNotEmpty() -> "success"
                    else -> "empty"
                }
            }.distinctUntilChanged().collect(emissions::add)
        }
        fixture.api.offline = true
        sync.setForeground(true)
        advanceTimeBy(351)
        runCurrent()
        assertEquals(listOf("loading", "error"), emissions)
        fixture.api.offline = false
        vm.retry()
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(listOf("loading", "error", "loading", "success"), emissions)
        assertEquals(2, fixture.api.listCalls)
    }

    @Test
    fun fastTypingSuppressesStaleQueriesAndDuplicateEmissions() = withControllerTest {
        val fixture = RepositoryFixture()
        fixture.index.value = listOf(CatalogIndexEntity(25, "Pikachu"), CatalogIndexEntity(150, "Mewtwo"))
        val settings = FakeSettings()
        val vm = MainVm(SavedStateHandle(), fixture.repository, settings, controller(fixture, settings))
        val lists = mutableListOf<List<Int>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.state.map { it.items.map { row -> row.id } }.distinctUntilChanged().collect(lists::add)
        }
        advanceTimeBy(351)
        vm.search("pika")
        advanceTimeBy(100)
        vm.search("mew")
        advanceTimeBy(351)
        runCurrent()
        vm.search("mew")
        advanceTimeBy(351)
        runCurrent()
        assertEquals(listOf(emptyList<Int>(), listOf(25, 150), listOf(150)), lists)
    }

    @Test
    fun queryRestoresTogetherWithFilteredResultsFromSavedState() = withControllerTest {
        val fixture = RepositoryFixture()
        fixture.index.value = listOf(CatalogIndexEntity(25, "Pikachu"), CatalogIndexEntity(150, "Mewtwo"))
        val settings = FakeSettings()
        val vm =
            MainVm(
                SavedStateHandle(mapOf("query" to "mew")),
                fixture.repository,
                settings,
                controller(fixture, settings),
            )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect { } }
        advanceTimeBy(351)
        runCurrent()
        assertEquals("mew", vm.query.value)
        assertEquals(listOf(150), vm.state.value.items.map { it.id })
    }

    @Test
    fun roomNoteStatusAndSettingsChangesAutomaticallyRecomputeCatalog() = withControllerTest {
        val fixture = RepositoryFixture()
        fixture.index.value = listOf(CatalogIndexEntity(25, "Pikachu"), CatalogIndexEntity(150, "Mewtwo"))
        val settings = FakeSettings()
        val vm = MainVm(SavedStateHandle(), fixture.repository, settings, controller(fixture, settings))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect { } }
        advanceTimeBy(351)
        settings.planned(true)
        runCurrent()
        assertTrue(vm.state.value.items.isEmpty())
        fixture.repository.save(150, PokemonStatus.PLANNED, "Заметка")
        runCurrent()
        assertEquals(listOf(150), vm.state.value.items.map { it.id })
        fixture.repository.save(150, PokemonStatus.READY, "Готов")
        runCurrent()
        assertTrue(vm.state.value.items.isEmpty())
    }

    @Test
    fun activeApplicationChecksTtlAndRespondsToChangedPreference() = withControllerTest {
        val fixture = RepositoryFixture()
        val settings = FakeSettings()
        val sync = controller(fixture, settings)
        sync.setForeground(true)
        advanceTimeBy(1_000)
        assertEquals(1, fixture.api.listCalls)
        fixture.now += 7 * 3_600_000L
        advanceTimeBy(60_000)
        assertEquals(1, fixture.api.listCalls)
        settings.hours(6)
        advanceTimeBy(1_000)
        assertEquals(2, fixture.api.listCalls)
        fixture.now += 7 * 3_600_000L
        advanceTimeBy(60_000)
        assertEquals(3, fixture.api.listCalls)
        sync.setForeground(false)
        fixture.now += 7 * 3_600_000L
        advanceTimeBy(120_000)
        assertEquals(3, fixture.api.listCalls)
    }
}
