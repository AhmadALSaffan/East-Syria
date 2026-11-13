package com.example.eastsyria.Itinerary

import com.example.eastsyria.MainPage.Data.Landmark
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale

/** Loads the catalog snapshot and the evaluation set, and writes the reports. */
object ItineraryFixtures {

    const val SNAPSHOT = "catalog-2026-11-13.json"

    fun resource(name: String): String =
        ItineraryFixtures::class.java.classLoader!!.getResourceAsStream("itinerary/$name")!!
            .bufferedReader(Charsets.UTF_8).use { it.readText() }

    /** The app's public `landmarks` node as downloaded; Firebase serves it as an array with a null at 0. */
    val catalog: List<Landmark> by lazy {
        val raw = resource(SNAPSHOT).trim()
        val items: List<Pair<String, JSONObject>> = if (raw.startsWith("[")) {
            val a = JSONArray(raw)
            (0 until a.length()).mapNotNull { i -> a.optJSONObject(i)?.let { i.toString() to it } }
        } else {
            val o = JSONObject(raw)
            o.keys().asSequence().map { it to o.getJSONObject(it) }.toList()
        }
        items.map { (key, o) -> landmark(key, o) }
    }

    fun landmark(key: String, o: JSONObject): Landmark {
        fun list(name: String) = o.optJSONArray(name)?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList()
        val loc = o.optJSONObject("location") ?: JSONObject()
        return Landmark(
            id = key,
            name = o.optString("name"), nameArabic = o.optString("nameArabic"),
            category = o.optString("category"), categoryArabic = o.optString("categoryArabic"),
            description = o.optString("description"), descriptionArabic = o.optString("descriptionArabic"),
            longDescription = o.optString("longDescription"), longDescriptionArabic = o.optString("longDescriptionArabic"),
            imageUrl = o.optString("imageUrl"),
            location = Landmark.Location(
                city = loc.optString("city"), cityArabic = loc.optString("cityArabic"),
                governorate = loc.optString("governorate"), governorateArabic = loc.optString("governorateArabic"),
                region = loc.optString("region"),
                latitude = loc.optDouble("latitude", 0.0), longitude = loc.optDouble("longitude", 0.0),
            ),
            tags = list("tags"), tagsArabic = list("tagsArabic"), sources = list("sources"),
            status = o.optString("status"), statusArabic = o.optString("statusArabic"),
        )
    }

    val evalSet: JSONObject by lazy { JSONObject(resource("eval-set.json")) }

    /** Reports go to docs/rag in the repository (tests run from the app module). */
    fun reportFile(name: String): File {
        val docs = File(System.getProperty("user.dir")).resolve("../docs").canonicalFile
        val dir = if (docs.isDirectory) docs.resolve("rag") else File("build/reports/itinerary")
        dir.mkdirs()
        return dir.resolve(name)
    }

    fun km(d: Double?) = if (d == null) "–" else String.format(Locale.ROOT, "%.1f km", d)
}
