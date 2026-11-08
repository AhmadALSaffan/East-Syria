package com.example.eastsyria.Ui

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.eastsyria.MainPage.Data.Landmark
import java.util.Locale

/**
 * The app's language: English or Arabic, chosen on the onboarding or in Settings.
 *
 * Uses AndroidX per-app languages, so the choice survives restarts (stored by AppCompat on
 * Android 12, by the system on 13+) and also shows in the system's per-app language settings.
 * Setting it recreates the open activities in the new language and layout direction.
 */
object AppLanguage {
    const val ENGLISH = "en"
    const val ARABIC = "ar"

    /** The language in use: the app's own choice, else the phone's. */
    fun current(): String {
        val chosen = AppCompatDelegate.getApplicationLocales()
        val tag = if (chosen.isEmpty) Locale.getDefault().language else chosen[0]?.language
        return if (tag == ARABIC) ARABIC else ENGLISH
    }

    fun set(language: String) {
        if (language == current() && !AppCompatDelegate.getApplicationLocales().isEmpty) return
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language))
    }
}

fun Context.isArabic(): Boolean = resources.configuration.locales[0].language == AppLanguage.ARABIC

private fun pick(context: Context, english: String, arabic: String): String {
    val en = english.trim()
    val ar = arabic.trim()
    return if (context.isArabic()) ar.ifEmpty { en } else en.ifEmpty { ar }
}

/** The landmark's name in the app's language (falling back to the other one). */
fun Landmark.localName(context: Context) = pick(context, name, nameArabic)

/** The name in the other script, for the subtitle under the main name; empty when missing. */
fun Landmark.otherName(context: Context): String {
    val other = if (context.isArabic()) name.trim() else nameArabic.trim()
    return if (other == localName(context)) "" else other
}

fun Landmark.localCategory(context: Context) = pick(context, category, categoryArabic)
fun Landmark.localCity(context: Context) = pick(context, location.city, location.cityArabic)
fun Landmark.localGovernorate(context: Context) = pick(context, location.governorate, location.governorateArabic)
fun Landmark.localDescription(context: Context) = pick(context, description, descriptionArabic)
fun Landmark.localLongDescription(context: Context) = pick(context, longDescription, longDescriptionArabic)
fun Landmark.localStatus(context: Context) = pick(context, status, statusArabic)
fun Landmark.localTags(context: Context): List<String> =
    if (context.isArabic() && tagsArabic.isNotEmpty()) tagsArabic else tags
