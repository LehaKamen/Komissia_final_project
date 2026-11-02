package com.example.pokeplanner.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_sync")
data class PendingSyncEntity(@PrimaryKey val pokemonId: Int, val completed: Boolean = false)
