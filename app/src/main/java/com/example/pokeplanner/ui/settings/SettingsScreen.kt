package com.example.pokeplanner.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pokeplanner.data.CachePolicy
import com.example.pokeplanner.ui.components.ErrorMessage

@Composable
fun SettingsRoute(viewModel: SettingsVm = hiltViewModel()) {
    val settings by viewModel.state.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Настройки", style = MaterialTheme.typography.headlineMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Тёмная тема")
            Switch(settings.dark, viewModel::dark)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Только запланированные")
            Switch(settings.onlyPlanned, viewModel::planned)
        }
        Text("Срок актуальности кэша: ${settings.refreshHours} ч")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CachePolicy.allowedHours.forEach { hours ->
                FilterChip(settings.refreshHours == hours, { viewModel.hours(hours) }, label = { Text("$hours ч") })
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Фоновое обновление запланировано раз в ${settings.refreshHours} ч при наличии сети. Android может отложить запуск для экономии батареи.",
        )
        Text("Пока приложение открыто, срок кэша проверяется каждую минуту и при смене этой настройки.")
        Spacer(Modifier.height(12.dp))
        Text(
            "Поиск использует полный сохранённый список имён. Карточки и изображения доступны офлайн после успешного сохранения. Для первого открытия остальных карточек нужен интернет.",
        )
        error?.let { ErrorMessage(it) }
    }
}
