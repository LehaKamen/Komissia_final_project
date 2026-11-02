package com.example.pokeplanner.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pokeplanner.data.Repository
import com.example.pokeplanner.data.userMessage
import com.example.pokeplanner.model.PokemonStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailLoadState(val loading: Boolean = false, val error: String? = null, val message: String? = null)

@HiltViewModel
class DetailVm @Inject constructor(savedState: SavedStateHandle, private val repository: Repository) : ViewModel() {
    val id: Int = checkNotNull(savedState["id"])
    val pokemon = repository.detail(id).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val personal = repository.personal(id).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val collections = repository.collections.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val members = repository.members.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val mutableLoad = MutableStateFlow(DetailLoadState())
    val load = mutableLoad.asStateFlow()
    private var loadJob: Job? = null

    init {
        retry()
    }

    fun retry() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            mutableLoad.value = DetailLoadState(loading = true)
            try {
                repository.openPokemon(id)
                mutableLoad.value = DetailLoadState()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableLoad.value = DetailLoadState(error = userMessage(error))
            }
        }
    }

    fun save(status: PokemonStatus, note: String) = perform("Запись сохранена") {
        repository.save(id, status, note)
    }

    fun remove() = perform("Личная запись удалена") { repository.remove(id) }

    fun toggleMember(collectionId: Long, exists: Boolean) = perform(null) {
        if (exists) repository.removeMember(collectionId, id) else repository.addMember(collectionId, id)
    }

    private fun perform(message: String?, action: suspend () -> Unit) = viewModelScope.launch {
        try {
            action()
            mutableLoad.value = mutableLoad.value.copy(message = message)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            mutableLoad.value = mutableLoad.value.copy(error = userMessage(error), message = null)
        }
    }
}
