package com.example.pokeplanner.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PokeDao {
    @Query("SELECT * FROM pokemon ORDER BY id")
    fun pokemon(): Flow<List<PokemonEntity>>

    @Query("SELECT * FROM pokemon WHERE id = :id")
    fun pokemon(id: Int): Flow<PokemonEntity?>

    @Query("SELECT * FROM pokemon WHERE id = :id")
    suspend fun cachedPokemon(id: Int): PokemonEntity?

    @Upsert
    suspend fun putPokemon(items: List<PokemonEntity>)

    @Upsert
    suspend fun putPokemon(item: PokemonEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPokemonIfAbsent(item: PokemonEntity)

    @Query("UPDATE pokemon SET localImage = :path WHERE id = :id")
    suspend fun updateLocalImage(id: Int, path: String)

    @Query("SELECT * FROM catalog_index ORDER BY id")
    fun catalogIndex(): Flow<List<CatalogIndexEntity>>

    @Upsert
    suspend fun putIndex(items: List<CatalogIndexEntity>)

    @Query("SELECT completedAt FROM sync_metadata WHERE id = 1")
    suspend fun lastCompleteSync(): Long?

    @Upsert
    suspend fun putSyncMetadata(metadata: SyncMetadata)

    @Query("DELETE FROM sync_metadata")
    suspend fun invalidateSync()

    @Transaction
    suspend fun completeSync(items: List<PokemonEntity>, completedAt: Long) {
        putPokemon(items)
        putSyncMetadata(SyncMetadata(completedAt = completedAt))
    }

    @Query("SELECT * FROM personal")
    fun personal(): Flow<List<PersonalEntity>>

    @Query("SELECT * FROM personal WHERE pokemonId = :id")
    fun personal(id: Int): Flow<PersonalEntity?>

    @Upsert
    suspend fun putPersonal(item: PersonalEntity)

    @Query("DELETE FROM personal WHERE pokemonId = :id")
    suspend fun deletePersonal(id: Int)

    @Query("SELECT * FROM recent ORDER BY viewedAt DESC, pokemonId DESC")
    fun recent(): Flow<List<RecentEntity>>

    @Upsert
    suspend fun putRecent(item: RecentEntity)

    @Query("DELETE FROM recent")
    suspend fun clearRecent()

    @Query("SELECT * FROM collections ORDER BY name")
    fun collections(): Flow<List<CollectionEntity>>

    @Insert
    suspend fun addCollection(item: CollectionEntity): Long

    @Update
    suspend fun updateCollection(item: CollectionEntity)

    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun deleteCollection(id: Long)

    @Query("SELECT * FROM collection_members")
    fun members(): Flow<List<CollectionMemberEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addMember(item: CollectionMemberEntity)

    @Query("DELETE FROM collection_members WHERE collectionId = :collectionId AND pokemonId = :pokemonId")
    suspend fun removeMember(collectionId: Long, pokemonId: Int)
}
