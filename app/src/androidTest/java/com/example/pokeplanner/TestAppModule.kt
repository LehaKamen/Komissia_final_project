package com.example.pokeplanner

import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import com.example.pokeplanner.data.Clock
import com.example.pokeplanner.data.ImageStore
import com.example.pokeplanner.data.Settings
import com.example.pokeplanner.data.SettingsSource
import com.example.pokeplanner.data.local.AppDatabase
import com.example.pokeplanner.data.local.PokeDao
import com.example.pokeplanner.data.remote.DetailDto
import com.example.pokeplanner.data.remote.ListDto
import com.example.pokeplanner.data.remote.Named
import com.example.pokeplanner.data.remote.PokeApi
import com.example.pokeplanner.data.remote.Sprites
import com.example.pokeplanner.di.AppModule
import com.example.pokeplanner.work.BackgroundDispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Singleton

class TestApi : PokeApi {
    @Volatile var offline = false
    val listCalls = AtomicInteger()
    val detailCalls = AtomicInteger()

    override suspend fun list(limit: Int): ListDto {
        listCalls.incrementAndGet()
        if (offline) throw IOException("offline")
        return ListDto((1..151).map { Named(name(it), "https://pokeapi.co/api/v2/pokemon/$it/") })
    }

    override suspend fun detail(id: Int): DetailDto {
        detailCalls.incrementAndGet()
        if (offline) throw IOException("offline")
        return DetailDto(id, name(id), Sprites("https://images/$id.png"), emptyList())
    }

    private fun name(id: Int) = when (id) {
        25 -> "pikachu"
        150 -> "mewtwo"
        else -> "pokemon$id"
    }
}

class TestSettings : SettingsSource {
    override val flow = MutableStateFlow(Settings())
    override suspend fun dark(value: Boolean) {
        flow.value = flow.value.copy(dark = value)
    }
    override suspend fun planned(value: Boolean) {
        flow.value = flow.value.copy(onlyPlanned = value)
    }
    override suspend fun hours(value: Int) {
        flow.value = flow.value.copy(refreshHours = value)
    }
}

class TestImages(private val directory: File) : ImageStore {
    override fun exists(path: String?) = path != null && File(path).isFile

    override suspend fun save(id: Int, url: String?): String {
        directory.mkdirs()
        val file = File(directory, "$id.png")
        if (!file.exists()) {
            val bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
        return file.absolutePath
    }
}

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [AppModule::class])
object TestAppModule {
    @Provides @Singleton
    fun database(@ApplicationContext context: Context): AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()

    @Provides fun dao(database: AppDatabase): PokeDao = database.dao()

    @Provides @Singleton
    fun testApi() = TestApi()

    @Provides fun api(api: TestApi): PokeApi = api

    @Provides @Singleton
    fun testSettings() = TestSettings()

    @Provides fun settings(settings: TestSettings): SettingsSource = settings

    @Provides @Singleton
    fun images(@ApplicationContext context: Context): ImageStore =
        TestImages(File(context.cacheDir, "test-pokemon-images"))

    @Provides fun clock(): Clock = Clock(System::currentTimeMillis)

    @Provides @BackgroundDispatcher
    fun dispatcher(): CoroutineDispatcher = Dispatchers.Default
}
