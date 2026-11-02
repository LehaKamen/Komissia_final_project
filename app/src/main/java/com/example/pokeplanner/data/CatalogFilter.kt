package com.example.pokeplanner.data

import com.example.pokeplanner.data.local.PersonalEntity
import com.example.pokeplanner.model.CatalogItem
import com.example.pokeplanner.model.PokemonStatus

object CatalogFilter {
    fun apply(
        items: List<CatalogItem>,
        personal: List<PersonalEntity>,
        query: String,
        onlyPlanned: Boolean,
    ): List<CatalogItem> {
        val plannedIds = personal.filter { it.status == PokemonStatus.PLANNED }
            .mapTo(mutableSetOf()) { it.pokemonId }
        val normalized = query.trim()
        val number = normalized.removePrefix("#").takeIf { value ->
            value.isNotEmpty() && value.all { it in '0'..'9' }
        }?.toIntOrNull()
        return items.filter { item ->
            (item.name.contains(normalized, ignoreCase = true) || item.id == number) &&
                (!onlyPlanned || item.id in plannedIds)
        }
    }
}
