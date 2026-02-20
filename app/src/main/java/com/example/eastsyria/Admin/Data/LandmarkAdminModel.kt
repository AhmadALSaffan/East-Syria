package com.example.eastsyria.Admin.Data

data class LocationModel(
    val city: String = "",
    val cityArabic: String = ""
)

data class LandmarkAdminModel(
    var id: String = "",
    val name: String = "",
    val category: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val status: String = "",
    val location: LocationModel = LocationModel(),
    val isFeatured: Boolean = false,
    val isHistorical: Boolean = false,
    val isTrending: Boolean = false,
    val entryFee: String = "",
    val createdAt: Long = 0
)
