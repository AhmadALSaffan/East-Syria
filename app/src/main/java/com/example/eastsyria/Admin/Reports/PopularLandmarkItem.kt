package com.example.eastsyria.Admin.Reports

data class PopularLandmarkItem(
    val name: String,
    val views: Long,
    val rating: Float,
    val growth: String,
    val imageUrl: String
)
