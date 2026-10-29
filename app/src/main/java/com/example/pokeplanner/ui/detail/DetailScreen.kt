package com.example.pokeplanner.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pokeplanner.model.PokemonStatus
import com.example.pokeplanner.ui.components.ErrorMessage
import com.example.pokeplanner.ui.components.PokemonImage

@Composable
fun DetailRoute(onBack: () -> Unit, viewModel: DetailVm = hiltViewModel()) {
    val pokemon by viewModel.pokemon.collectAsStateWithLifecycle()
    val personal by viewModel.personal.collectAsStateWithLifecycle()
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    val members by viewModel.members.collectAsStateWithLifecycle()
    val load by viewModel.load.collectAsStateWithLifecycle()
    var note by rememberSaveable(viewModel.id, personal?.note) { mutableStateOf(personal?.note.orEmpty()) }
    var statusName by rememberSaveable(viewModel.id, personal?.status) {
        mutableStateOf((personal?.status ?: PokemonStatus.PLANNED).name)
    }
    val status = PokemonStatus.fromStored(statusName)

    LazyColumn(Modifier.padding(16.dp).testTag("detail-screen")) {
        item {
            TextButton(onClick = onBack) { Text("Назад") }
            if (load.loading) CircularProgressIndicator()
            load.error?.let { ErrorMessage(it, viewModel::retry) }
            load.message?.let { Text(it) }
        }
        val item = pokemon
        if (item != null) {
            item {
                Text(
                    item.name,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.testTag("detail-name"),
                )
                Text("№${item.id}")
                PokemonImage(item.localImage, item.name, Modifier.size(130.dp))
                Text("Тип: ${item.types}")
                if (item.localImage == null) {
                    Text("Изображение ещё не сохранено. Подключитесь к сети и нажмите «Повторить».")
                }
                HorizontalDivider()
                Text("Личная запись", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Моя заметка") },
                    modifier = Modifier.fillMaxWidth().testTag("note"),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    PokemonStatus.entries.forEach { choice ->
                        FilterChip(
                            selected = status == choice,
                            onClick = { statusName = choice.name },
                            label = { Text(choice.title) },
                        )
                    }
                }
                Row {
                    Button(onClick = { viewModel.save(status, note) }) { Text("Сохранить") }
                    TextButton(onClick = { viewModel.remove() }) { Text("Удалить запись") }
                }
                HorizontalDivider()
                Text("Участие в коллекциях", style = MaterialTheme.typography.titleMedium)
                if (collections.isEmpty()) Text("Создайте коллекцию на вкладке «Коллекции».")
            }
            items(collections, key = { it.id }) { collection ->
                val checked = members.any { it.collectionId == collection.id && it.pokemonId == item.id }
                Row(Modifier.fillMaxWidth().clickable { viewModel.toggleMember(collection.id, checked) }) {
                    Checkbox(checked, onCheckedChange = { viewModel.toggleMember(collection.id, checked) })
                    Text(collection.name, Modifier.padding(top = 12.dp))
                }
            }
        } else if (!load.loading && load.error == null) {
            item { Text("Карточка пока не сохранена. Для первой загрузки нужен интернет.") }
        }
    }
}
