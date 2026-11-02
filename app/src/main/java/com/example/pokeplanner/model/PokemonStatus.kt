package com.example.pokeplanner.model

enum class PokemonStatus(val title: String) {
    PLANNED("В планах"),
    TRAINING("Тренирую"),
    READY("Готов"),
    ;

    companion object {
        fun fromStored(value: String): PokemonStatus = entries.firstOrNull { it.name == value } ?: PLANNED
    }
}
