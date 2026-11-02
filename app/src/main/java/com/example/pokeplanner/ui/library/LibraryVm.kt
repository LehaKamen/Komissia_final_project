package com.example.pokeplanner.ui.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pokeplanner.data.Repository
import com.example.pokeplanner.data.local.CollectionEntity
import com.example.pokeplanner.data.userMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryVm @Inject constructor(private val repository: Repository, private val savedState: SavedStateHandle) :
    ViewModel() {
    val catalog = repository.catalog.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val personal = repository.personal.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val collections = repository.collections.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val members = repository.members.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val recent = repository.recent.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val mutableError = MutableStateFlow<String?>(null)
    val error = mutableError.asStateFlow()
    private val mutableEditor = MutableStateFlow(restoreEditor())
    val editor = mutableEditor.asStateFlow()

    fun createCollection() {
        dismissError()
        updateEditor(CollectionForm())
    }

    fun editCollection(item: CollectionEntity) {
        dismissError()
        updateEditor(CollectionForm(item.id, item.name, item.description))
    }

    fun editorName(value: String) {
        mutableEditor.value?.takeUnless { it.saving }?.let { updateEditor(it.copy(name = value, error = null)) }
    }

    fun editorDescription(value: String) {
        mutableEditor.value?.takeUnless { it.saving }?.let { updateEditor(it.copy(description = value)) }
    }

    fun closeEditor() {
        if (mutableEditor.value?.saving != true) updateEditor(null)
    }

    fun saveEditor() {
        val form = mutableEditor.value?.takeUnless { it.saving } ?: return
        updateEditor(form.copy(saving = true, error = null))
        viewModelScope.launch {
            try {
                if (form.collectionId == null) {
                    repository.addCollection(form.name, form.description)
                } else {
                    val item = repository.collections.first().find { it.id == form.collectionId }
                        ?: throw IllegalArgumentException("Коллекция уже удалена. Закройте окно редактирования.")
                    repository.renameCollection(item, form.name, form.description)
                }
                updateEditor(null)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                updateEditor(form.copy(error = userMessage(error)))
            }
        }
    }

    private fun restoreEditor(): CollectionForm? = if (savedState.get<Boolean>("editorOpen") == true) {
        CollectionForm(
            collectionId = savedState["editorId"],
            name = savedState["editorName"] ?: "",
            description = savedState["editorDescription"] ?: "",
        )
    } else {
        null
    }

    private fun updateEditor(form: CollectionForm?) {
        mutableEditor.value = form
        savedState["editorOpen"] = form != null
        savedState["editorId"] = form?.collectionId
        savedState["editorName"] = form?.name.orEmpty()
        savedState["editorDescription"] = form?.description.orEmpty()
    }

    fun delete(id: Long) = perform { repository.deleteCollection(id) }
    fun clearHistory() = perform { repository.clearRecent() }
    fun dismissError() {
        mutableError.value = null
    }

    private fun perform(action: suspend () -> Unit) = viewModelScope.launch {
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
