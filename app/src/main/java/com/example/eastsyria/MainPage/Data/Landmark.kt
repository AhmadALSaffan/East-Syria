package com.example.eastsyria.MainPage.Data

import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName

@IgnoreExtraProperties
data class Landmark(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",

    @get:PropertyName("name") @set:PropertyName("name")
    var name: String = "",

    @get:PropertyName("nameArabic") @set:PropertyName("nameArabic")
    var nameArabic: String = "",

    @get:PropertyName("category") @set:PropertyName("category")
    var category: String = "",

    @get:PropertyName("categoryArabic") @set:PropertyName("categoryArabic")
    var categoryArabic: String = "",

    @get:PropertyName("description") @set:PropertyName("description")
    var description: String = "",

    @get:PropertyName("descriptionArabic") @set:PropertyName("descriptionArabic")
    var descriptionArabic: String = "",

    @get:PropertyName("longDescription") @set:PropertyName("longDescription")
    var longDescription: String = "",

    @get:PropertyName("longDescriptionArabic") @set:PropertyName("longDescriptionArabic")
    var longDescriptionArabic: String = "",

    @get:PropertyName("imageUrl") @set:PropertyName("imageUrl")
    var imageUrl: String = "",

    @get:PropertyName("location") @set:PropertyName("location")
    var location: Location = Location(),

    @get:PropertyName("details") @set:PropertyName("details")
    var details: Map<String, Any> = emptyMap(),

    @get:PropertyName("rating") @set:PropertyName("rating")
    var rating: Double = 0.0,

    @get:PropertyName("reviewCount") @set:PropertyName("reviewCount")
    var reviewCount: Int = 0,

    @get:PropertyName("isFeatured") @set:PropertyName("isFeatured")
    var isFeatured: Boolean = false,

    @get:PropertyName("isTrending") @set:PropertyName("isTrending")
    var isTrending: Boolean = false,

    @get:PropertyName("isHistorical") @set:PropertyName("isHistorical")
    var isHistorical: Boolean = false,

    @get:PropertyName("tags") @set:PropertyName("tags")
    var tags: List<String> = emptyList(),

    @get:PropertyName("tagsArabic") @set:PropertyName("tagsArabic")
    var tagsArabic: List<String> = emptyList(),

    @get:PropertyName("status") @set:PropertyName("status")
    var status: String = "",

    @get:PropertyName("statusArabic") @set:PropertyName("statusArabic")
    var statusArabic: String = "",

    @get:PropertyName("visitingHours") @set:PropertyName("visitingHours")
    var visitingHours: String = "",

    @get:PropertyName("entryFee") @set:PropertyName("entryFee")
    var entryFee: String = "",

    @get:PropertyName("bestTimeToVisit") @set:PropertyName("bestTimeToVisit")
    var bestTimeToVisit: String = "",

    @get:PropertyName("createdAt") @set:PropertyName("createdAt")
    var createdAt: Long = 0L,

    @get:PropertyName("updatedAt") @set:PropertyName("updatedAt")
    var updatedAt: Long = 0L
) {
    // No-argument constructor required by Firebase
    constructor() : this("")
}

@IgnoreExtraProperties
data class Location(
    @get:PropertyName("city") @set:PropertyName("city")
    var city: String = "",

    @get:PropertyName("cityArabic") @set:PropertyName("cityArabic")
    var cityArabic: String = "",

    @get:PropertyName("governorate") @set:PropertyName("governorate")
    var governorate: String = "",

    @get:PropertyName("governorateArabic") @set:PropertyName("governorateArabic")
    var governorateArabic: String = "",

    @get:PropertyName("region") @set:PropertyName("region")
    var region: String = "",

    @get:PropertyName("latitude") @set:PropertyName("latitude")
    var latitude: Double = 0.0,

    @get:PropertyName("longitude") @set:PropertyName("longitude")
    var longitude: Double = 0.0
) {
    constructor() : this("")
}