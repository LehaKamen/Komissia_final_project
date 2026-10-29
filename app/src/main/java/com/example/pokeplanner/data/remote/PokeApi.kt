package com.example.pokeplanner.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

data class Named(val name: String, val url: String)
data class ListDto(val results: List<Named>)
data class Sprites(val front_default: String?)
data class TypeRef(val type: Named)
data class DetailDto(val id: Int, val name: String, val sprites: Sprites, val types: List<TypeRef>)

interface PokeApi {
    @GET("pokemon")
    suspend fun list(@Query("limit") limit: Int = 100_000): ListDto

    @GET("pokemon/{id}")
    suspend fun detail(@Path("id") id: Int): DetailDto
}
