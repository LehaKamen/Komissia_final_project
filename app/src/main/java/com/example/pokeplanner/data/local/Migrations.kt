package com.example.pokeplanner.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS collections (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                description TEXT NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS collection_members (
                collectionId INTEGER NOT NULL,
                pokemonId INTEGER NOT NULL,
                PRIMARY KEY(collectionId, pokemonId),
                FOREIGN KEY(collectionId) REFERENCES collections(id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_collection_members_pokemonId ON collection_members(pokemonId)")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Convert the former free-text team into real collections before removing it.
        db.execSQL(
            """
            INSERT INTO collections(name, description)
            SELECT DISTINCT TRIM(p.collection), '' FROM personal p
            WHERE TRIM(p.collection) <> ''
              AND NOT EXISTS (SELECT 1 FROM collections c WHERE c.name = TRIM(p.collection))
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT OR IGNORE INTO collection_members(collectionId, pokemonId)
            SELECT (SELECT MIN(c.id) FROM collections c WHERE c.name = TRIM(p.collection)), p.pokemonId
            FROM personal p WHERE TRIM(p.collection) <> ''
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE personal_new (
                pokemonId INTEGER NOT NULL PRIMARY KEY,
                status TEXT NOT NULL,
                note TEXT NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL("INSERT INTO personal_new SELECT pokemonId, status, note FROM personal")
        db.execSQL("DROP TABLE personal")
        db.execSQL("ALTER TABLE personal_new RENAME TO personal")
        db.execSQL("ALTER TABLE pokemon ADD COLUMN localImage TEXT")
        db.execSQL("CREATE TABLE catalog_index (id INTEGER NOT NULL PRIMARY KEY, name TEXT NOT NULL)")
        db.execSQL("INSERT INTO catalog_index SELECT id, name FROM pokemon")
        // An old or partial cache must not be marked as a complete successful sync.
        db.execSQL("CREATE TABLE sync_metadata (id INTEGER NOT NULL PRIMARY KEY, completedAt INTEGER NOT NULL)")
    }
}
