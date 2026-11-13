package com.example.eastsyria.Itinerary

import com.example.eastsyria.MainPage.Data.Landmark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ItineraryUnitTest {

    private val catalog = ItineraryFixtures.catalog

    @Test
    fun arabicIsNormalisedAndMatchedAcrossPrefixes() {
        assertEquals("بدي 3 اماكن قريبه", ItineraryText.normalize("بدي ٣ أماكن قريبة"))
        assertTrue(ItineraryText.variants("بتاريخ").any { it in ItineraryText.variants("تاريخي") })
        assertTrue(ItineraryText.variants("الفرات").any { it in ItineraryText.variants("فرات") })
        assertTrue(ItineraryText.variants("castles").contains("castle"))
    }

    @Test
    fun haversineMatchesAKnownDistance() {
        // Deir ez-Zor to Raqqa town centres; 123.75 km from an independent Python haversine.
        val d = ItineraryGeo.distanceKm(35.33333, 40.15, 35.95, 39.01)
        assertEquals(123.75, d, 0.01)
    }

    @Test
    fun missingOrForeignCoordinatesAreNotUsable() {
        assertFalse(ItineraryGeo.isUsable(0.0, 0.0))
        assertFalse(ItineraryGeo.isUsable(Double.NaN, 40.0))
        assertFalse(ItineraryGeo.isUsable(48.85, 2.35)) // Paris
        assertTrue(ItineraryGeo.isUsable(35.345, 40.1511))
    }

    @Test
    fun everyTownIsInSyriaAndAgreesWithTheCatalog() {
        ItineraryLexicon.towns.forEach { assertTrue(it.key, ItineraryGeo.isUsable(it.lat, it.lng)) }
        // A town centre and a catalog record in that town should be close.
        val deir = ItineraryLexicon.towns.first { it.key == "deir-ez-zor" }
        val bridge = catalog.first { it.id == "1" }.location
        assertTrue(ItineraryGeo.distanceKm(deir.lat, deir.lng, bridge.latitude, bridge.longitude) < 3)
        val mayadin = ItineraryLexicon.towns.first { it.key == "mayadin" }
        val rahba = catalog.first { it.id == "6" }.location
        assertTrue(ItineraryGeo.distanceKm(mayadin.lat, mayadin.lng, rahba.latitude, rahba.longitude) < 5)
        val palmyra = ItineraryLexicon.towns.first { it.key == "palmyra" }
        val ruins = catalog.first { it.id == "3" }.location
        assertTrue(ItineraryGeo.distanceKm(palmyra.lat, palmyra.lng, ruins.latitude, ruins.longitude) < 3)
    }

    @Test
    fun orderIsTheShortestPathAndStable() {
        val stops = listOf("a" to (35.0 to 40.0), "b" to (35.0 to 41.0), "c" to (35.0 to 40.5))
        val order = ItineraryGeo.bestOrder(35.0 to 39.5, stops, { it.first }, { it.second }).map { it.first }
        assertEquals(listOf("a", "c", "b"), order)
    }

    @Test
    fun theStoryQueryIsReadCorrectly() {
        val q = ItineraryPlanner.parse("بدي ٣ أماكن قريبة من دير الزور مرتبطة بتاريخ الفرات", catalog)
        assertTrue(q.isItinerary)
        assertEquals("ar", q.language)
        assertEquals(3, q.requested)
        assertEquals(listOf("town:deir-ez-zor"), q.anchors.map { it.key })
        assertEquals(setOf("euphrates", "history"), q.conceptIds.toSet())
        assertTrue(q.freeTerms.isEmpty())
    }

    @Test
    fun aPlaceWithoutCoordinatesIsReportedNotPlaced() {
        val mill = Landmark(
            id = "test-mill", name = "Test Euphrates Water Mill", nameArabic = "طاحونة الفرات التجريبية",
            category = "Historical", description = "A historic water mill on the Euphrates (synthetic test record).",
            descriptionArabic = "طاحونة مياه تاريخية على نهر الفرات (سجل اختبار).",
            tags = listOf("euphrates", "historical"), tagsArabic = listOf("الفرات", "تاريخي"),
            location = Landmark.Location(latitude = 0.0, longitude = 0.0), status = "published",
        )
        val plan = ItineraryPlanner.plan("3 places near Deir ez-Zor about Euphrates history", catalog + mill) as ItineraryOutcome.Plan
        assertTrue(plan.stops.none { it.landmark.id == "test-mill" })
        assertEquals(listOf("test-mill"), plan.withoutLocation.map { it.id })
        assertEquals(listOf("1", "6", "2"), plan.stops.map { it.landmark.id })
    }

    @Test
    fun pickingAStartingPointResolvesAnAmbiguousRequest() {
        val text = "3 places near the castle"
        val first = ItineraryPlanner.plan(text, catalog) as ItineraryOutcome.Ambiguous
        val jabar = first.options.first { it.key == "landmark:4" }
        val plan = ItineraryPlanner.plan(text, catalog, anchorOverride = jabar) as ItineraryOutcome.Plan
        assertTrue(plan.stops.none { it.landmark.id == "4" })
        assertTrue(plan.stops.all { (it.fromAnchorKm ?: 999.0) <= 100.0 })
    }

    @Test
    fun aWiderRadiusBringsBackTheExcludedPlace() {
        val text = "٣ مواقع رومانية قرب دير الزور"
        val near = ItineraryPlanner.plan(text, catalog) as ItineraryOutcome.Plan
        assertEquals(listOf("2"), near.stops.map { it.landmark.id })
        val wide = ItineraryPlanner.plan(text, catalog, radiusOverride = 200.0) as ItineraryOutcome.Plan
        assertTrue(wide.stops.any { it.landmark.id == "3" }) // Palmyra, 191.7 km
        assertFalse(wide.radiusIsDefault)
    }

    @Test
    fun aPendingLandmarkIsNeverSuggested() {
        val plan = ItineraryPlanner.plan("3 Byzantine places near Raqqa", catalog) as ItineraryOutcome.Plan
        assertTrue(plan.stops.none { it.landmark.id == "8" }) // Rasafeh is "pending"
    }

    @Test
    fun aRequestForAKindOfPlaceTheCatalogLacksSaysSo() {
        val plan = ItineraryPlanner.plan("3 museums near Deir ez-Zor", catalog) as ItineraryOutcome.Plan
        assertTrue(plan.stops.isEmpty())
        assertEquals(listOf("museum"), plan.unknownConcepts)
    }
}
