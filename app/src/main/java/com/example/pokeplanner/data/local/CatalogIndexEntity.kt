package com.example.pokeplanner.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "catalog_index")
data class CatalogIndexEntity(@PrimaryKey val id: Int, val name: String)
