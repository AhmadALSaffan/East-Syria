package com.example.eastsyria.CategoryList

import java.io.Serializable

data class CategoryItem(
    val id: String = "",
    val name: String = "",
    val nameArabic: String = "",
    val description: String = "",
    val descriptionArabic: String = "",
    val longDescription: String = "",
    val longDescriptionArabic: String = "",
    val category: String = "",
    val categoryArabic: String = "",
    val imageUrl: String = "",
    val rating: Double = 0.0,
    val reviewCount: Int = 0,
    val status: String = "",
    val statusArabic: String = "",
    val location: Location = Location(),
    val details: Map<String, Any> = emptyMap(),
    val tags: List<String> = emptyList(),
    val tagsArabic: List<String> = emptyList(),
    val entryFee: String = "",
    val visitingHours: String = "",
    val bestTimeToVisit: String = "",
    val isFeatured: Boolean = false,
    val isHistorical: Boolean = false,
    val isTrending: Boolean = false,
    var isBookmarked: Boolean = false,
    val createdAt: Long = 0,
    val updatedAt: Long = 0
) : Serializable {

    data class Location(
        val city: String = "",
        val cityArabic: String = "",
        val governorate: String = "",
        val governorateArabic: String = "",
        val region: String = "",
        val latitude: Double = 0.0,
        val longitude: Double = 0.0
    ) : Serializable


    fun getFormattedReviewCount(): String {
        return when {
            reviewCount >= 1000 -> "${String.format("%.1f", reviewCount / 1000.0)}k reviews"
            reviewCount > 0 -> "$reviewCount reviews"
            else -> ""
        }
    }

    fun getBadgeText(): String {
        return when {
            details.containsKey("unescoStatus") &&
                    details["unescoStatus"].toString().contains("UNESCO") -> "UNESCO WORLD HERITAGE"
            category.equals("Archaeological", ignoreCase = true) -> status.uppercase()
            category.equals("Historical", ignoreCase = true) -> status.uppercase()
            else -> ""
        }
    }
}
