package com.example.pokeplanner

import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.example.pokeplanner.data.local.AppDatabase
import com.example.pokeplanner.work.SyncScheduler
import com.example.pokeplanner.work.SyncWorker
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidTest
class WorkerSchedulingTest {
    @get:Rule val hilt = HiltAndroidRule(this)

    @Inject lateinit var factory: HiltWorkerFactory

    @Inject lateinit var database: AppDatabase

    @Inject lateinit var api: TestApi
    private lateinit var manager: WorkManager

    @Before
    fun setup() {
        hilt.inject()
        val context = ApplicationProvider.getApplicationContext<Context>()
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().setWorkerFactory(factory).setExecutor(SynchronousExecutor()).build(),
        )
        manager = WorkManager.getInstance(context)
    }

    @After
    fun close() {
        manager.cancelAllWork().result.get(10, TimeUnit.SECONDS)
        WorkManagerTestInitHelper.closeWorkDatabase()
        database.close()
    }

    @Test
    fun workManagerCreatesWorkerThroughHiltAndWritesRealRoomData() = runBlocking {
        val request = OneTimeWorkRequestBuilder<SyncWorker>().build()
        manager.enqueue(request).result.get(10, TimeUnit.SECONDS)
        val info = withTimeout(20_000) {
            manager.getWorkInfoByIdFlow(request.id).first { it?.state?.isFinished == true }
        }
        assertEquals(WorkInfo.State.SUCCEEDED, info?.state)
        assertEquals(60, database.dao().pokemon().first().size)
        assertNotNull(database.dao().lastCompleteSync())
        assertEquals(1, api.listCalls.get())
    }

    @Test
    fun networkFailureIsRescheduledByWorkManagerWithoutFreshCache() = runBlocking {
        api.offline = true
        val request = OneTimeWorkRequestBuilder<SyncWorker>().build()
        manager.enqueue(request).result.get(10, TimeUnit.SECONDS)
        val info = withTimeout(20_000) {
            manager.getWorkInfoByIdFlow(request.id).first {
                it != null &&
                    it.runAttemptCount >= 1 &&
                    it.state == WorkInfo.State.ENQUEUED
            }
        }
        assertEquals(WorkInfo.State.ENQUEUED, info?.state)
        assertNull(database.dao().lastCompleteSync())
    }

    @Test
    fun changingTtlUpdatesSamePeriodicWork() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val scheduler = SyncScheduler(context)
        scheduler.schedule(6)
        val first = withTimeout(10_000) {
            manager.getWorkInfosForUniqueWorkFlow(SyncScheduler.WORK_NAME).first { it.isNotEmpty() }.single()
        }
        assertEquals(TimeUnit.HOURS.toMillis(6), first.periodicityInfo?.repeatIntervalMillis)
        scheduler.schedule(24)
        val updated = withTimeout(10_000) {
            manager.getWorkInfosForUniqueWorkFlow(SyncScheduler.WORK_NAME)
                .first {
                    it.singleOrNull()?.periodicityInfo?.repeatIntervalMillis == TimeUnit.HOURS.toMillis(24)
                }.single()
        }
        assertEquals(first.id, updated.id)
    }
}
