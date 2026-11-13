package com.example.eastsyria.Itinerary

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Straight-line geography: distances, usable coordinates and the order of a few stops. */
object ItineraryGeo {

    private const val EARTH_RADIUS_KM = 6371.0088

    /**
     * A box around Syria with a small margin. Coordinates outside it, (0, 0) or NaN are treated as
     * missing: the catalog stores 0.0 when a curator left the location empty.
     */
    private const val MIN_LAT = 32.0
    private const val MAX_LAT = 37.6
    private const val MIN_LNG = 35.4
    private const val MAX_LNG = 42.6

    fun isUsable(lat: Double, lng: Double): Boolean =
        !lat.isNaN() && !lng.isNaN() && !(lat == 0.0 && lng == 0.0) &&
            lat in MIN_LAT..MAX_LAT && lng in MIN_LNG..MAX_LNG

    /** Great-circle distance in kilometres (haversine). */
    fun distanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val p1 = Math.toRadians(lat1)
        val p2 = Math.toRadians(lat2)
        val dp = p2 - p1
        val dl = Math.toRadians(lng2 - lng1)
        val h = sin(dp / 2) * sin(dp / 2) + cos(p1) * cos(p2) * sin(dl / 2) * sin(dl / 2)
        return 2 * EARTH_RADIUS_KM * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }

    /**
     * The visiting order with the shortest straight-line path. From a starting point the path
     * begins there; without one, it may begin at any stop. With at most three stops every order
     * is tried (six at most), so the answer is exact and the same every time; ties keep the
     * order of the ids.
     */
    fun <T> bestOrder(start: Pair<Double, Double>?, stops: List<T>, id: (T) -> String, point: (T) -> Pair<Double, Double>): List<T> {
        if (stops.size <= 1) return stops
        val sorted = stops.sortedBy(id)
        var best: List<T> = sorted
        var bestLength = Double.MAX_VALUE
        for (order in permutations(sorted)) {
            var length = 0.0
            var prev = start
            for (s in order) {
                val p = point(s)
                if (prev != null) length += distanceKm(prev.first, prev.second, p.first, p.second)
                prev = p
            }
            if (length < bestLength - 1e-9) {
                bestLength = length
                best = order
            }
        }
        return best
    }

    private fun <T> permutations(items: List<T>): List<List<T>> =
        if (items.size <= 1) listOf(items)
        else items.indices.flatMap { i ->
            permutations(items.filterIndexed { j, _ -> j != i }).map { listOf(items[i]) + it }
        }
}
