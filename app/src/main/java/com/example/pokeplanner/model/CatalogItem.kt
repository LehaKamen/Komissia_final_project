package com.example.pokeplanner.model

data class CatalogItem(
    val id: Int,
    val name: String,
    val types: String,
    val localImage: String?,
    val detailsSaved: Boolean,
)
