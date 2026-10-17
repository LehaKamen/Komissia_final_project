package com.example.pokeplanner.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.pokeplanner.data.local.AppDatabase
import com.example.pokeplanner.data.local.PokeDao
import com.example.pokeplanner.data.remote.PokeApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

val MIGRATION_1_2=object:Migration(1,2){
 override fun migrate(db:SupportSQLiteDatabase){
  db.execSQL("CREATE TABLE IF NOT EXISTS `collections` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `description` TEXT NOT NULL)")
  db.execSQL("CREATE TABLE IF NOT EXISTS `collection_members` (`collectionId` INTEGER NOT NULL, `pokemonId` INTEGER NOT NULL, PRIMARY KEY(`collectionId`, `pokemonId`), FOREIGN KEY(`collectionId`) REFERENCES `collections`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
  db.execSQL("CREATE INDEX IF NOT EXISTS `index_collection_members_pokemonId` ON `collection_members` (`pokemonId`)")
 }
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
 @Provides @Singleton fun db(@ApplicationContext c:Context):AppDatabase =
  Room.databaseBuilder(c,AppDatabase::class.java,"pokeplanner.db").addMigrations(MIGRATION_1_2).build()
 @Provides fun dao(db:AppDatabase):PokeDao=db.dao()
 @Provides @Singleton fun api():PokeApi=Retrofit.Builder()
  .baseUrl("https://pokeapi.co/api/v2/")
  .addConverterFactory(GsonConverterFactory.create()).build().create(PokeApi::class.java)
}
