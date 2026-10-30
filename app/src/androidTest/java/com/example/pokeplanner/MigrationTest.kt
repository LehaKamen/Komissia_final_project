package com.example.pokeplanner

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.pokeplanner.data.local.AppDatabase
import com.example.pokeplanner.data.local.MIGRATION_1_2
import com.example.pokeplanner.data.local.MIGRATION_2_3
import com.example.pokeplanner.model.PokemonStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MigrationTest {
    @Test fun versionOnePreservesPersonalDataAndCreatesCollectionLinks() = verifyMigration(1)

    @Test fun versionTwoPreservesExistingCollectionsAndConvertsOldTeam() = verifyMigration(2)

    private fun verifyMigration(version: Int) = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-test-$version.db"
        context.deleteDatabase(name)
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(version) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            "CREATE TABLE pokemon (id INTEGER NOT NULL PRIMARY KEY, name TEXT NOT NULL, image TEXT NOT NULL, types TEXT NOT NULL, cachedAt INTEGER NOT NULL)",
                        )
                        db.execSQL(
                            "CREATE TABLE personal (pokemonId INTEGER NOT NULL PRIMARY KEY, status TEXT NOT NULL, note TEXT NOT NULL, collection TEXT NOT NULL)",
                        )
                        db.execSQL(
                            "CREATE TABLE recent (pokemonId INTEGER NOT NULL PRIMARY KEY, viewedAt INTEGER NOT NULL)",
                        )
                        if (version == 2) MIGRATION_1_2.migrate(db)
                        db.execSQL("INSERT INTO pokemon VALUES (25, 'Pikachu', 'https://image', 'electric', 123)")
                        db.execSQL("INSERT INTO personal VALUES (25, 'TRAINING', 'Старая заметка', 'Моя команда')")
                        db.execSQL("INSERT INTO recent VALUES (25, 456)")
                        if (version == 2) {
                            db.execSQL("INSERT INTO collections VALUES (7, 'Существующая', 'Описание')")
                            db.execSQL("INSERT INTO collection_members VALUES (7, 25)")
                        }
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build(),
        )
        helper.writableDatabase
        helper.close()
        val database = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
        try {
            val dao = database.dao()
            val personal = dao.personal().first().single()
            assertEquals("Старая заметка", personal.note)
            assertEquals(PokemonStatus.TRAINING, personal.status)
            assertEquals(25, dao.recent().first().single().pokemonId)
            assertEquals("Pikachu", dao.catalogIndex().first().single().name)
            val team = dao.collections().first().single { it.name == "Моя команда" }
            assertTrue(dao.members().first().any { it.collectionId == team.id && it.pokemonId == 25 })
            if (version == 2) assertTrue(dao.members().first().any { it.collectionId == 7L })
            assertNull(dao.lastCompleteSync())
            assertNull(dao.cachedPokemon(25)?.localImage)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }
}
