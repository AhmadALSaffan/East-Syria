package com.example.eastsyria.Itinerary

import com.example.eastsyria.MainPage.Data.Landmark
import kotlin.math.abs

/**
 * Checks that a plan says nothing its records don't: every stop is a published catalog landmark,
 * every quote is copied word for word from the field it names, every tag and link is the record's
 * own, and every distance and the radius hold when recomputed from the coordinates.
 *
 * The app runs it before showing a plan and drops any stop that fails; the evaluation reports it.
 */
object ItineraryCheck {

    data class Report(val landmarkId: String, val problems: List<String>) {
        val supported get() = problems.isEmpty()
    }

    fun fieldText(l: Landmark, field: String): String? = when (field) {
        "name" -> l.name
        "nameArabic" -> l.nameArabic
        "description" -> l.description
        "descriptionArabic" -> l.descriptionArabic
        "longDescription" -> l.longDescription
        "longDescriptionArabic" -> l.longDescriptionArabic
        else -> null
    }

    /** Is [text] a word-for-word part of the record's [field]? Paraphrases don't count. */
    fun isSupported(l: Landmark, field: String, text: String): Boolean {
        val source = fieldText(l, field) ?: return false
        val claim = ItineraryText.squash(text)
        return claim.isNotEmpty() && ItineraryText.squash(source).contains(claim)
    }

    fun verify(plan: ItineraryOutcome.Plan, catalog: List<Landmark>): List<Report> = plan.stops.map { stop ->
        val problems = mutableListOf<String>()
        val record = catalog.firstOrNull { it.id == stop.landmark.id }
        if (record == null) {
            problems += "not in the catalog"
        } else {
            if (record.status != "published") problems += "not published"
            if (record.name != stop.landmark.name || record.nameArabic != stop.landmark.nameArabic) problems += "name differs from the record"
            val e = stop.explanation
            val q = e.quote
            if (q == null) problems += "no quote"
            else if (!isSupported(record, q.field, q.text)) problems += "quote not found in ${q.field}"
            val tags = when (e.tagField) {
                "tags" -> record.tags
                "tagsArabic" -> record.tagsArabic
                null -> emptyList()
                else -> { problems += "unknown tag field"; emptyList() }
            }
            e.matchedTags.filterNot { it in tags }.forEach { problems += "tag \"$it\" not in ${e.tagField}" }
            e.sourceLinks.filterNot { it in record.sources }.forEach { problems += "link $it not in sources" }

            val lat = record.location.latitude
            val lng = record.location.longitude
            if (!ItineraryGeo.isUsable(lat, lng)) problems += "no usable coordinates"
            val a = plan.anchor
            if (a != null) {
                val d = ItineraryGeo.distanceKm(a.lat, a.lng, lat, lng)
                if (stop.fromAnchorKm == null || abs(stop.fromAnchorKm - d) > 0.01) problems += "distance from start differs"
                if (plan.radiusKm != null && d > plan.radiusKm + 1e-9) problems += "outside the radius"
            }
        }
        Report(stop.landmark.id, problems)
    }
}
