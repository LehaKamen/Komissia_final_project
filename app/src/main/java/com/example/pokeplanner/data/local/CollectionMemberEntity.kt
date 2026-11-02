package com.example.pokeplanner.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "collection_members",
    primaryKeys = ["collectionId", "pokemonId"],
    indices = [Index("pokemonId")],
    foreignKeys = [
        ForeignKey(
            entity = CollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class CollectionMemberEntity(val collectionId: Long, val pokemonId: Int)
