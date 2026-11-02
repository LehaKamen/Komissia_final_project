package com.example.pokeplanner

import com.example.pokeplanner.data.Clock
import com.example.pokeplanner.data.ImageStore
import com.example.pokeplanner.data.Repository
import com.example.pokeplanner.data.Settings
import com.example.pokeplanner.data.SettingsSource
import com.example.pokeplanner.data.local.CatalogIndexEntity
import com.example.pokeplanner.data.local.CollectionEntity
import com.example.pokeplanner.data.local.CollectionMemberEntity
import com.example.pokeplanner.data.local.PersonalEntity
import com.example.pokeplanner.data.local.PokeDao
import com.example.pokeplanner.data.local.PokemonEntity
import com.example.pokeplanner.data.local.RecentEntity
import com.example.pokeplanner.data.remote.DetailDto
import com.example.pokeplanner.data.remote.ListDto
import com.example.pokeplanner.data.remote.Named
import com.example.pokeplanner.data.remote.PokeApi
import com.example.pokeplanner.data.remote.Sprites
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.io.IOException

class FakeSettings : SettingsSource {
    override val flow = MutableStateFlow(Settings())
    override suspend fun dark(value: Boolean) {
        flow.value = flow.value.copy(dark = value)
    }
    override suspend fun planned(value: Boolean) {
        flow.value = flow.value.copy(onlyPlanned = value)
    }
    override suspend fun hours(value: Int) {
        flow.value = flow.value.copy(refreshHours = value)
    }
}

class FakeApi : PokeApi {
    var offline = false
    var listCalls = 0
    var activeCalls = 0
    var maximumConcurrent = 0
    val failedIds = mutableSetOf<Int>()
    val requestedIds = mutableListOf<Int>()
    var total = 151

    override suspend fun list(limit: Int): ListDto {
        listCalls++
        if (offline) throw IOException("offline")
        return ListDto((1..total).map { id -> Named(name(id), "https://pokeapi.co/api/v2/pokemon/$id/") })
    }

    override suspend fun detail(id: Int): DetailDto {
        requestedIds += id
        activeCalls++
        maximumConcurrent = maxOf(maximumConcurrent, activeCalls)
        try {
            delay(10)
            if (offline || id in failedIds) throw IOException("offline")
            return DetailDto(id, name(id), Sprites("https://images/$id.png"), emptyList())
        } finally {
            activeCalls--
        }
    }

    private fun name(id: Int) = when (id) {
        25 -> "pikachu"
        150 -> "mewtwo"
        else -> "pokemon$id"
    }
}

class FakeImages : ImageStore {
    val saved = mutableSetOf<String>()
    val failedIds = mutableSetOf<Int>()
    var calls = 0
    var onSave: suspend (Int) -> Unit = { }
    override suspend fun save(id: Int, url: String?): String {
        calls++
        if (id in failedIds) throw IOException("image failed")
        onSave(id)
        return "/images/$id.png".also { saved += it }
    }
    override fun exists(path: String?) = path in saved
}

class RepositoryFixture {
    val api = FakeApi()
    val images = FakeImages()
    val cards = MutableStateFlow<List<PokemonEntity>>(emptyList())
    val index = MutableStateFlow<List<CatalogIndexEntity>>(emptyList())
    val personal = MutableStateFlow<List<PersonalEntity>>(emptyList())
    val recent = MutableStateFlow<List<RecentEntity>>(emptyList())
    val collections = MutableStateFlow<List<CollectionEntity>>(emptyList())
    val members = MutableStateFlow<List<CollectionMemberEntity>>(emptyList())
    var stamp: Long? = null
    var now = 10_000L
    val dao = mockk<PokeDao>(relaxed = true)

    init {
        every { dao.pokemon() } returns cards
        every { dao.catalogIndex() } returns index
        every { dao.personal() } returns personal
        every { dao.recent() } returns recent
        every { dao.collections() } returns collections
        every { dao.members() } returns members
        every { dao.pokemon(any()) } answers {
            val id = firstArg<Int>()
            cards.map { rows -> rows.find { it.id == id } }
        }
        every { dao.personal(any()) } answers {
            val id = firstArg<Int>()
            personal.map { rows -> rows.find { it.pokemonId == id } }
        }
        coEvery { dao.cachedPokemon(any()) } answers { cards.value.find { it.id == firstArg<Int>() } }
        coEvery { dao.lastCompleteSync() } answers { stamp }
        coEvery { dao.putIndex(any()) } answers { index.value = firstArg() }
        coEvery { dao.putPokemon(any<List<PokemonEntity>>()) } answers { putCards(firstArg()) }
        coEvery { dao.putPokemon(any<PokemonEntity>()) } answers { putCards(listOf(firstArg())) }
        coEvery { dao.insertPokemonIfAbsent(any()) } answers {
            val item = firstArg<PokemonEntity>()
            if (cards.value.none { it.id == item.id }) putCards(listOf(item))
        }
        coEvery { dao.updateLocalImage(any(), any()) } answers {
            val id = firstArg<Int>()
            val path = secondArg<String>()
            cards.value = cards.value.map { if (it.id == id) it.copy(localImage = path) else it }
        }
        coEvery { dao.completeSync(any(), any()) } answers {
            putCards(firstArg())
            stamp = secondArg()
        }
        coEvery { dao.invalidateSync() } answers { stamp = null }
        coEvery { dao.putRecent(any()) } answers {
            val item = firstArg<RecentEntity>()
            recent.value =
                (recent.value.filterNot { it.pokemonId == item.pokemonId } + item).sortedByDescending { it.viewedAt }
        }
        coEvery { dao.putPersonal(any()) } answers {
            val item = firstArg<PersonalEntity>()
            personal.value = personal.value.filterNot { it.pokemonId == item.pokemonId } + item
        }
        coEvery { dao.deletePersonal(any()) } answers {
            personal.value = personal.value.filterNot { it.pokemonId == firstArg<Int>() }
        }
    }

    val repository = Repository(api, dao, images, Clock { now })

    private fun putCards(items: List<PokemonEntity>) {
        val ids = items.map { it.id }.toSet()
        cards.value = (cards.value.filterNot { it.id in ids } + items).sortedBy { it.id }
    }
}
