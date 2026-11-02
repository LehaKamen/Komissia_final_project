package com.example.pokeplanner.data

object CollectionNames {
    fun normalize(name: String): String {
        val result = name.trim()
        require(result.isNotEmpty()) { "Введите название коллекции" }
        require(result.length <= 100) { "Название должно быть не длиннее 100 символов" }
        return result
    }
}
