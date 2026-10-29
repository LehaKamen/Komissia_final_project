package com.example.pokeplanner.ui.catalog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pokeplanner.ui.components.ErrorMessage
import com.example.pokeplanner.ui.components.PokemonImage

@Composable
fun CatalogRoute(onOpen: (Int) -> Unit, viewModel: MainVm = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    CatalogScreen(state, query, viewModel::search, viewModel::retry, onOpen)
}

@Composable
fun CatalogScreen(
    state: CatalogState,
    query: String,
    onQuery: (String) -> Unit,
    onRefresh: () -> Unit,
    onOpen: (Int) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("ПокеПланнер", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            label = { Text("Поиск по английскому имени или номеру") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("search"),
        )
        TextButton(onClick = onRefresh, enabled = !state.loading) { Text("Обновить каталог") }
        if (state.loading) CircularProgressIndicator(Modifier.testTag("catalog-loading"))
        state.error?.let { ErrorMessage(it, onRefresh) }
        if (!state.loading && state.items.isEmpty() && state.error == null) {
            Text("Ничего не найдено. Измените запрос или фильтр.")
        }
        LazyColumn(Modifier.weight(1f).testTag("catalog-list")) {
            items(state.items, key = { it.id }) { item ->
                ListItem(
                    headlineContent = { Text(item.name) },
                    supportingContent = {
                        Text(
                            if (item.detailsSaved) {
                                "№${item.id} • ${item.types}"
                            } else {
                                "№${item.id} • Откройте онлайн, чтобы сохранить карточку"
                            },
                        )
                    },
                    leadingContent = { PokemonImage(item.localImage, item.name, Modifier.size(56.dp)) },
                    modifier = Modifier.clickable { onOpen(item.id) }.testTag("pokemon-${item.id}"),
                )
                HorizontalDivider()
            }
        }
    }
}
