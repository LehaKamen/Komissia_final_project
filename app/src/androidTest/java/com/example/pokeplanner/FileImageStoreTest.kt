package com.example.pokeplanner

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.example.pokeplanner.data.FileImageStore
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

class FileImageStoreTest {
    @Test
    fun absentApiSpriteCreatesLocalPlaceholderWithoutNetwork() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = File(context.filesDir, "pokemon-images/999993.png")
        file.delete()
        val client = OkHttpClient.Builder().addInterceptor { throw IOException("Unexpected network request") }.build()
        try {
            val store = FileImageStore(context, client)
            assertTrue(store.exists(store.save(999993, null)))
            assertTrue(file.length() > 0)
        } finally {
            file.delete()
        }
    }

    @Test
    fun downloadedImageSurvivesNewStoreInstanceWithoutNetwork() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = File(context.filesDir, "pokemon-images/999991.png")
        file.delete()
        var requests = 0
        val bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        val bytes = ByteArrayOutputStream().apply {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, this)
        }.toByteArray()
        bitmap.recycle()
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            requests++
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK").body(bytes.toResponseBody("image/png".toMediaType())).build()
        }.build()
        try {
            val first = FileImageStore(context, client)
            val path = first.save(999991, "https://example.invalid/image.png")
            assertTrue(first.exists(path))
            val offlineClient = OkHttpClient.Builder().addInterceptor { throw IOException("offline") }.build()
            val reopened = FileImageStore(context, offlineClient)
            assertEquals(path, reopened.save(999991, "https://example.invalid/image.png"))
            assertEquals(1, requests)
        } finally {
            file.delete()
        }
    }

    @Test
    fun invalidImageIsNeverPublishedAsSavedFile() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = File(context.filesDir, "pokemon-images/999992.png")
        file.delete()
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK").body("not an image".toResponseBody()).build()
        }.build()
        try {
            FileImageStore(context, client).save(999992, "https://example.invalid/image.png")
            fail("Invalid image must fail")
        } catch (_: IOException) {
            assertFalse(file.exists())
        }
    }
}
