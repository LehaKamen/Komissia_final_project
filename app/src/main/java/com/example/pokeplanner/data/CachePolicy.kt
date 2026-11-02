package com.example.pokeplanner.data

object CachePolicy {
    val allowedHours = listOf(6, 12, 24)

    fun isStale(lastCompleteSync: Long?, now: Long, ttlHours: Int): Boolean =
        lastCompleteSync == null || now - lastCompleteSync >= ttlHours * 3_600_000L

    fun validateHours(hours: Int) {
        require(hours in allowedHours) { "Допустимый срок кэша: 6, 12 или 24 часа" }
    }
}
