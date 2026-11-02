package com.example.pokeplanner.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.pokeplanner.ui.components.ErrorMessage

@Composable
fun CollectionEditor(
    form: CollectionForm,
    onDismiss: () -> Unit,
    onName: (String) -> Unit,
    onDescription: (String) -> Unit,
    onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (form.collectionId == null) "Новая коллекция" else "Редактирование коллекции") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    form.name,
                    onName,
                    enabled = !form.saving,
                    label = { Text("Название коллекции") },
                    modifier = Modifier.fillMaxWidth().testTag("collection-name"),
                )
                OutlinedTextField(
                    form.description,
                    onDescription,
                    label = { Text("Описание") },
                    enabled = !form.saving,
                )
                form.error?.let { ErrorMessage(it) }
            }
        },
        confirmButton = {
            TextButton(enabled = form.name.isNotBlank() && !form.saving, onClick = onSave) {
                Text(if (form.saving) "Сохранение…" else "Сохранить коллекцию")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !form.saving) { Text("Отмена") } },
    )
}
