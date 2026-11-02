package com.example.pokeplanner.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun ErrorMessage(message: String, onRetry: (() -> Unit)? = null) {
    Column {
        Text(message, color = MaterialTheme.colorScheme.error)
        if (onRetry != null) TextButton(onClick = onRetry) { Text("Повторить") }
    }
}
