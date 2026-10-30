package com.example.pokeplanner.work

import com.example.pokeplanner.data.Repository
import com.example.pokeplanner.data.SettingsSource
import com.example.pokeplanner.data.userMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BackgroundDispatcher

data class SyncState(val loading: Boolean = false, val error: String? = null)

@Singleton
class SyncController @Inject constructor(
    private val repository: Repository,
    private val settings: SettingsSource,
    @BackgroundDispatcher dispatcher: CoroutineDispatcher,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val requests = MutableSharedFlow<Boolean>(replay = 1)
    private val mutableState = MutableStateFlow(SyncState())
    private var foregroundJob: Job? = null
    val state = mutableState.asStateFlow()

    init {
        scope.launch {
            requests.collect { force ->
                mutableState.value = SyncState(loading = true)
                try {
                    repository.refreshIfNeeded(settings.flow.first().refreshHours, force)
                    mutableState.value = SyncState()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    mutableState.value = SyncState(error = userMessage(error))
                }
            }
        }
    }

    fun refresh() {
        scope.launch { requests.emit(true) }
    }

    fun setForeground(active: Boolean) {
        if (!active) {
            foregroundJob?.cancel()
            foregroundJob = null
        } else if (foregroundJob == null) {
            foregroundJob = scope.launch {
                settings.flow.map { it.refreshHours }.distinctUntilChanged().collectLatest {
                    while (isActive) {
                        requests.emit(false)
                        delay(60_000)
                    }
                }
            }
        }
    }

    fun close() {
        scope.coroutineContext[Job]?.cancel()
    }
}
