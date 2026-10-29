package com.example.pokeplanner.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.pokeplanner.data.Repository
import com.example.pokeplanner.data.SettingsSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val repository: Repository,
    private val settings: SettingsSource,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = try {
        repository.refreshIfNeeded(settings.flow.first().refreshHours)
        Result.success()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.retry()
    }
}
