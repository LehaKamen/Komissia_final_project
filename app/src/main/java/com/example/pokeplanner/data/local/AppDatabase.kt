package com.example.pokeplanner.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        PokemonEntity::class,
        PersonalEntity::class,
        RecentEntity::class,
        CollectionEntity::class,
        CollectionMemberEntity::class,
        CatalogIndexEntity::class,
        SyncMetadata::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(StatusConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): PokeDao
}
