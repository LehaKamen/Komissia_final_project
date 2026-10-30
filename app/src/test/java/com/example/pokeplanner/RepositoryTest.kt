package com.example.pokeplanner

import com.example.pokeplanner.data.IncompleteSyncException
import com.example.pokeplanner.data.local.PokemonEntity
import com.example.pokeplanner.model.PokemonStatus
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class RepositoryTest {
    @Test
    fun openingCachedCardDoesNotOverwriteConcurrentRefresh() = runTest {
        val fixture = RepositoryFixture()
        val old = PokemonEntity(25, "Pikachu", "https://image", "old", 1, "/images/25.png")
        val updated = old.copy(types = "updated", cachedAt = 100)
        fixture.cards.value = listOf(updated)
        fixture.images.saved += "/images/25.png"
        coEvery { fixture.dao.cachedPokemon(25) } returns old

        fixture.repository.openPokemon(25)

        assertEquals(updated, fixture.cards.value.single())
        assertEquals(25, fixture.recent.value.single().pokemonId)
        coVerify(exactly = 0) { fixture.dao.putPokemon(any<PokemonEntity>()) }
    }

    @Test
    fun restoringImageDoesNotOverwriteMetadataUpdatedDuringDownload() = runTest {
        val fixture = RepositoryFixture()
        val old = PokemonEntity(25, "Pikachu", "https://image", "old", 1)
        fixture.cards.value = listOf(old)
        fixture.images.onSave = {
            fixture.cards.value = listOf(old.copy(types = "updated", cachedAt = 100))
        }

        fixture.repository.openPokemon(25)

        val result = fixture.cards.value.single()
        assertEquals("updated", result.types)
        assertEquals(100L, result.cachedAt)
        assertTrue(fixture.images.exists(result.localImage))
    }

    @Test
    fun forcedPartialRefreshInvalidatesPreviouslyFreshBatch() = runTest {
        val fixture = RepositoryFixture()
        fixture.repository.refreshIfNeeded(12)
        assertFalse(fixture.repository.shouldRefresh(12))
        fixture.api.failedIds += 25

        try {
            fixture.repository.refreshIfNeeded(12, force = true)
            fail("Partial refresh must not succeed")
        } catch (_: IncompleteSyncException) { }

        assertNull(fixture.stamp)
        assertTrue(fixture.repository.shouldRefresh(12))
        assertEquals(60, fixture.cards.value.size)
    }

    @Test
    fun completeRefreshSavesFullSearchIndexAndOnlyPreloadsFirstPage() = runTest {
        val fixture = RepositoryFixture()
        fixture.repository.refreshIfNeeded(12)
        assertEquals(151, fixture.repository.catalog.first().size)
        assertEquals(60, fixture.cards.value.size)
        assertEquals(fixture.now, fixture.stamp)
        assertEquals("Mewtwo", fixture.repository.catalog.first().single { it.id == 150 }.name)
        assertFalse(fixture.repository.catalog.first().single { it.id == 150 }.detailsSaved)
    }

    @Test
    fun detailRequestsNeverExceedConcurrencyLimit() = runTest {
        val fixture = RepositoryFixture()
        fixture.repository.refreshIfNeeded(12)
        assertEquals(4, fixture.api.maximumConcurrent)
    }

    @Test
    fun partialRefreshIsNotFreshAndMissingCardsAreRetriedImmediately() = runTest {
        val fixture = RepositoryFixture()
        fixture.api.failedIds += (11..60)
        try {
            fixture.repository.refreshIfNeeded(12)
            fail("A partial sync must fail")
        } catch (error: IncompleteSyncException) {
            assertEquals(50, error.failedCount)
        }
        assertEquals(10, fixture.cards.value.size)
        assertTrue(fixture.repository.shouldRefresh(12))
        fixture.api.failedIds.clear()
        fixture.repository.refreshIfNeeded(12)
        assertEquals(60, fixture.cards.value.size)
        assertFalse(fixture.repository.shouldRefresh(12))
    }

    @Test
    fun offlineRefreshPreservesExistingCacheAndNotes() = runTest {
        val fixture = RepositoryFixture()
        fixture.repository.refreshIfNeeded(12)
        fixture.repository.save(25, PokemonStatus.TRAINING, "Моя заметка")
        val before = fixture.cards.value
        fixture.api.offline = true
        try {
            fixture.repository.refreshIfNeeded(12, force = true)
            fail("Expected network error")
        } catch (_: IOException) { }
        assertEquals(before, fixture.cards.value)
        assertEquals("Моя заметка", fixture.personal.value.single().note)
    }

    @Test
    fun ttlBoundaryUsesLastCompleteBatchInsteadOfIndividualRows() = runTest {
        val fixture = RepositoryFixture()
        fixture.repository.refreshIfNeeded(6)
        fixture.now += 6 * 3_600_000L - 1
        assertFalse(fixture.repository.shouldRefresh(6))
        fixture.now++
        assertTrue(fixture.repository.shouldRefresh(6))
    }

    @Test
    fun freshCacheSkipsApiUnlessUserForcesRefresh() = runTest {
        val fixture = RepositoryFixture()
        fixture.repository.refreshIfNeeded(12)
        fixture.repository.refreshIfNeeded(12)
        assertEquals(1, fixture.api.listCalls)
        fixture.repository.refreshIfNeeded(12, force = true)
        assertEquals(2, fixture.api.listCalls)
    }

    @Test
    fun cardOutsideFirstPageLoadsByIdAndIsIncludedInNextSync() = runTest {
        val fixture = RepositoryFixture()
        fixture.repository.openPokemon(150)
        assertEquals(150, fixture.cards.value.single().id)
        assertEquals(150, fixture.recent.value.single().pokemonId)
        fixture.api.requestedIds.clear()
        fixture.repository.refreshIfNeeded(12)
        assertTrue(150 in fixture.api.requestedIds)
    }

    @Test
    fun failedCardDoesNotCreateHistoryEntry() = runTest {
        val fixture = RepositoryFixture()
        fixture.api.failedIds += 150
        try {
            fixture.repository.openPokemon(150)
            fail("Expected error")
        } catch (_: IOException) { }
        assertTrue(fixture.cards.value.isEmpty())
        assertTrue(fixture.recent.value.isEmpty())
        fixture.api.failedIds.clear()
        fixture.repository.openPokemon(150)
        assertEquals(150, fixture.recent.value.single().pokemonId)
    }

    @Test
    fun imageFailureCannotMarkCardOrBatchAsAvailableOffline() = runTest {
        val fixture = RepositoryFixture()
        fixture.images.failedIds += 25
        try {
            fixture.repository.refreshIfNeeded(12)
            fail("Image failure must fail the batch")
        } catch (_: IncompleteSyncException) { }
        assertNull(fixture.cards.value.find { it.id == 25 })
        assertNull(fixture.stamp)
    }

    @Test
    fun savedCardAndImageReopenOfflineWithoutNetwork() = runTest {
        val fixture = RepositoryFixture()
        fixture.repository.openPokemon(25)
        fixture.api.offline = true
        fixture.api.requestedIds.clear()
        val imageCalls = fixture.images.calls
        fixture.now++
        fixture.repository.openPokemon(25)
        assertTrue(fixture.api.requestedIds.isEmpty())
        assertEquals(imageCalls, fixture.images.calls)
        assertTrue(fixture.images.exists(fixture.cards.value.single().localImage))
        assertEquals(1, fixture.recent.value.size)
        assertEquals(fixture.now, fixture.recent.value.single().viewedAt)
    }

    @Test
    fun collectionValidationAndNormalizationAreUsedByRepository() = runTest {
        val fixture = RepositoryFixture()
        try {
            fixture.repository.addCollection("  ", "")
            fail("Empty name must fail")
        } catch (_: IllegalArgumentException) { }
        coVerify(exactly = 0) { fixture.dao.addCollection(any()) }
        fixture.repository.addCollection("  Команда  ", "  Описание  ")
        coVerify { fixture.dao.addCollection(match { it.name == "Команда" && it.description == "Описание" }) }
    }
}
