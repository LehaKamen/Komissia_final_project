package com.example.pokeplanner.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pokemon")
data class PokemonEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val image: String,
    val types: String,
    val cachedAt: Long,
    val localImage: String? = null,
)
