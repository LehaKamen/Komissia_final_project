package com.example.pokeplanner.ui.library

data class CollectionForm(
    val collectionId: Long? = null,
    val name: String = "",
    val description: String = "",
    val saving: Boolean = false,
    val error: String? = null,
)
