package com.example.eastsyria.CategoryList

data class HistoricalSite(
    val id: Int,
    val name: String,
    val location: String,
    val description: String,
    val imageUrl: String,
    val rating: Double,
    val reviewCount: String,
    val badge: String,
    var isBookmarked: Boolean = false
)

