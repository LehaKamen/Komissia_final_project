package com.example.pokeplanner.data

import com.example.pokeplanner.data.local.CatalogIndexEntity
import com.example.pokeplanner.data.local.CollectionEntity
import com.example.pokeplanner.data.local.CollectionMemberEntity
import com.example.pokeplanner.data.local.PersonalEntity
import com.example.pokeplanner.data.local.PokeDao
import com.example.pokeplanner.data.local.PokemonEntity
import com.example.pokeplanner.data.local.RecentEntity
import com.example.pokeplanner.data.remote.PokeApi
import com.example.pokeplanner.model.CatalogItem
import com.example.pokeplanner.model.PokemonStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

class IncompleteSyncException(val failedCount: Int, val totalCount: Int, val retryable: Boolean = true) :
    IOException(
        "Не удалось загрузить $failedCount из $totalCount карточек. Обновление не завершено; повторите попытку.",
    )

@Singleton
class Repository @Inject constructor(
    private val api: PokeApi,
    private val dao: PokeDao,
    private val images: ImageStore,
    private val clock: Clock,
) {
    private val syncMutex = Mutex()
    private val requests = Semaphore(4)

    val pokemon = dao.pokemon()
    val personal = dao.personal()
    val recent = dao.recent()
    val collections = dao.collections()
    val members = dao.members()
    val catalog = combine(dao.catalogIndex(), pokemon) { index, saved ->
        val byId = saved.associateBy { it.id }
        val entries = (index + saved.map { CatalogIndexEntity(it.id, it.name) })
            .distinctBy { it.id }.sortedBy { it.id }
        entries.map { item ->
            val details = byId[item.id]
            CatalogItem(
                id = item.id,
                name = item.name,
                types = details?.types.orEmpty(),
                localImage = details?.localImage?.takeIf(images::exists),
                detailsSaved = details != null,
            )
        }
    }

    suspend fun refreshIfNeeded(ttlHours: Int, force: Boolean = false) = syncMutex.withLock {
        if (force || shouldRefresh(ttlHours)) refreshLocked()
    }

    suspend fun shouldRefresh(ttlHours: Int): Boolean {
        CachePolicy.validateHours(ttlHours)
        return CachePolicy.isStale(dao.lastCompleteSync(), clock.now(), ttlHours)
    }

    private suspend fun refreshLocked() = coroutineScope {
        val ids = if (dao.hasPendingSync()) {
            dao.pendingSyncIds()
        } else {
            beginRefresh()
        }
        val results = ids.map { id ->
            async {
                try {
                    dao.saveSyncedPokemon(downloadDetail(id))
                    Result.success(Unit)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    Result.failure<Unit>(error)
                }
            }
        }.awaitAll()
        val errors = results.mapNotNull { it.exceptionOrNull() }
        if (errors.isNotEmpty()) {
            throw IncompleteSyncException(errors.size, ids.size, errors.any(::isRetryableSyncError))
        }
        dao.finishSync(clock.now())
    }

    private suspend fun beginRefresh(): List<Int> {
        val index = api.list().results.map { item ->
            val id = item.url.trimEnd('/').substringAfterLast('/').toIntOrNull()
                ?: throw PermanentSyncException("Сервер вернул некорректный идентификатор покемона")
            CatalogIndexEntity(id, item.name.replaceFirstChar(Char::uppercase))
        }.distinctBy { it.id }.sortedBy { it.id }
        if (index.isEmpty()) throw PermanentSyncException("Сервер вернул пустой каталог")
        dao.putIndex(index)

        // Preload the first page; also refresh every previously saved card.
        val savedIds = pokemon.first().map { it.id }
        val ids = (index.take(60).map { it.id } + savedIds).distinct()
        dao.beginSync(ids)
        return ids
    }

    private suspend fun downloadDetail(id: Int): PokemonEntity = requests.withPermit {
        val detail = api.detail(id)
        val imageUrl = detail.sprites.front_default
        val localImage = images.save(detail.id, imageUrl)
        PokemonEntity(
            id = detail.id,
            name = detail.name.replaceFirstChar(Char::uppercase),
            image = imageUrl.orEmpty(),
            types = detail.types.joinToString(", ") { it.type.name },
            cachedAt = clock.now(),
            localImage = localImage,
        )
    }

    fun detail(id: Int) = dao.pokemon(id)
    fun personal(id: Int) = dao.personal(id)

    suspend fun openPokemon(id: Int) {
        val cached = dao.cachedPokemon(id)
        if (cached == null) {
            dao.insertPokemonIfAbsent(downloadDetail(id))
        }
        // A saved text card can be viewed even if its image cannot be restored offline.
        dao.putRecent(RecentEntity(id, clock.now()))
        if (cached != null && !images.exists(cached.localImage)) {
            val path = images.save(id, cached.image)
            dao.updateLocalImage(id, path)
        }
    }

    suspend fun save(id: Int, status: PokemonStatus, note: String) = dao.putPersonal(PersonalEntity(id, status, note))

    suspend fun remove(id: Int) = dao.deletePersonal(id)
    suspend fun clearRecent() = dao.clearRecent()

    suspend fun addCollection(name: String, description: String): Long =
        dao.addCollection(CollectionEntity(name = CollectionNames.normalize(name), description = description.trim()))

    suspend fun renameCollection(item: CollectionEntity, name: String, description: String) =
        dao.updateCollection(item.copy(name = CollectionNames.normalize(name), description = description.trim()))

    suspend fun deleteCollection(id: Long) = dao.deleteCollection(id)
    suspend fun addMember(collectionId: Long, pokemonId: Int) =
        dao.addMember(CollectionMemberEntity(collectionId, pokemonId))

    suspend fun removeMember(collectionId: Long, pokemonId: Int) = dao.removeMember(collectionId, pokemonId)
}
