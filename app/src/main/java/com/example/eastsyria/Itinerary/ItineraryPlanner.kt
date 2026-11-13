package com.example.eastsyria.Itinerary

import com.example.eastsyria.MainPage.Data.Landmark
import kotlin.math.ln

/**
 * Plans up to three stops from the landmark catalog for a request such as
 * "بدي ٣ أماكن قريبة من دير الزور مرتبطة بتاريخ الفرات".
 *
 * Retrieval-augmented and deterministic, with no model and no network:
 *  1. read the request: how many places, near where, within what radius, about what;
 *  2. retrieve published landmarks whose names, tags and descriptions (Arabic and English) share
 *     the request's concepts, weighted by how distinctive each concept is in the catalog;
 *  3. keep those whose verified coordinates lie within the radius (haversine);
 *  4. order them by the shortest straight-line path from the starting point;
 *  5. explain each stop only with text copied from its own record ([Explanation]).
 *
 * Pure Kotlin, so the same code runs in the app and in the unit tests, demo and evaluation.
 */
object ItineraryPlanner {

    /** What "near" means when no radius is given: 100 km in a straight line. Shown in the UI. */
    const val DEFAULT_NEAR_RADIUS_KM = 100.0
    const val MAX_STOPS = 3
    const val DEFAULT_STOPS = 3

    /**
     * A place must cover at least half of the request's meaning, where each concept counts by how
     * rare it is in the catalog: "history" alone (most records) can't carry a request about "the
     * history of the Euphrates", but "Euphrates" can.
     */
    const val MIN_COVERAGE = 0.5

    private const val MAX_RADIUS_KM = 500.0

    // ------------------------------------------------------------------ reading the request

    fun parse(text: String, catalog: List<Landmark>): ParsedQuery {
        val language = if (ItineraryText.hasArabic(text)) "ar" else "en"
        val words = ItineraryText.tokens(text)
        val forms = words.map { ItineraryText.variants(it) }
        val used = BooleanArray(words.size)
        fun take(start: Int, length: Int) { for (i in start until start + length) used[i] = true }
        fun free(start: Int, length: Int) = (start until start + length).none { used[it] }
        fun occurrences(phrase: List<Set<String>>): List<Int> {
            val out = mutableListOf<Int>()
            var from = 0
            while (true) {
                val at = ItineraryText.find(forms, phrase, from)
                if (at < 0) break
                if (free(at, phrase.size)) out += at
                from = at + 1
            }
            return out
        }

        // Radius: "within 50 km", "ضمن ٥٠ كم", "50km".
        var radius: Double? = null
        for (i in words.indices) {
            val w = words[i]
            val joined = Regex("^(\\d{1,3})(km|كم)$").find(w)
            if (joined != null) {
                radius = joined.groupValues[1].toDouble(); take(i, 1); break
            }
            if (w.all { it.isDigit() } && i + 1 < words.size && forms[i + 1].any { it in ItineraryLexicon.unitWords }) {
                radius = w.toDouble(); take(i, 2); break
            }
        }
        radius = radius?.coerceIn(1.0, MAX_RADIUS_KM)

        // How many places.
        var count: Int? = null
        for (i in words.indices) {
            if (used[i]) continue
            val w = words[i]
            val n = if (w.all { it.isDigit() } && w.length <= 2) w.toInt()
            else forms[i].firstNotNullOfOrNull { ItineraryLexicon.numberWords[it] }
            if (n != null && n in 1..20) {
                count = n; take(i, 1); break
            }
        }

        val mentionsPlaces = forms.any { f -> f.any { it in ItineraryLexicon.placeWords } }

        // "Near me" first, since it also contains "near".
        var nearMe = false
        for (p in ItineraryLexicon.nearMeWords) occurrences(p).firstOrNull()?.let { nearMe = true; take(it, p.size) }
        var nearEnd = -1
        for (p in ItineraryLexicon.nearWords) {
            for (at in occurrences(p)) {
                take(at, p.size)
                if (nearEnd < 0 || at + p.size < nearEnd) nearEnd = at + p.size
            }
        }
        val wantsNear = nearMe || nearEnd >= 0

        // Starting points: towns, then landmark names. The longest name wins an overlap.
        data class Hit(val start: Int, val length: Int, val town: ItineraryLexicon.Town?, val landmark: Landmark?)
        val hits = mutableListOf<Hit>()
        for (town in ItineraryLexicon.towns) for (p in town.phrases) occurrences(p).forEach { hits += Hit(it, p.size, town, null) }
        for (l in catalog) {
            for (name in listOf(l.name, l.nameArabic)) {
                val p = ItineraryText.index(name)
                if (p.isNotEmpty()) occurrences(p).forEach { hits += Hit(it, p.size, null, l) }
            }
        }
        val kept = mutableListOf<Hit>()
        for (h in hits.sortedWith(compareByDescending<Hit> { it.length }.thenBy { it.start })) {
            val overlaps = kept.any { k -> h.start < k.start + k.length && k.start < h.start + h.length && !(k.start == h.start && k.length == h.length) }
            if (!overlaps) kept += h
        }
        val spans = kept.groupBy { it.start to it.length }.toSortedMap(compareBy({ it.first }, { it.second }))
        val anchorSpans = mutableListOf<Pair<Int, Anchor>>()
        for ((span, group) in spans) {
            val town = group.firstNotNullOfOrNull { it.town }
            val same = group.mapNotNull { it.landmark }.filter { it.status == "published" }
            val anchor = when {
                town != null -> Anchor(
                    key = "town:${town.key}", nameEn = town.nameEn, nameAr = town.nameAr, lat = town.lat, lng = town.lng,
                    source = ItineraryLexicon.TOWN_SOURCE, excludeIds = same.map { it.id }.toSet(),
                )
                same.isNotEmpty() && ItineraryGeo.isUsable(same[0].location.latitude, same[0].location.longitude) -> anchorFor(same[0])
                else -> null
            }
            take(span.first, span.second)
            if (anchor != null && anchorSpans.none { it.second.key == anchor.key }) anchorSpans += span.first to anchor
        }
        // Several places named: the one right after "near" is the start.
        var anchors = anchorSpans.map { it.second }
        if (anchors.size > 1 && nearEnd >= 0) {
            val next = (nearEnd until words.size).firstOrNull { i ->
                !forms[i].any { it in ItineraryLexicon.stopWords } || anchorSpans.any { it.first == i }
            }
            anchorSpans.firstOrNull { it.first == next }?.let { anchors = listOf(it.second) }
        }

        // Topic concepts.
        val conceptIds = mutableListOf<String>()
        var genericCandidates = emptyList<Anchor>()
        for (concept in ItineraryLexicon.concepts) {
            for (p in concept.phrases) {
                for (at in occurrences(p)) {
                    take(at, p.size)
                    // "near the castle": a kind of place used as the starting point.
                    val afterNear = nearEnd >= 0 && at >= nearEnd && anchors.isEmpty() && !nearMe &&
                        (nearEnd until at).all { i -> forms[i].any { it in ItineraryLexicon.stopWords } }
                    if (afterNear) {
                        genericCandidates = catalog.filter { l ->
                            l.status == "published" && ItineraryGeo.isUsable(l.location.latitude, l.location.longitude) &&
                                listOf(l.name, l.nameArabic).any { n ->
                                    val idx = ItineraryText.index(n)
                                    concept.phrases.any { ItineraryText.find(idx, it) >= 0 }
                                }
                        }.sortedBy { it.id }.map { anchorFor(it) }
                    } else if (concept.id !in conceptIds) {
                        conceptIds += concept.id
                    }
                }
            }
        }
        if (genericCandidates.size == 1) {
            anchors = genericCandidates
            genericCandidates = emptyList()
        }

        val freeTerms = words.indices.filter { i ->
            !used[i] && words[i].length >= 3 && !words[i].all { it.isDigit() } &&
                forms[i].none { it in ItineraryLexicon.stopWords }
        }.map { words[it] }.distinct()

        val hasTopic = conceptIds.isNotEmpty() || freeTerms.isNotEmpty()
        val isItinerary = (wantsNear && (count != null || mentionsPlaces || hasTopic || anchors.isNotEmpty() || genericCandidates.isNotEmpty())) ||
            (anchors.isNotEmpty() && mentionsPlaces) ||
            (count != null && mentionsPlaces)

        return ParsedQuery(
            text = text, language = language, isItinerary = isItinerary,
            requested = count ?: DEFAULT_STOPS, countGiven = count != null, mentionsPlaces = mentionsPlaces,
            wantsNear = wantsNear, nearMe = nearMe, anchors = anchors, genericAnchorCandidates = genericCandidates,
            radiusKm = radius, conceptIds = conceptIds, freeTerms = freeTerms,
        )
    }

    fun anchorFor(l: Landmark) = Anchor(
        key = "landmark:${l.id}", nameEn = l.name, nameAr = l.nameArabic,
        lat = l.location.latitude, lng = l.location.longitude, source = "landmark record ${l.id}", excludeIds = setOf(l.id),
    )

    /** Starting points to offer when the request doesn't name one. */
    fun townAnchors(): List<Anchor> = ItineraryLexicon.towns.map { t ->
        Anchor("town:${t.key}", t.nameEn, t.nameAr, t.lat, t.lng, ItineraryLexicon.TOWN_SOURCE)
    }

    // ------------------------------------------------------------------ retrieval

    /** Searchable fields of a record and how much a match in each counts. */
    private class Field(val name: String, val weight: Double, val parts: List<List<Set<String>>>)

    private fun fieldsOf(l: Landmark): List<Field> = listOf(
        Field("name", 2.5, listOf(ItineraryText.index(l.name))),
        Field("nameArabic", 2.5, listOf(ItineraryText.index(l.nameArabic))),
        Field("tags", 3.0, l.tags.map { ItineraryText.index(it) }),
        Field("tagsArabic", 3.0, l.tagsArabic.map { ItineraryText.index(it) }),
        Field("category", 1.5, listOf(ItineraryText.index(l.category))),
        Field("categoryArabic", 1.5, listOf(ItineraryText.index(l.categoryArabic))),
        Field("description", 2.0, listOf(ItineraryText.index(l.description))),
        Field("descriptionArabic", 2.0, listOf(ItineraryText.index(l.descriptionArabic))),
        Field("longDescription", 1.0, listOf(ItineraryText.index(l.longDescription))),
        Field("longDescriptionArabic", 1.0, listOf(ItineraryText.index(l.longDescriptionArabic))),
    )

    private fun matches(text: List<Set<String>>, term: QueryTerm) = term.phrases.any { ItineraryText.find(text, it) >= 0 }

    /** Best field weight in which the term appears, 0 when it doesn't. */
    private fun weightOf(fields: List<Field>, term: QueryTerm): Double =
        fields.filter { f -> f.parts.any { matches(it, term) } }.maxOfOrNull { it.weight } ?: 0.0

    /** The request's terms with their catalog rarity; free words found nowhere are dropped. */
    fun terms(query: ParsedQuery, pool: List<Landmark>): Pair<List<QueryTerm>, List<String>> {
        val fields = pool.associate { it.id to fieldsOf(it) }
        val n = pool.size.coerceAtLeast(1)
        fun idf(df: Int) = ln((n + 1.0) / (df + 0.5))
        val out = mutableListOf<QueryTerm>()
        val unknown = mutableListOf<String>()
        for (id in query.conceptIds) {
            val concept = ItineraryLexicon.concepts.first { it.id == id }
            val probe = QueryTerm(id, concept.phrases, 0.0, true)
            val df = pool.count { weightOf(fields.getValue(it.id), probe) > 0 }
            if (df == 0) unknown += id
            out += probe.copy(idf = idf(df))
        }
        for (word in query.freeTerms) {
            val probe = QueryTerm("word:$word", listOf(listOf(ItineraryText.variants(word))), 0.0, false)
            val df = pool.count { weightOf(fields.getValue(it.id), probe) > 0 }
            if (df > 0) out += probe.copy(idf = idf(df))
        }
        return out to unknown
    }

    private class Scored(val landmark: Landmark, val score: Double, val coverage: Double, val distanceKm: Double?)

    /** Landmarks that cover enough of the request, with score and coverage. All of them when there's no topic. */
    private fun relevantOf(pool: List<Landmark>, terms: List<QueryTerm>): List<Triple<Landmark, Double, Double>> {
        val totalIdf = terms.sumOf { it.idf }
        return pool.mapNotNull { l ->
            val fields = fieldsOf(l)
            var score = 0.0
            var covered = 0.0
            for (t in terms) {
                val w = weightOf(fields, t)
                if (w > 0) { score += t.idf * w; covered += t.idf }
            }
            val coverage = if (totalIdf > 0) covered / totalIdf else 1.0
            if (terms.isNotEmpty() && coverage < MIN_COVERAGE) null else Triple(l, score, coverage)
        }
    }

    /** Retrieval alone, with no geography: the published landmarks relevant to [text], best first. */
    fun retrieve(text: String, catalog: List<Landmark>): List<Pair<Landmark, Double>> {
        val pool = catalog.filter { it.status == "published" && it.id.isNotBlank() }
        val (terms, _) = terms(parse(text, catalog), pool)
        return relevantOf(pool, terms).sortedWith(compareByDescending<Triple<Landmark, Double, Double>> { it.second }.thenBy { it.first.id })
            .map { it.first to it.second }
    }

    // ------------------------------------------------------------------ the plan

    /**
     * @param anchorOverride a starting point the user picked after an [ItineraryOutcome.Ambiguous].
     * @param radiusOverride a wider radius the user asked for after a short result.
     */
    fun plan(
        text: String,
        catalog: List<Landmark>,
        anchorOverride: Anchor? = null,
        radiusOverride: Double? = null,
    ): ItineraryOutcome {
        val query = parse(text, catalog)
        if (!query.isItinerary && anchorOverride == null) return ItineraryOutcome.NotItinerary(query)

        val pool = catalog.filter { it.status == "published" && it.id.isNotBlank() }
        val (terms, unknown) = terms(query, pool)
        // Only unknown filler words and no other sign of a stop request: let the guide answer it.
        if (terms.isEmpty() && unknown.isEmpty() && query.freeTerms.isNotEmpty() && !query.countGiven && !query.mentionsPlaces) {
            return ItineraryOutcome.NotItinerary(query)
        }

        val anchor: Anchor? = anchorOverride ?: when {
            query.anchors.size == 1 -> query.anchors[0]
            query.anchors.size > 1 -> return ItineraryOutcome.Ambiguous(query, query.anchors, ItineraryOutcome.Ambiguous.Reason.SEVERAL_PLACES)
            query.genericAnchorCandidates.size > 1 -> return ItineraryOutcome.Ambiguous(query, query.genericAnchorCandidates, ItineraryOutcome.Ambiguous.Reason.GENERIC_PLACE)
            query.nearMe -> return ItineraryOutcome.Ambiguous(query, townAnchors(), ItineraryOutcome.Ambiguous.Reason.NEAR_ME)
            query.wantsNear -> return ItineraryOutcome.Ambiguous(query, townAnchors(), ItineraryOutcome.Ambiguous.Reason.NO_PLACE)
            else -> null
        }
        val radius = if (anchor == null) null else radiusOverride ?: query.radiusKm ?: DEFAULT_NEAR_RADIUS_KM
        val radiusIsDefault = anchor != null && radiusOverride == null && query.radiusKm == null

        val relevant = relevantOf(pool.filter { anchor == null || it.id !in anchor.excludeIds }, terms).map { (l, score, coverage) ->
            val usable = ItineraryGeo.isUsable(l.location.latitude, l.location.longitude)
            val d = if (anchor != null && usable) ItineraryGeo.distanceKm(anchor.lat, anchor.lng, l.location.latitude, l.location.longitude) else null
            Scored(l, score, coverage, d)
        }
        val byRelevance = compareByDescending<Scored> { it.score }.thenBy { it.distanceKm ?: 0.0 }.thenBy { it.landmark.id }

        val located = relevant.filter { ItineraryGeo.isUsable(it.landmark.location.latitude, it.landmark.location.longitude) }
        val withoutLocation = relevant.filterNot { it in located }.sortedWith(byRelevance).take(MAX_STOPS).map { it.landmark }

        val wanted = query.requested.coerceAtMost(MAX_STOPS)
        val inside = if (radius == null) located else located.filter { it.distanceKm!! <= radius }
        // "Also relevant, but farther" only means something when the request has a topic.
        val outside = if (radius == null || terms.isEmpty()) emptyList() else located.filter { it.distanceKm!! > radius }
            .sortedWith(byRelevance).take(MAX_STOPS).sortedBy { it.distanceKm }.map { Excluded(it.landmark, it.distanceKm!!) }
        val chosen = inside.sortedWith(byRelevance).take(wanted)

        val ordered = ItineraryGeo.bestOrder(
            anchor?.let { it.lat to it.lng }, chosen,
            id = { it.landmark.id },
            point = { it.landmark.location.latitude to it.landmark.location.longitude },
        )
        var prev: Pair<Double, Double>? = anchor?.let { it.lat to it.lng }
        val stops = ordered.mapIndexed { i, s ->
            val here = s.landmark.location.latitude to s.landmark.location.longitude
            val leg = prev?.let { ItineraryGeo.distanceKm(it.first, it.second, here.first, here.second) }
            prev = here
            Stop(i + 1, s.landmark, s.score, s.coverage, s.distanceKm, leg, explain(s.landmark, terms, query.language))
        }

        return ItineraryOutcome.Plan(
            query = query, anchor = anchor, radiusKm = radius, radiusIsDefault = radiusIsDefault,
            requested = query.requested, capped = query.requested > MAX_STOPS,
            stops = stops, outsideRadius = outside, withoutLocation = withoutLocation, unknownConcepts = unknown,
        )
    }

    // ------------------------------------------------------------------ explanations

    /** Builds a stop's explanation from its record only (see [Explanation]). */
    fun explain(l: Landmark, terms: List<QueryTerm>, language: String): Explanation {
        val mine = if (language == "ar") listOf("descriptionArabic" to l.descriptionArabic, "longDescriptionArabic" to l.longDescriptionArabic)
        else listOf("description" to l.description, "longDescription" to l.longDescription)
        val other = if (language == "ar") listOf("description" to l.description, "longDescription" to l.longDescription)
        else listOf("descriptionArabic" to l.descriptionArabic, "longDescriptionArabic" to l.longDescriptionArabic)
        val otherLanguage = mine.all { it.second.isBlank() }
        val fields = if (otherLanguage) other else mine

        var best: Quote? = null
        var bestScore = -1.0
        for ((field, text) in fields) {
            for (sentence in ItineraryText.sentences(text)) {
                val idx = ItineraryText.index(sentence)
                val s = terms.filter { matches(idx, it) }.sumOf { it.idf }
                if (s > bestScore + 1e-9) {
                    bestScore = s
                    best = Quote(field, sentence, otherLanguage)
                }
            }
        }

        val tagField = if (language == "ar") "tagsArabic" else "tags"
        val tags = if (language == "ar") l.tagsArabic else l.tags
        val matched = tags.filter { tag -> val idx = ItineraryText.index(tag); terms.any { matches(idx, it) } }

        return Explanation(
            quote = best,
            tagField = tagField.takeIf { matched.isNotEmpty() },
            matchedTags = matched,
            sourceLinks = l.sources.filter { it.startsWith("https://") || it.startsWith("http://") },
        )
    }
}
