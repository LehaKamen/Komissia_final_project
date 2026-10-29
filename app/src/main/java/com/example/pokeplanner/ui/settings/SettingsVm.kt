package com.example.pokeplanner.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pokeplanner.data.Repository
import com.example.pokeplanner.data.Settings
import com.example.pokeplanner.data.SettingsSource
import com.example.pokeplanner.data.userMessage
import com.example.pokeplanner.work.SyncController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsVm @Inject constructor(
    private val settings: SettingsSource,
    private val repository: Repository,
    private val sync: SyncController,
) : ViewModel() {
    val state = settings.flow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Settings())
    private val mutableError = MutableStateFlow<String?>(null)
    val error = mutableError.asStateFlow()

    fun foreground(active: Boolean) = sync.setForeground(active)
    fun dark(value: Boolean) = change { settings.dark(value) }
    fun planned(value: Boolean) = change { settings.planned(value) }
    fun hours(value: Int) = change { settings.hours(value) }
    fun clear() = change { repository.clearRecent() }

    private fun change(action: suspend () -> Unit) = viewModelScope.launch {
        mutableError.value = null
        try {
            action()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            mutableError.value = userMessage(error)
        }
    }
}
