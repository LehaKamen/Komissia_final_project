package com.example.pokeplanner.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent")
data class RecentEntity(@PrimaryKey val pokemonId: Int, val viewedAt: Long)
