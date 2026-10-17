package com.example.pokeplanner.data

object CollectionRules {
 fun validName(name:String)=name.trim().isNotEmpty()
 fun isMember(collectionId:Long,pokemonId:Int,members:List<Pair<Long,Int>>)=
  members.any{it.first==collectionId&&it.second==pokemonId}
 fun visible(name:String,query:String,status:String?,onlyPlanned:Boolean)=
  name.contains(query,ignoreCase=true)&&(!onlyPlanned||status=="PLANNED")
 fun stale(cachedAt:Long,now:Long,ttlHours:Int)=cachedAt<now-ttlHours*3600000L
}
