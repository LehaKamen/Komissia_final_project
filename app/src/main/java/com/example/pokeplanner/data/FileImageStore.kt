package com.example.pokeplanner.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileImageStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: OkHttpClient,
) : ImageStore {
    private val imageLocks = ConcurrentHashMap<Int, Mutex>()

    override fun exists(path: String?): Boolean = path != null && File(path).let { it.isFile && it.length() > 0 }

    override suspend fun save(id: Int, url: String?): String = withContext(Dispatchers.IO) {
        imageLocks.computeIfAbsent(id) { Mutex() }.withLock { saveImage(id, url) }
    }

    private fun saveImage(id: Int, url: String?): String {
        val directory = File(context.filesDir, "pokemon-images").apply { mkdirs() }
        val destination = File(directory, "$id.png")
        if (exists(destination.absolutePath)) return destination.absolutePath

        val temporary = File.createTempFile("pokemon-$id-", ".tmp", directory)
        try {
            if (url.isNullOrBlank()) {
                savePlaceholder(id, temporary)
            } else {
                client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException(
                            "Не удалось сохранить изображение: HTTP ${response.code}",
                        )
                    }
                    val body = response.body ?: throw IOException("Сервер вернул пустое изображение")
                    body.byteStream().use { input -> temporary.outputStream().use { output -> input.copyTo(output) } }
                }
            }
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(temporary.absolutePath, options)
            if (options.outWidth <= 0) throw IOException("Сервер вернул некорректное изображение")
            if (!temporary.renameTo(destination)) {
                throw IOException("Не удалось сохранить изображение на устройстве")
            }
            return destination.absolutePath
        } finally {
            temporary.delete()
        }
    }

    private fun savePlaceholder(id: Int, destination: File) {
        val bitmap = Bitmap.createBitmap(160, 160, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.LTGRAY)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 28f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("#$id", 80f, 90f, paint)
        try {
            destination.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } finally {
            bitmap.recycle()
        }
    }
}
