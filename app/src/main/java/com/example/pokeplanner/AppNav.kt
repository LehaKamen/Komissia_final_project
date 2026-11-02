package com.example.pokeplanner

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.pokeplanner.ui.catalog.CatalogRoute
import com.example.pokeplanner.ui.detail.DetailRoute
import com.example.pokeplanner.ui.library.CollectionsRoute
import com.example.pokeplanner.ui.library.HistoryRoute
import com.example.pokeplanner.ui.settings.SettingsRoute

private enum class Destination(val route: String, val title: String) {
    CATALOG("list", "Каталог"),
    COLLECTIONS("collections", "Коллекции"),
    HISTORY("recent", "История"),
    SETTINGS("settings", "Настройки"),
}

@Composable
fun AppNav() {
    val navigation = rememberNavController()
    val backStack by navigation.currentBackStackEntryAsState()
    val openPokemon: (Int) -> Unit = { navigation.navigate("detail/$it") }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Destination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = backStack?.destination?.route == destination.route,
                        onClick = {
                            navigation.navigate(destination.route) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(navigation.graph.startDestinationId) { saveState = true }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = when (destination) {
                                    Destination.CATALOG -> Icons.Default.Search
                                    Destination.COLLECTIONS -> Icons.Default.Star
                                    Destination.HISTORY -> Icons.Default.History
                                    Destination.SETTINGS -> Icons.Default.Settings
                                },
                                contentDescription = destination.title,
                                modifier = Modifier.size(30.dp),
                            )
                        },
                        label = { Text(destination.title) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(navigation, startDestination = "list", modifier = Modifier.padding(padding)) {
            composable("list") { CatalogRoute(openPokemon) }
            composable("collections") { CollectionsRoute(openPokemon) }
            composable("recent") { HistoryRoute(openPokemon) }
            composable("settings") { SettingsRoute() }
            composable("detail/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType })) {
                DetailRoute(onBack = { navigation.popBackStack() })
            }
        }
    }
}
