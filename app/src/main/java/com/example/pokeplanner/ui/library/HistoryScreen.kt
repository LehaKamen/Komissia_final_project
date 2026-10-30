package com.example.pokeplanner.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pokeplanner.ui.components.ErrorMessage

@Composable
fun HistoryRoute(onOpen: (Int) -> Unit, viewModel: LibraryVm = hiltViewModel()) {
    val history by viewModel.recent.collectAsStateWithLifecycle()
    val catalog by viewModel.catalog.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    var confirming by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.padding(12.dp)) {
        Text("Недавно просмотренные", style = MaterialTheme.typography.headlineMedium)
        TextButton(enabled = history.isNotEmpty(), onClick = { confirming = true }) { Text("Очистить историю") }
        error?.let { ErrorMessage(it) }
        if (history.isEmpty()) Text("Вы ещё не просматривали покемонов")
        LazyColumn {
            items(history, key = { it.pokemonId }) { entry ->
                ListItem(
                    headlineContent = {
                        Text(
                            catalog.find { it.id == entry.pokemonId }?.name ?: "Покемон №${entry.pokemonId}",
                        )
                    },
                    supportingContent = { Text("Открыть сохранённую карточку") },
                    modifier = Modifier.clickable { onOpen(entry.pokemonId) },
                )
            }
        }
    }
    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text("Очистить историю просмотров?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearHistory()
                    confirming = false
                }) { Text("Очистить") }
            },
            dismissButton = { TextButton(onClick = { confirming = false }) { Text("Отмена") } },
        )
    }
}
