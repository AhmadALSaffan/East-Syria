package com.example.eastsyria.Itinerary

import com.example.eastsyria.MainPage.Data.Landmark

/** Where an itinerary starts: a town from the gazetteer or a landmark from the catalog. */
data class Anchor(
    val key: String,
    val nameEn: String,
    val nameAr: String,
    val lat: Double,
    val lng: Double,
    /** Where the coordinates come from (the town list, or the landmark's own record). */
    val source: String,
    /** Landmarks that are the starting point itself, so not suggested as a stop. */
    val excludeIds: Set<String> = emptySet(),
) {
    fun name(language: String) = if (language == "ar") nameAr.ifBlank { nameEn } else nameEn.ifBlank { nameAr }
}

/** What the request asked for, read without any model. */
data class ParsedQuery(
    val text: String,
    /** "ar" when the question is written in Arabic, else "en". The answer uses this language. */
    val language: String,
    val isItinerary: Boolean,
    val requested: Int,
    val countGiven: Boolean,
    val mentionsPlaces: Boolean,
    val wantsNear: Boolean,
    val nearMe: Boolean,
    val anchors: List<Anchor>,
    /** Several landmarks fit a generic starting point ("near the castle"). */
    val genericAnchorCandidates: List<Anchor>,
    val radiusKm: Double?,
    val conceptIds: List<String>,
    /** Words outside the lexicon, matched literally (e.g. "Zenobia"). */
    val freeTerms: List<String>,
)

/** One concept or free word of the query, with how distinctive it is in the catalog. */
data class QueryTerm(val id: String, val phrases: List<List<Set<String>>>, val idf: Double, val fromLexicon: Boolean)

/** A record field quoted word for word. */
data class Quote(
    /** The [Landmark] property it was taken from, e.g. "descriptionArabic". */
    val field: String,
    val text: String,
    /** True when the record has no text in the question's language and the other one is used. */
    val otherLanguage: Boolean,
)

/**
 * Why a stop was chosen, made only of things that can be checked against the record: a sentence
 * copied from one of its description fields, its own tags, and distances computed from its
 * coordinates. Connecting words come from the app's strings and carry no facts.
 */
data class Explanation(
    val quote: Quote?,
    val tagField: String?,
    val matchedTags: List<String>,
    val sourceLinks: List<String>,
)

data class Stop(
    val number: Int,
    val landmark: Landmark,
    val score: Double,
    val coverage: Double,
    /** Straight-line distance from the starting point, when there is one. */
    val fromAnchorKm: Double?,
    /** Straight-line distance from the previous stop (or the starting point for stop 1). */
    val legKm: Double?,
    val explanation: Explanation,
)

data class Excluded(val landmark: Landmark, val distanceKm: Double)

sealed interface ItineraryOutcome {
    val query: ParsedQuery

    /** Not a request for stops; the guide's model answers it. */
    data class NotItinerary(override val query: ParsedQuery) : ItineraryOutcome

    /** The starting point is unclear; the user picks one. */
    data class Ambiguous(override val query: ParsedQuery, val options: List<Anchor>, val reason: Reason) : ItineraryOutcome {
        enum class Reason { SEVERAL_PLACES, GENERIC_PLACE, NEAR_ME, NO_PLACE }
    }

    data class Plan(
        override val query: ParsedQuery,
        val anchor: Anchor?,
        val radiusKm: Double?,
        val radiusIsDefault: Boolean,
        val requested: Int,
        val capped: Boolean,
        val stops: List<Stop>,
        /** Relevant places left out only because they are beyond the radius. */
        val outsideRadius: List<Excluded>,
        /** Relevant places that have no usable coordinates, so can't be placed or ordered. */
        val withoutLocation: List<Landmark>,
        /** Lexicon concepts the catalog has no place for (e.g. "museum"). */
        val unknownConcepts: List<String>,
    ) : ItineraryOutcome
}
