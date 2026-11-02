package com.example.pokeplanner.data

interface ImageStore {
    suspend fun save(id: Int, url: String?): String
    fun exists(path: String?): Boolean
}
