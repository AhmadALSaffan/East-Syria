package com.example.eastsyria.CategoryList

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.eastsyria.R

/**
 * The six ways into the catalog, and the one place that decides which landmark
 * belongs to which. Categories and the category list used to carry their own
 * copies of this, which is how they came to disagree.
 */
object CategoryRules {

    data class Def(
        val type: String,
        @StringRes val name: Int,
        @DrawableRes val icon: Int
    )

    val all = listOf(
        Def("historical_sites", R.string.cat_name_historical, R.drawable.ic_historical),
        Def("nature_rivers", R.string.cat_name_nature, R.drawable.ic_waves),
        Def("local_culture", R.string.cat_name_culture, R.drawable.ic_people),
        Def("museums", R.string.cat_name_museums, R.drawable.ic_museum),
        Def("cuisine", R.string.cat_name_cuisine, R.drawable.ic_restaurant),
        Def("handicrafts", R.string.cat_name_crafts, R.drawable.ic_tools)
    )

    fun matches(
        type: String,
        category: String,
        tags: List<String>,
        name: String,
        isHistorical: Boolean
    ): Boolean = when (type) {
        "historical_sites" ->
            category.equals("Historical", ignoreCase = true) ||
                    category.equals("Archaeological", ignoreCase = true) ||
                    isHistorical

        "nature_rivers" ->
            tags.any {
                it.contains("river", ignoreCase = true) ||
                        it.contains("lake", ignoreCase = true) ||
                        it.contains("nature", ignoreCase = true)
            } || name.contains("river", ignoreCase = true) || name.contains("lake", ignoreCase = true)

        "local_culture" ->
            tags.any {
                it.contains("culture", ignoreCase = true) ||
                        it.contains("traditional", ignoreCase = true) ||
                        it.contains("local", ignoreCase = true)
            }

        "museums" ->
            tags.any { it.contains("museum", ignoreCase = true) } ||
                    name.contains("museum", ignoreCase = true)

        "cuisine" ->
            tags.any {
                it.contains("food", ignoreCase = true) ||
                        it.contains("cuisine", ignoreCase = true) ||
                        it.contains("dish", ignoreCase = true)
            }

        "handicrafts" ->
            tags.any {
                it.contains("craft", ignoreCase = true) ||
                        it.contains("artisan", ignoreCase = true) ||
                        it.contains("traditional art", ignoreCase = true)
            }

        else -> true
    }
}
