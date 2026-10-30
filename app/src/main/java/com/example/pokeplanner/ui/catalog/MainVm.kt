package com.example.pokeplanner.ui.catalog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pokeplanner.data.CatalogFilter
import com.example.pokeplanner.data.Repository
import com.example.pokeplanner.data.SettingsSource
import com.example.pokeplanner.model.CatalogItem
import com.example.pokeplanner.work.SyncController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class CatalogState(
    val items: List<CatalogItem> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class MainVm @Inject constructor(
    private val savedState: SavedStateHandle,
    repository: Repository,
    settings: SettingsSource,
    private val sync: SyncController,
) : ViewModel() {
    val query = savedState.getStateFlow("query", "")
    val state = combine(
        repository.catalog,
        repository.personal,
        settings.flow,
        query.debounce(350).distinctUntilChanged(),
        sync.state,
    ) { items, personal, preferences, search, syncState ->
        CatalogState(
            items = CatalogFilter.apply(items, personal, search, preferences.onlyPlanned),
            loading = syncState.loading,
            error = syncState.error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CatalogState(loading = true))

    fun search(value: String) {
        savedState["query"] = value
    }

    fun retry() = sync.refresh()
}
