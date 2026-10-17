package com.example.pokeplanner.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName="pokemon")
data class PokemonEntity(@PrimaryKey val id:Int,val name:String,val image:String,val types:String,val cachedAt:Long)
@Entity(tableName="personal")
data class PersonalEntity(@PrimaryKey val pokemonId:Int,val status:String,val note:String,val collection:String)
@Entity(tableName="recent")
data class RecentEntity(@PrimaryKey val pokemonId:Int,val viewedAt:Long)
@Entity(tableName="collections")
data class CollectionEntity(@PrimaryKey(autoGenerate=true) val id:Long=0,val name:String,val description:String="")
@Entity(tableName="collection_members",primaryKeys=["collectionId","pokemonId"],indices=[Index("pokemonId")],foreignKeys=[ForeignKey(entity=CollectionEntity::class,parentColumns=["id"],childColumns=["collectionId"],onDelete=ForeignKey.CASCADE)])
data class CollectionMemberEntity(val collectionId:Long,val pokemonId:Int)

@Dao interface PokeDao {
 @Query("SELECT * FROM pokemon ORDER BY id") fun pokemon():Flow<List<PokemonEntity>>
 @Query("SELECT * FROM pokemon WHERE id=:id") fun pokemon(id:Int):Flow<PokemonEntity?>
 @Query("SELECT * FROM pokemon") suspend fun cachedPokemon():List<PokemonEntity>
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun putPokemon(v:List<PokemonEntity>)
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun putPokemon(v:PokemonEntity)
 @Query("SELECT * FROM personal") fun personal():Flow<List<PersonalEntity>>
 @Query("SELECT * FROM personal WHERE pokemonId=:id") fun personal(id:Int):Flow<PersonalEntity?>
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun putPersonal(v:PersonalEntity)
 @Query("DELETE FROM personal WHERE pokemonId=:id") suspend fun deletePersonal(id:Int)
 @Query("SELECT * FROM recent ORDER BY viewedAt DESC") fun recent():Flow<List<RecentEntity>>
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun putRecent(v:RecentEntity)
 @Query("DELETE FROM recent") suspend fun clearRecent()
 @Query("SELECT * FROM collections ORDER BY name") fun collections():Flow<List<CollectionEntity>>
 @Insert suspend fun addCollection(v:CollectionEntity):Long
 @Update suspend fun updateCollection(v:CollectionEntity)
 @Query("DELETE FROM collections WHERE id=:id") suspend fun deleteCollection(id:Long)
 @Query("SELECT * FROM collection_members") fun members():Flow<List<CollectionMemberEntity>>
 @Insert(onConflict=OnConflictStrategy.IGNORE) suspend fun addMember(v:CollectionMemberEntity)
 @Query("DELETE FROM collection_members WHERE collectionId=:collectionId AND pokemonId=:pokemonId") suspend fun removeMember(collectionId:Long,pokemonId:Int)
}

@Database(entities=[PokemonEntity::class,PersonalEntity::class,RecentEntity::class,CollectionEntity::class,CollectionMemberEntity::class],version=2,exportSchema=false)
abstract class AppDatabase:RoomDatabase(){abstract fun dao():PokeDao}
