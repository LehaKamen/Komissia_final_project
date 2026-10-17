package com.example.pokeplanner

import com.example.pokeplanner.data.Repository
import com.example.pokeplanner.data.local.*
import com.example.pokeplanner.data.remote.*
import io.mockk.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class RepositoryBehaviorTest {
    private val api = mockk<PokeApi>()
    private val dao = mockk<PokeDao> {
        every { pokemon() } returns MutableStateFlow(emptyList())
        every { personal() } returns MutableStateFlow(emptyList())
        every { recent() } returns MutableStateFlow(emptyList())
        every { collections() } returns MutableStateFlow(emptyList())
        every { members() } returns MutableStateFlow(emptyList())
    }
    private val repository = Repository(api, dao)

    @Test fun emptyCacheRequiresRefresh() = runTest {
        coEvery { dao.cachedPokemon() } returns emptyList()
        assertTrue(repository.shouldRefresh(12))
    }

    @Test fun recentCacheDoesNotRequireRefresh() = runTest {
        coEvery { dao.cachedPokemon() } returns listOf(
            PokemonEntity(25, "Pikachu", "", "electric", System.currentTimeMillis())
        )
        assertFalse(repository.shouldRefresh(12))
    }

    @Test fun staleCacheRequiresRefresh() = runTest {
        coEvery { dao.cachedPokemon() } returns listOf(
            PokemonEntity(25, "Pikachu", "", "electric", System.currentTimeMillis() - 48 * 3600000L)
        )
        assertTrue(repository.shouldRefresh(12))
    }

    @Test fun savingRussianNoteWritesToRoom() = runTest {
        val saved = slot<PersonalEntity>()
        coEvery { dao.putPersonal(capture(saved)) } just Runs
        repository.save(25, "PLANNED", "Мой любимый покемон", "Команда")
        assertEquals("Мой любимый покемон", saved.captured.note)
        assertEquals("Команда", saved.captured.collection)
        coVerify(exactly = 1) { dao.putPersonal(any()) }
    }

    @Test fun clearingHistoryDelegatesToDatabase() = runTest {
        coEvery { dao.clearRecent() } just Runs
        repository.clearRecent()
        coVerify(exactly = 1) { dao.clearRecent() }
    }

    @Test fun failedApiRequestDoesNotOverwriteOfflineCache() = runTest {
        coEvery { api.list() } throws java.io.IOException("offline")
        try {
            repository.refresh()
            fail("Expected an IOException")
        } catch (_: java.io.IOException) { }
        coVerify(exactly = 0) { dao.putPokemon(any<List<PokemonEntity>>()) }
    }

    @Test fun roomChangesAreDeliveredReactively() = runTest {
        val cached = MutableStateFlow<List<PokemonEntity>>(emptyList())
        every { dao.pokemon() } returns cached
        val observed = Repository(api, dao).pokemon
        assertTrue(observed.first().isEmpty())
        cached.value = listOf(PokemonEntity(1, "Bulbasaur", "", "grass", 1L))
        assertEquals("Bulbasaur", observed.first().single().name)
    }

    @Test fun deletingCollectionCallsDao() = runTest {
        coEvery { dao.deleteCollection(7L) } just Runs
        repository.deleteCollection(7L)
        coVerify(exactly = 1) { dao.deleteCollection(7L) }
    }
}
