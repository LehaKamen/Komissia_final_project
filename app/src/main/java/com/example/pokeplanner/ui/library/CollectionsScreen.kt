package com.example.pokeplanner.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pokeplanner.ui.components.ErrorMessage

@Composable
fun CollectionsRoute(onOpen: (Int) -> Unit, viewModel: LibraryVm = hiltViewModel()) {
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    val members by viewModel.members.collectAsStateWithLifecycle()
    val catalog by viewModel.catalog.collectAsStateWithLifecycle()
    val personal by viewModel.personal.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val editor by viewModel.editor.collectAsStateWithLifecycle()
    var deletingId by rememberSaveable { mutableStateOf<Long?>(null) }
    val names = remember(catalog) { catalog.associate { it.id to it.name } }

    LazyColumn(Modifier.padding(12.dp)) {
        item {
            Text("Мои коллекции", style = MaterialTheme.typography.headlineMedium)
            Text("Коллекции и личные записи сохраняются на устройстве.")
            Button(onClick = viewModel::createCollection) { Text("Создать коллекцию") }
            error?.let { ErrorMessage(it) }
        }
        items(collections, key = { "collection-${it.id}" }) { collection ->
            val ids = members.filter { it.collectionId == collection.id }.map { it.pokemonId }
            Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text(collection.name, style = MaterialTheme.typography.titleMedium)
                    if (collection.description.isNotBlank()) Text(collection.description)
                    Text("Покемонов: ${ids.size}")
                    ids.forEach { id ->
                        TextButton(onClick = { onOpen(id) }) { Text(names[id] ?: "Покемон №$id") }
                    }
                    Row {
                        TextButton(onClick = { viewModel.editCollection(collection) }) { Text("Изменить") }
                        TextButton(onClick = { deletingId = collection.id }) { Text("Удалить коллекцию") }
                    }
                }
            }
        }
        item {
            HorizontalDivider()
            Text("Личные записи (${personal.size})", style = MaterialTheme.typography.titleMedium)
        }
        items(personal, key = { "personal-${it.pokemonId}" }) { entry ->
            ListItem(
                headlineContent = { Text(names[entry.pokemonId] ?: "Покемон №${entry.pokemonId}") },
                supportingContent = { Text("${entry.status.title} • ${entry.note}") },
                modifier = Modifier.clickable { onOpen(entry.pokemonId) },
            )
        }
    }
    editor?.let { form ->
        CollectionEditor(
            form = form,
            onDismiss = viewModel::closeEditor,
            onName = viewModel::editorName,
            onDescription = viewModel::editorDescription,
            onSave = viewModel::saveEditor,
        )
    }
    val deleting = collections.find { it.id == deletingId }
    if (deleting != null) {
        AlertDialog(
            onDismissRequest = { deletingId = null },
            title = { Text("Удалить «${deleting.name}»?") },
            text = { Text("Коллекция и её связи будут удалены. Покемоны, заметки и статусы сохранятся.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(deleting.id)
                    deletingId = null
                }) { Text("Подтвердить удаление") }
            },
            dismissButton = { TextButton(onClick = { deletingId = null }) { Text("Отмена") } },
        )
    }
}
