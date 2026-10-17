package com.example.pokeplanner

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.example.pokeplanner.data.Repository
import com.example.pokeplanner.work.SyncWorker
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class SyncWorkerIntegrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val params = mockk<WorkerParameters>(relaxed = true)
    private val repository = mockk<Repository>()

    @Test fun successfulRefreshReturnsSuccess() = runTest {
        coEvery { repository.refresh() } just Runs
        val result = SyncWorker(context, params, repository).doWork()
        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 1) { repository.refresh() }
    }

    @Test fun failedRefreshReturnsRetry() = runTest {
        coEvery { repository.refresh() } throws IOException("offline")
        val result = SyncWorker(context, params, repository).doWork()
        assertEquals(ListenableWorker.Result.retry(), result)
        coVerify(exactly = 1) { repository.refresh() }
    }
}
