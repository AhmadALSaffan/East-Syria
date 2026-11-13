package com.example.eastsyria.Itinerary

/**
 * Text handling shared by the query parser, the retriever and the explanation check, for Arabic
 * and English.
 *
 * Arabic is normalised the usual way for search (no diacritics or tatweel, one alef, ى→ي, ة→ه,
 * ؤ→و, ئ→ي, Arabic-Indic digits → 0-9). Instead of a single stem, each word gets a small set of
 * plausible forms (with and without و/ف/ب/ك/ل, "ال" and common suffixes); two words match when
 * their sets meet. That keeps "بتاريخ" ~ "تاريخي" and "الفرات" ~ "فرات" without a stemmer that
 * would wrongly cut "فرات" to "رات".
 */
object ItineraryText {

    private val tashkeel = Regex("[\\u064B-\\u0652\\u0670\\u0640]")
    private val nonWord = Regex("[^\\p{L}\\p{Nd}]+")
    private val spaces = Regex("\\s+")
    private val arSuffixes = listOf("ات", "ين", "ون", "يه", "ه", "ي")

    fun hasArabic(s: String) = s.any { it in '؀'..'ۿ' }

    /** Lower-cased, Arabic-normalised, punctuation turned into spaces. */
    fun normalize(raw: String): String {
        val sb = StringBuilder(raw.length)
        for (ch in raw.lowercase()) {
            sb.append(
                when (ch) {
                    in '٠'..'٩' -> '0' + (ch - '٠')
                    in '۰'..'۹' -> '0' + (ch - '۰')
                    'أ', 'إ', 'آ', 'ٱ' -> 'ا'
                    'ى' -> 'ي'
                    'ة' -> 'ه'
                    'ؤ' -> 'و'
                    'ئ' -> 'ي'
                    '’', '\'' -> ' '
                    else -> ch
                }
            )
        }
        return sb.toString().replace(tashkeel, "").replace(nonWord, " ").trim()
    }

    fun tokens(raw: String): List<String> = normalize(raw).split(' ').filter { it.isNotEmpty() }

    /** Whitespace collapsed; used to compare a quote with the field it came from. */
    fun squash(raw: String): String = raw.replace(spaces, " ").trim()

    /** The forms a (normalised) word may take; two words match when their forms meet. */
    fun variants(token: String): Set<String> {
        if (!hasArabic(token)) return linkedSetOf(token, stemEnglish(token))
        val bases = mutableListOf(token)
        if (token.length >= 4 && token[0] in "وفبكل") bases += token.substring(1)
        for (b in bases.toList()) {
            if ((b.startsWith("ال") || b.startsWith("لل")) && b.length >= 5) bases += b.substring(2)
        }
        val out = linkedSetOf<String>()
        for (b in bases) {
            out += b
            for (s in arSuffixes) if (b.endsWith(s) && b.length - s.length >= 3) out += b.dropLast(s.length)
        }
        return out
    }

    private fun stemEnglish(t: String): String = when {
        t.length <= 3 -> t
        t.endsWith("sses") -> t.dropLast(2)
        t.endsWith("ies") && t.length > 4 -> t.dropLast(3) + "y"
        t.endsWith("ss") || t.endsWith("us") -> t
        t.endsWith("s") -> t.dropLast(1)
        else -> t
    }

    /** A text as a sequence of word-form sets, ready for phrase matching. */
    fun index(raw: String): List<Set<String>> = tokens(raw).map { variants(it) }

    /** Does [phrase] (a sequence of form sets) occur in [text]? Returns the start position or -1. */
    fun find(text: List<Set<String>>, phrase: List<Set<String>>, from: Int = 0): Int {
        if (phrase.isEmpty() || phrase.size > text.size) return -1
        outer@ for (i in from..text.size - phrase.size) {
            for (j in phrase.indices) if (text[i + j].none { it in phrase[j] }) continue@outer
            return i
        }
        return -1
    }

    /** Splits a description into sentences, keeping each sentence's own wording. */
    fun sentences(raw: String): List<String> =
        squash(raw).split(Regex("(?<=[.!?؟])\\s+")).map { it.trim() }.filter { it.length > 1 }
}
