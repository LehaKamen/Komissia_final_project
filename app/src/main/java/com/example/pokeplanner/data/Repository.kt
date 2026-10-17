package com.example.pokeplanner.data

import com.example.pokeplanner.data.local.*
import com.example.pokeplanner.data.remote.PokeApi
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton class Repository @Inject constructor(private val api:PokeApi,private val dao:PokeDao){
 val pokemon=dao.pokemon()
 val personal=dao.personal()
 val recent=dao.recent()
 val collections=dao.collections()
 val members=dao.members()
 suspend fun refresh()=coroutineScope {
  val remote=api.list().results
  val details=remote.mapNotNull { item ->
   item.url.trimEnd('/').substringAfterLast('/').toIntOrNull()?.let { id ->
    async { runCatching { api.detail(id) }.getOrNull() }
   }
  }.awaitAll().filterNotNull()
  if(details.isEmpty()) error("API returned no Pokémon details")
  dao.putPokemon(details.map { d ->
   PokemonEntity(d.id,d.name.replaceFirstChar(Char::uppercase),
    d.sprites.front_default ?: "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/${d.id}.png",
    d.types.joinToString(", "){it.type.name},System.currentTimeMillis())
  })
 }
 fun detail(id:Int)=dao.pokemon(id)
 fun personal(id:Int)=dao.personal(id)
 suspend fun ensure(id:Int){
  val d=api.detail(id)
  dao.putPokemon(PokemonEntity(d.id,d.name.replaceFirstChar(Char::uppercase),
   d.sprites.front_default ?: "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/${d.id}.png",
   d.types.joinToString(", "){it.type.name},System.currentTimeMillis()))
 }
 suspend fun save(id:Int,status:String,note:String,collection:String)=dao.putPersonal(PersonalEntity(id,status,note,collection))
 suspend fun remove(id:Int)=dao.deletePersonal(id)
 suspend fun recent(id:Int)=dao.putRecent(RecentEntity(id,System.currentTimeMillis()))
 suspend fun clearRecent()=dao.clearRecent()
 suspend fun addCollection(name:String,description:String)=dao.addCollection(CollectionEntity(name=name.trim(),description=description.trim()))
 suspend fun renameCollection(item:CollectionEntity)=dao.updateCollection(item)
 suspend fun deleteCollection(id:Long)=dao.deleteCollection(id)
 suspend fun addMember(collectionId:Long,pokemonId:Int)=dao.addMember(CollectionMemberEntity(collectionId,pokemonId))
 suspend fun removeMember(collectionId:Long,pokemonId:Int)=dao.removeMember(collectionId,pokemonId)
 suspend fun shouldRefresh(ttlHours:Int):Boolean {
  val cache=dao.cachedPokemon()
  return cache.isEmpty() || cache.minOf { it.cachedAt } < System.currentTimeMillis()-ttlHours*3600000L
 }
}
