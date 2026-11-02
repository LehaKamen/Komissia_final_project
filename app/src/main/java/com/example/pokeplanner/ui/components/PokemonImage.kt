package com.example.pokeplanner.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import coil.compose.SubcomposeAsyncImage
import java.io.File

@Composable
fun PokemonImage(path: String?, name: String, modifier: Modifier = Modifier) {
    if (path == null || !File(path).exists()) {
        Box(modifier, contentAlignment = Alignment.Center) { Text("№") }
    } else {
        SubcomposeAsyncImage(
            model = File(path),
            contentDescription = name,
            modifier = modifier,
            error = { Box(contentAlignment = Alignment.Center) { Text("№") } },
        )
    }
}
