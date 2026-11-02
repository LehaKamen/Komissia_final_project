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
    fun completedBatchIsFinalizedAfterReopenWithoutAnyNetworkRequests() = runBlocking {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val name = "finished-sync-test.db"
        context.deleteDatabase(name)
        var db = androidx.room.Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        try {
            db.dao().beginSync(listOf(25))
            db.dao().saveSyncedPokemon(
                com.example.pokeplanner.data.local.PokemonEntity(25, "Pikachu", "", "electric", 1),
            )
            assertTrue(db.dao().hasPendingSync())
            assertTrue(db.dao().pendingSyncIds().isEmpty())
            db.close()
            db = androidx.room.Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            api.offline = true
            val images = TestImages(File(context.cacheDir, "finished-sync-images"))
            val clock = com.example.pokeplanner.data.Clock { 2000L }
            Repository(api, db.dao(), images, clock).refreshIfNeeded(12)
            assertEquals(0, api.listCalls.get())
            assertEquals(0, api.detailCalls.get())
            assertEquals(2000L, db.dao().lastCompleteSync())
            assertTrue(!db.dao().hasPendingSync())
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun pendingBatchSurvivesDatabaseCloseAndRetriesOnlyMissingCard() = runBlocking {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val name = "pending-sync-test.db"
        context.deleteDatabase(name)
        val images = TestImages(File(context.cacheDir, "pending-sync-images"))
        val clock = com.example.pokeplanner.data.Clock { 1000L }
        var db = androidx.room.Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        try {
            api.failedId = 25
            try {
                Repository(api, db.dao(), images, clock).refreshIfNeeded(12)
                fail("Expected partial failure")
            } catch (_: com.example.pokeplanner.data.IncompleteSyncException) { }
            assertEquals(listOf(25), db.dao().pendingSyncIds())
            assertEquals(59, db.dao().pokemon().first().size)
            db.close()
            db = androidx.room.Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            api.failedId = null
            val calls = api.detailCalls.get()
            Repository(api, db.dao(), images, clock).refreshIfNeeded(12)
            assertEquals(1, api.detailCalls.get() - calls)
            assertEquals(1, api.listCalls.get())
            assertEquals(60, db.dao().pokemon().first().size)
            assertTrue(db.dao().pendingSyncIds().isEmpty())
            assertNotNull(db.dao().lastCompleteSync())
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
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
