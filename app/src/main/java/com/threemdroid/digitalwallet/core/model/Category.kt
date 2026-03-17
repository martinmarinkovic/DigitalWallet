package com.threemdroid.digitalwallet.core.model

import java.time.Instant

data class Category(
    val id: String,
    val name: String,
    val color: String,
    val isDefault: Boolean,
    val isFavorites: Boolean,
    val position: Int,
    val createdAt: Instant,
    val updatedAt: Instant
)
