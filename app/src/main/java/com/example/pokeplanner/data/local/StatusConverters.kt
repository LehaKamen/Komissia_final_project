package com.example.pokeplanner.data.local

import androidx.room.TypeConverter
import com.example.pokeplanner.model.PokemonStatus

class StatusConverters {
    @TypeConverter
    fun fromStatus(value: PokemonStatus): String = value.name

    @TypeConverter
    fun toStatus(value: String): PokemonStatus = PokemonStatus.fromStored(value)
}
