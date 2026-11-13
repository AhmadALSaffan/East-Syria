package com.example.eastsyria.Itinerary

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale
import kotlin.math.abs

/**
 * Runs the labeled evaluation set (src/test/resources/itinerary/eval-set.json) against the
 * catalog snapshot and writes docs/rag/eval-report.md. Fails if any check fails.
 *
 *   ./gradlew :app:testDebugUnitTest --tests "com.example.eastsyria.Itinerary.*"
 */
class ItineraryEvalTest {

    private val catalog = ItineraryFixtures.catalog
    private fun pct(a: Int, b: Int) = if (b == 0) "–" else String.format(Locale.ROOT, "%d/%d (%.0f%%)", a, b, 100.0 * a / b)

    @Test
    fun evaluationSet() {
        val cases = ItineraryFixtures.evalSet.getJSONArray("cases")
        val failures = mutableListOf<String>()
        val rows = StringBuilder()

        // Relevance (retrieval alone, before geography): label 2 = relevant, 1 = neutral, 0 = not.
        var retrievedRelevant = 0; var retrievedJudged = 0; var relevantTotal = 0
        // Stops: relevant (label >= 1) among stops of labeled cases.
        var stopsRelevant = 0; var stopsJudged = 0
        // Geography.
        var geoChecks = 0; var geoPassed = 0
        var distanceChecks = 0; var distancePassed = 0
        // Order and intent.
        var orderCases = 0; var orderPassed = 0
        var intentCases = 0; var intentPassed = 0
        // Explanations.
        var explanations = 0; var supported = 0

        for (i in 0 until cases.length()) {
            val c = cases.getJSONObject(i)
            val id = c.getString("id")
            val query = c.getString("query")
            val outcome = ItineraryPlanner.plan(query, catalog)
            val expect = c.getString("expect")
            val notes = mutableListOf<String>()

            intentCases++
            val kindOk = when (expect) {
                "plan" -> outcome is ItineraryOutcome.Plan
                "ambiguous" -> outcome is ItineraryOutcome.Ambiguous
                else -> outcome is ItineraryOutcome.NotItinerary
            } && outcome.query.language == c.getString("language")
            var intentOk = kindOk
            if (kindOk && outcome is ItineraryOutcome.Ambiguous) {
                if (outcome.reason.name != c.getString("reason")) intentOk = false
                c.optJSONArray("options")?.let { o ->
                    val want = (0 until o.length()).map { o.getString(it) }
                    if (outcome.options.map { it.key } != want) intentOk = false
                }
                notes += "asks: ${outcome.options.joinToString { it.nameEn }}"
            }
            if (intentOk) intentPassed++ else failures += "$id: expected $expect, got ${outcome::class.simpleName}"

            var stopsText = "–"
            if (outcome is ItineraryOutcome.Plan && expect == "plan") {
                val labels = c.optJSONObject("relevance") ?: JSONObject()
                val label = { lid: String -> if (labels.has(lid)) labels.getInt(lid) else null }

                if (labels.length() > 0) {
                    val retrieved = ItineraryPlanner.retrieve(query, catalog).map { it.first.id }
                    val judged = retrieved.filter { label(it) != 1 }
                    retrievedJudged += judged.size
                    retrievedRelevant += judged.count { label(it) == 2 }
                    relevantTotal += labels.keys().asSequence().count { labels.getInt(it) == 2 }
                    val l2 = labels.keys().asSequence().filter { labels.getInt(it) == 2 }.toSet()
                    val missed = l2 - retrieved.toSet()
                    if (missed.isNotEmpty()) notes += "not retrieved: ${missed.sorted().joinToString()}"
                    plan(outcome).forEach { s -> stopsJudged++; if ((label(s) ?: 0) >= 1) stopsRelevant++ }
                }

                // Anchor and radius.
                if (outcome.anchor?.key != c.getString("anchor")) failures += "$id: anchor ${outcome.anchor?.key}"
                if (outcome.radiusKm != c.getDouble("radiusKm")) failures += "$id: radius ${outcome.radiusKm}"
                if (c.optBoolean("capped") != outcome.capped) failures += "$id: capped ${outcome.capped}"

                // Geography: independent distances, inside the radius, exclusions.
                val dist = c.optJSONObject("expectedDistancesKm") ?: JSONObject()
                val shown = outcome.stops.associate { it.landmark.id to it.fromAnchorKm!! } +
                    outcome.outsideRadius.associate { it.landmark.id to it.distanceKm }
                for (lid in dist.keys()) {
                    val app = shown[lid] ?: continue
                    distanceChecks++
                    if (abs(app - dist.getDouble(lid)) <= 0.2) distancePassed++ else failures += "$id: distance $lid app ${ItineraryFixtures.km(app)} vs ${dist.getDouble(lid)}"
                }
                for (s in outcome.stops) {
                    geoChecks++
                    val labeled = if (dist.has(s.landmark.id)) dist.getDouble(s.landmark.id) else s.fromAnchorKm!!
                    if (labeled <= outcome.radiusKm!!) geoPassed++ else failures += "$id: stop ${s.landmark.id} beyond radius"
                }
                val outsideWant = c.optJSONArray("expectedOutsideRadius")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList()
                val mustNot = c.optJSONArray("mustNotInclude")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList()
                for (lid in outsideWant + mustNot) {
                    geoChecks++
                    val excluded = outcome.stops.none { it.landmark.id == lid } &&
                        (lid !in outsideWant || outcome.outsideRadius.any { it.landmark.id == lid })
                    if (excluded) geoPassed++ else failures += "$id: $lid should be excluded by distance"
                }
                outcome.outsideRadius.forEach { notes += "beyond ${outcome.radiusKm!!.toInt()} km: ${it.landmark.name} (${ItineraryFixtures.km(it.distanceKm)})" }

                // Order.
                val wantOrder = c.getJSONArray("expectedStopsInOrder").let { a -> (0 until a.length()).map { a.getString(it) } }
                orderCases++
                if (plan(outcome) == wantOrder) orderPassed++ else failures += "$id: order ${plan(outcome)} vs $wantOrder"

                // Explanations, each checked against its own record.
                ItineraryCheck.verify(outcome, catalog).forEach { r ->
                    explanations++
                    if (r.supported) supported++ else failures += "$id: explanation for ${r.landmarkId}: ${r.problems}"
                }
                stopsText = outcome.stops.joinToString(" → ") { "${it.landmark.name} (${ItineraryFixtures.km(it.fromAnchorKm)})" }
                    .ifEmpty { "none" }
            }
            rows.append("| `$id` | ${query.replace("|", "\\|")} | ${outcome::class.simpleName} | ${if (intentOk) "✓" else "✗"} | $stopsText | ${notes.joinToString("; ")} |\n")
        }

        // The word-for-word support check against hand-labeled claims.
        val claims = ItineraryFixtures.evalSet.getJSONArray("claims")
        var tp = 0; var tn = 0; var fp = 0; var fn = 0
        val claimRows = StringBuilder()
        for (i in 0 until claims.length()) {
            val c = claims.getJSONObject(i)
            val l = catalog.first { it.id == c.getString("landmark") }
            val got = ItineraryCheck.isSupported(l, c.getString("field"), c.getString("text"))
            val want = c.getBoolean("supported")
            when {
                got && want -> tp++
                !got && !want -> tn++
                got && !want -> fp++
                else -> fn++
            }
            claimRows.append("| ${l.name} · `${c.getString("field")}` | ${c.getString("text")} | ${if (want) "supported" else "not supported"} | ${if (got) "supported" else "not supported"} | ${c.optString("note")} |\n")
        }

        val report = buildString {
            append("# Itinerary planner: evaluation\n\n")
            append("Generated by `ItineraryEvalTest` from `app/src/test/resources/itinerary/eval-set.json` against the catalog snapshot `${ItineraryFixtures.SNAPSHOT}` ")
            append("(${catalog.size} records, ${catalog.count { it.status == "published" }} published). Re-run with:\n\n")
            append("```bash\n./gradlew :app:testDebugUnitTest --tests \"com.example.eastsyria.Itinerary.*\"\n```\n\n")
            append("Default radius for \"near\": **${ItineraryPlanner.DEFAULT_NEAR_RADIUS_KM.toInt()} km**, straight line. Minimum coverage: ${ItineraryPlanner.MIN_COVERAGE}.\n\n")
            append("## Summary\n\n| Check | Result |\n|---|---|\n")
            append("| Retrieval precision (relevant among retrieved, neutral labels ignored) | ${pct(retrievedRelevant, retrievedJudged)} |\n")
            append("| Retrieval recall (label-2 places retrieved) | ${pct(retrievedRelevant, relevantTotal)} |\n")
            append("| Suggested stops that are relevant (label ≥ 1) | ${pct(stopsRelevant, stopsJudged)} |\n")
            append("| Geographic filter (stops inside the radius, far places excluded) | ${pct(geoPassed, geoChecks)} |\n")
            append("| Distances within 0.2 km of an independent calculation | ${pct(distancePassed, distanceChecks)} |\n")
            append("| Stop order equals the shortest straight-line path | ${pct(orderPassed, orderCases)} |\n")
            append("| Intent, language and ambiguity handled as labeled | ${pct(intentPassed, intentCases)} |\n")
            append("| Generated explanations supported by their record | ${pct(supported, explanations)} |\n")
            append("| Support check vs labeled claims (TP / TN / FP / FN) | $tp / $tn / $fp / $fn |\n\n")
            append("## Cases\n\n| Case | Query | Outcome | Labeled outcome | Stops (from start) | Notes |\n|---|---|---|---|---|---|\n")
            append(rows)
            append("\n## Support check on labeled claims\n\n")
            append("The planner only quotes records, so its check is word for word. A faithful paraphrase is rejected (the one false negative below); a changed word or a fact from another place is rejected too.\n\n")
            append("| Record · field | Claim | Label | Check | Note |\n|---|---|---|---|---|\n")
            append(claimRows)
            if (failures.isNotEmpty()) append("\n## Failures\n\n").append(failures.joinToString("\n") { "- $it" }).append("\n")
        }
        ItineraryFixtures.reportFile("eval-report.md").writeText(report)
        println(report)

        assertTrue("Evaluation failures:\n" + failures.joinToString("\n"), failures.isEmpty())
        assertEquals("support check: no unsupported claim may pass", 0, fp)
    }

    private fun plan(p: ItineraryOutcome.Plan) = p.stops.map { it.landmark.id }
}
