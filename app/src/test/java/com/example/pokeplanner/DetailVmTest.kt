package com.example.pokeplanner

import androidx.lifecycle.SavedStateHandle
import com.example.pokeplanner.ui.detail.DetailVm
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetailVmTest {
    @Test
    fun savingNoteDoesNotHideFailedImageLoadOrItsRetryState() = runTest(main.dispatcher) {
        val fixture = RepositoryFixture()
        fixture.cards.value = listOf(
            com.example.pokeplanner.data.local.PokemonEntity(25, "Pikachu", "https://image", "electric", 1),
        )
        fixture.images.failedIds += 25
        val viewModel = DetailVm(SavedStateHandle(mapOf("id" to 25)), fixture.repository)
        runCurrent()
        val error = viewModel.load.value.error
        assertNotNull(error)

        viewModel.save(com.example.pokeplanner.model.PokemonStatus.PLANNED, "Офлайн-заметка")
        runCurrent()

        assertEquals(error, viewModel.load.value.error)
        assertEquals("Офлайн-заметка", fixture.personal.value.single().note)
    }

    @get:Rule val main = MainDispatcherRule()

    @Test
    fun failedLoadHasRetryStateAndHistoryAppearsOnlyAfterSuccessfulRetry() = runTest(main.dispatcher) {
        val fixture = RepositoryFixture()
        fixture.api.offline = true
        val viewModel = DetailVm(SavedStateHandle(mapOf("id" to 150)), fixture.repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.pokemon.collect { } }
        advanceTimeBy(20)
        runCurrent()
        assertFalse(viewModel.load.value.loading)
        assertNotNull(viewModel.load.value.error)
        assertTrue(fixture.recent.value.isEmpty())
        fixture.api.offline = false
        viewModel.retry()
        advanceTimeBy(20)
        runCurrent()
        assertNull(viewModel.load.value.error)
        assertEquals(150, viewModel.pokemon.value?.id)
        assertEquals(150, fixture.recent.value.single().pokemonId)
    }
}
