package com.example.pokeplanner.di

import android.content.Context
import androidx.room.Room
import com.example.pokeplanner.data.Clock
import com.example.pokeplanner.data.FileImageStore
import com.example.pokeplanner.data.ImageStore
import com.example.pokeplanner.data.SettingsRepository
import com.example.pokeplanner.data.SettingsSource
import com.example.pokeplanner.data.local.AppDatabase
import com.example.pokeplanner.data.local.MIGRATION_1_2
import com.example.pokeplanner.data.local.MIGRATION_2_3
import com.example.pokeplanner.data.local.PokeDao
import com.example.pokeplanner.data.remote.PokeApi
import com.example.pokeplanner.work.BackgroundDispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "pokeplanner.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()

    @Provides
    fun dao(database: AppDatabase): PokeDao = database.dao()

    @Provides
    @Singleton
    fun httpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun api(client: OkHttpClient): PokeApi = Retrofit.Builder()
        .baseUrl("https://pokeapi.co/api/v2/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(PokeApi::class.java)

    @Provides
    fun images(store: FileImageStore): ImageStore = store

    @Provides
    fun settings(repository: SettingsRepository): SettingsSource = repository

    @Provides
    fun clock(): Clock = Clock(System::currentTimeMillis)

    @Provides
    @BackgroundDispatcher
    fun dispatcher(): CoroutineDispatcher = Dispatchers.IO
}
