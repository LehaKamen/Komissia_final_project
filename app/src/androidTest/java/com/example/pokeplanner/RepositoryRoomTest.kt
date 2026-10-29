package com.example.pokeplanner

import com.example.pokeplanner.data.Repository
import com.example.pokeplanner.data.local.AppDatabase
import com.example.pokeplanner.model.PokemonStatus
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.IOException
import javax.inject.Inject

@HiltAndroidTest
class RepositoryRoomTest {
    @get:Rule val hilt = HiltAndroidRule(this)

    @Inject lateinit var repository: Repository

    @Inject lateinit var database: AppDatabase

    @Inject lateinit var api: TestApi

    @Before fun setup() {
        hilt.inject()
    }

    @After fun close() {
        database.close()
    }

    @Test
    fun collectionsPersistMembershipAndCascadeOnlyTheirOwnLinks() = runBlocking {
        repository.openPokemon(25)
        repository.save(25, PokemonStatus.TRAINING, "Заметка")
        val first = repository.addCollection("Команда", "Описание")
        val second = repository.addCollection("Другая", "")
        repository.addMember(first, 25)
        repository.addMember(first, 25)
        repository.addMember(second, 25)
        assertEquals(2, repository.members.first().size)
        val collection = repository.collections.first().single { it.id == first }
        repository.renameCollection(collection, "Новое имя", "Новое описание")
        assertEquals("Новое имя", repository.collections.first().single { it.id == first }.name)
        repository.deleteCollection(first)
        assertEquals(second, repository.members.first().single().collectionId)
        assertEquals("Заметка", repository.personal.first().single().note)
        assertNotNull(repository.detail(25).first())
    }

    @Test
    fun failedDetailDoesNotPolluteHistoryAndOfflineReopenUsesLocalImage() = runBlocking {
        api.offline = true
        try {
            repository.openPokemon(150)
            fail("Expected network error")
        } catch (_: IOException) { }
        assertTrue(repository.recent.first().isEmpty())
        api.offline = false
        repository.openPokemon(150)
        val calls = api.detailCalls.get()
        api.offline = true
        repository.openPokemon(150)
        assertEquals(calls, api.detailCalls.get())
        assertEquals(1, repository.recent.first().size)
        assertTrue(File(checkNotNull(repository.detail(150).first()?.localImage)).isFile)
    }

    @Test
    fun russianPersonalDataUpdatesInsteadOfDuplicating() = runBlocking {
        repository.save(25, PokemonStatus.PLANNED, "Первая")
        repository.save(25, PokemonStatus.READY, "Готово")
        assertEquals(1, repository.personal.first().size)
        assertEquals(PokemonStatus.READY, repository.personal.first().single().status)
        assertEquals("Готово", repository.personal.first().single().note)
        repository.remove(25)
        assertTrue(repository.personal.first().isEmpty())
    }
}
