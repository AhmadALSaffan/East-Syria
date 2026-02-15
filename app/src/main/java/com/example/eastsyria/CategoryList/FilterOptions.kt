package com.example.eastsyria.CategoryList

data class FilterOptions(
    var minRating: Double = 0.0,
    var statusFilters: MutableList<String> = mutableListOf(),
    var isFeatured: Boolean? = null,
    var isTrending: Boolean? = null
) {
    fun isActive(): Boolean {
        return minRating > 0.0 ||
                statusFilters.isNotEmpty() ||
                isFeatured == true ||
                isTrending == true
    }

    fun reset() {
        minRating = 0.0
        statusFilters.clear()
        isFeatured = null
        isTrending = null
    }
}
