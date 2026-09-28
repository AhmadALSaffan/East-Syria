package com.example.eastsyria.Admin.Reports

/** A landmark in the highest-rated list. The rating is the curator's mark, not a review score. */
data class PopularLandmarkItem(
    val name: String,
    val category: String,
    val rating: Float,
    val imageUrl: String
)
