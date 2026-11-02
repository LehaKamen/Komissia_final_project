package com.example.pokeplanner

import androidx.lifecycle.SavedStateHandle
import com.example.pokeplanner.data.Clock
import com.example.pokeplanner.data.Repository
import com.example.pokeplanner.data.local.CollectionEntity
import com.example.pokeplanner.ui.library.LibraryVm
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryVmTest {
    @get:Rule val main = MainDispatcherRule()

    @Test
    fun duplicateSaveClickCreatesOnlyOneCollection() = runTest(main.dispatcher) {
        val fixture = RepositoryFixture()
        coEvery { fixture.dao.addCollection(any()) } coAnswers {
            delay(100)
            7L
        }
        val state = SavedStateHandle()
        val viewModel = LibraryVm(fixture.repository, state)
        viewModel.createCollection()
        viewModel.editorName("Моя команда")

        viewModel.saveEditor()
        viewModel.saveEditor()
        assertTrue(checkNotNull(viewModel.editor.value).saving)
        advanceTimeBy(101)
        runCurrent()

        coVerify(exactly = 1) { fixture.dao.addCollection(any()) }
        assertNull(viewModel.editor.value)
        assertEquals(false, state.get<Boolean>("editorOpen"))
    }

    @Test
    fun restoredEditWaitsForDatabaseAndNeverBecomesCreate() = runTest(main.dispatcher) {
        val fixture = RepositoryFixture()
        val original = CollectionEntity(7, "Команда", "Описание")
        every { fixture.dao.collections() } returns flow {
            delay(100)
            emit(listOf(original))
        }
        val repository = Repository(fixture.api, fixture.dao, fixture.images, Clock { fixture.now })
        val saved = SavedStateHandle(
            mapOf(
                "editorOpen" to true,
                "editorId" to 7L,
                "editorName" to "Черновик",
                "editorDescription" to "Несохранённое описание",
            ),
        )
        val viewModel = LibraryVm(repository, saved)

        assertEquals(7L, viewModel.editor.value?.collectionId)
        assertEquals("Черновик", viewModel.editor.value?.name)
        viewModel.saveEditor()
        runCurrent()
        coVerify(exactly = 0) { fixture.dao.updateCollection(any()) }
        advanceTimeBy(101)
        runCurrent()

        coVerify(exactly = 0) { fixture.dao.addCollection(any()) }
        coVerify(exactly = 1) {
            fixture.dao.updateCollection(match { it.id == 7L && it.name == "Черновик" })
        }
        assertNull(viewModel.editor.value)
    }

    @Test
    fun deletedCollectionCannotBeRecreatedBySavingOldEditor() = runTest(main.dispatcher) {
        val fixture = RepositoryFixture()
        val viewModel = LibraryVm(fixture.repository, SavedStateHandle())
        viewModel.editCollection(CollectionEntity(7, "Удалённая"))

        viewModel.saveEditor()
        runCurrent()

        coVerify(exactly = 0) { fixture.dao.addCollection(any()) }
        coVerify(exactly = 0) { fixture.dao.updateCollection(any()) }
        assertEquals(7L, viewModel.editor.value?.collectionId)
        assertNotNull(viewModel.editor.value?.error)
    }

    @Test
    fun validationErrorKeepsDraftAndAllowsCorrection() = runTest(main.dispatcher) {
        val fixture = RepositoryFixture()
        val viewModel = LibraryVm(fixture.repository, SavedStateHandle())
        viewModel.createCollection()
        viewModel.editorName("   ")
        viewModel.editorDescription("Описание")

        viewModel.saveEditor()
        runCurrent()

        assertNotNull(viewModel.editor.value?.error)
        assertEquals("Описание", viewModel.editor.value?.description)
        coVerify(exactly = 0) { fixture.dao.addCollection(any()) }
        viewModel.editorName("Исправлено")
        assertNull(viewModel.editor.value?.error)
    }
}
