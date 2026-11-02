package com.example.pokeplanner.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_metadata")
data class SyncMetadata(@PrimaryKey val id: Int = 1, val completedAt: Long)
