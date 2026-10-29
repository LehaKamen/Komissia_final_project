package com.example.pokeplanner.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.pokeplanner.model.PokemonStatus

@Entity(tableName = "personal")
data class PersonalEntity(@PrimaryKey val pokemonId: Int, val status: PokemonStatus, val note: String)
