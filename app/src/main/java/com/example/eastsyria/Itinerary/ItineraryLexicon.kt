package com.example.eastsyria.Itinerary

/**
 * The fixed vocabulary of the itinerary planner: bilingual concepts for retrieval, the words that
 * shape a request (counts, "near", units, filler), and a small gazetteer of towns.
 *
 * Retrieval "by meaning" works through these concepts: an Arabic word in the query and an English
 * word in a record (or the other way round) match when they belong to the same concept. There are
 * no embeddings; this keeps results reproducible and explainable, and the catalog is small.
 */
object ItineraryLexicon {

    /** A concept and the words (any language, may be several words) that express it. */
    class Concept(val id: String, terms: List<String>) {
        val phrases: List<List<Set<String>>> = terms.map { ItineraryText.index(it) }.filter { it.isNotEmpty() }
    }

    val concepts: List<Concept> = listOf(
        Concept("euphrates", listOf("euphrates", "furat", "alfurat", "الفرات")),
        Concept("river", listOf("river", "riverbank", "river crossing", "نهر", "ضفه", "ضفتي", "معابر النهر")),
        Concept("history", listOf("history", "historic", "historical", "heritage", "تاريخ", "تاريخي", "تراث")),
        Concept("ancient", listOf("ancient", "antiquity", "archaeological", "archaeology", "ruins", "قديم", "قديمه", "اثري", "اثريه", "اثار", "اطلال")),
        Concept("fortress", listOf("fortress", "castle", "citadel", "fort", "fortified", "stronghold", "fortification", "قلعه", "قلاع", "حصن", "حصون", "معقل", "محصنه")),
        Concept("medieval", listOf("medieval", "middle ages", "ayyubid", "mamluk", "zengid", "عصور وسطى", "ايوبي", "مملوكي", "زنكي")),
        Concept("islamic", listOf("islamic", "mosque", "اسلامي", "اسلاميه", "مسجد")),
        Concept("roman", listOf("roman", "greco roman", "hellenistic", "byzantine", "روماني", "رومانيه", "يوناني", "هلنستي", "بيزنطي", "بيزنطيه")),
        Concept("city", listOf("city", "cities", "town", "مدينه", "مدن")),
        Concept("bridge", listOf("bridge", "causeway", "جسر", "جسور")),
        Concept("lake", listOf("lake", "reservoir", "بحيره")),
        Concept("desert", listOf("desert", "steppe", "oasis", "صحراء", "صحراويه", "باديه", "واحه")),
        Concept("trade", listOf("trade", "trading", "caravan", "silk road", "تجاري", "تجاريا", "قوافل", "طريق الحرير")),
        Concept("religion", listOf("church", "christian", "synagogue", "temple", "pilgrimage", "كنيسه", "كنائس", "مسيحي", "مسيحيه", "كنيس", "معبد", "حج")),
        Concept("bronze", listOf("bronze age", "mesopotamia", "babylon", "العصر البرونزي", "برونزي", "ما بين النهرين", "بابل")),
        Concept("neolithic", listOf("neolithic", "prehistoric", "نيوليتي", "نيوليتيه", "ما قبل التاريخ")),
        Concept("unesco", listOf("unesco", "world heritage", "يونسكو", "التراث العالمي")),
        Concept("french", listOf("french", "france", "mandate", "فرنسي", "فرنسيه", "الانتداب")),
        Concept("museum", listOf("museum", "متحف", "متاحف")),
    )

    /** Words that say "close to a place". */
    val nearWords: List<List<Set<String>>> = listOf(
        "near", "nearby", "near to", "around", "close to", "close by", "next to", "not far from", "within",
        "قريب", "قريبه", "قريبين", "قرب", "بالقرب", "جنب", "بجانب", "حول", "ضمن", "مجاور", "جوار",
    ).map { ItineraryText.index(it) }

    /** "Near me" has no starting point the guide can know. */
    val nearMeWords: List<List<Set<String>>> = listOf(
        "near me", "around me", "close to me", "near here", "nearby me", "قريب مني", "قريبه مني", "حولي", "جنبي", "قريب علي", "من موقعي",
    ).map { ItineraryText.index(it) }

    /** Words for "places", which together with a count or a "near" make a stop request. */
    val placeWords: Set<String> = setOf(
        "place", "places", "site", "sites", "landmark", "landmarks", "stop", "stops", "spot", "spots", "itinerary", "route", "tour",
        "اماكن", "مكان", "مكانين", "مواقع", "موقع", "معالم", "معلم", "معلمين", "محطات", "محطه", "جوله", "مسار",
    ).map { ItineraryText.normalize(it) }.toSet()

    val numberWords: Map<String, Int> = mapOf(
        "one" to 1, "two" to 2, "couple" to 2, "three" to 3, "few" to 3, "four" to 4, "five" to 5,
        "واحد" to 1, "واحده" to 1, "اثنين" to 2, "اثنان" to 2, "مكانين" to 2, "معلمين" to 2, "موقعين" to 2,
        "ثلاث" to 3, "ثلاثه" to 3, "تلات" to 3, "تلاته" to 3, "اربع" to 4, "اربعه" to 4, "خمس" to 5, "خمسه" to 5,
    ).mapKeys { ItineraryText.normalize(it.key) }

    val unitWords: Set<String> = setOf("km", "kms", "kilometer", "kilometers", "kilometre", "kilometres", "كم", "كيلو", "كيلومتر", "كيلومترات")
        .map { ItineraryText.normalize(it) }.toSet()

    /** Request filler that carries no topic. Compared on word forms. */
    val stopWords: Set<String> = (setOf(
        "a", "an", "the", "me", "i", "we", "my", "want", "would", "like", "show", "give", "suggest", "find", "list", "some", "any",
        "please", "can", "could", "you", "which", "what", "where", "visit", "see", "go", "trip", "plan", "order", "best", "good", "top",
        "to", "from", "of", "in", "on", "at", "by", "with", "and", "or", "that", "are", "is", "be", "for", "about", "related", "linked",
        "connected", "tied", "worth", "km", "kilometers", "near", "nearby", "around", "close", "within", "next", "far", "not", "it", "its",
        "بدي", "ابي", "ابغى", "اريد", "بدنا", "عايز", "اعطني", "اقترح", "اقترحلي", "ورجيني", "شو", "ما", "ماهي", "هي", "هو", "التي", "الذي",
        "من", "في", "الى", "على", "عن", "مع", "او", "و", "مرتبط", "مرتبطه", "متعلق", "متعلقه", "علاقه", "لها", "له", "حول", "ضمن",
        "قريب", "قريبه", "قرب", "بالقرب", "زياره", "ازور", "رحله", "خطه", "لي", "انا", "يا", "بس", "كمان", "شي", "شي", "اشوف",
    ) + placeWords + numberWords.keys + unitWords).map { ItineraryText.normalize(it) }.toSet()

    /** A town that can start an itinerary. Coordinates are city centres. */
    data class Town(val key: String, val nameEn: String, val nameAr: String, val lat: Double, val lng: Double, val aliases: List<String>) {
        val phrases: List<List<Set<String>>> = (aliases + nameEn + nameAr).map { ItineraryText.index(it) }.filter { it.isNotEmpty() }
    }

    /**
     * Source: Wikipedia's coordinates for each town (en.wikipedia.org API, prop=coordinates),
     * checked 2026-11-13. A unit test keeps each one inside Syria and near the catalog's own
     * records for that town.
     */
    const val TOWN_SOURCE = "Wikipedia town coordinates, checked 2026-11-13"

    val towns: List<Town> = listOf(
        Town("deir-ez-zor", "Deir ez-Zor", "دير الزور", 35.33333, 40.15, listOf("deir ezzor", "deir al zor", "deir el zor", "deir alzor", "deir zor", "dayr az zawr", "deirezzor", "ديرالزور")),
        Town("raqqa", "Raqqa", "الرقة", 35.95, 39.01, listOf("raqqah", "ar raqqah", "al raqqa", "rakka")),
        Town("mayadin", "Al-Mayadin", "الميادين", 35.01833, 40.45333, listOf("mayadin", "mayadeen", "mayadine")),
        Town("abu-kamal", "Abu Kamal", "البوكمال", 34.45361, 40.93667, listOf("al bukamal", "albukamal", "bukamal", "abukamal", "ابو كمال")),
        Town("palmyra", "Palmyra", "تدمر", 34.55139, 38.26806, listOf("tadmur", "tadmor")),
        Town("hasakah", "Al-Hasakah", "الحسكة", 36.51167, 40.74222, listOf("hasakah", "hasaka", "hassakeh")),
        Town("qamishli", "Qamishli", "القامشلي", 37.05472, 41.22806, listOf("qamishly", "kamishli")),
        Town("tabqa", "Tabqa", "الطبقة", 35.83667, 38.54806, listOf("al thawrah")),
    )
}
