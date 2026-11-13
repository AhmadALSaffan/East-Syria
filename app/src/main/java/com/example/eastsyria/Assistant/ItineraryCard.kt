package com.example.eastsyria.Assistant

import android.content.Context
import android.content.res.Configuration
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.example.eastsyria.Itinerary.Anchor
import com.example.eastsyria.Itinerary.ItineraryOutcome
import com.example.eastsyria.Itinerary.ItineraryPlanner
import com.example.eastsyria.Itinerary.Stop
import com.example.eastsyria.MainPage.Data.Landmark
import com.example.eastsyria.Onboarding.EsShapes
import com.example.eastsyria.Onboarding.EsType
import com.example.eastsyria.Onboarding.esColors
import com.example.eastsyria.R
import com.example.eastsyria.Ui.localCity
import com.example.eastsyria.Ui.localName
import java.text.NumberFormat
import java.util.Locale

/** What the itinerary card can ask the screen to do. */
internal class ItineraryActions(
    val onOpen: (Landmark) -> Unit,
    val onShowMap: (ItineraryOutcome.Plan) -> Unit,
    val onPickAnchor: (ItineraryOutcome.Ambiguous, Anchor) -> Unit,
    val onWiden: (ItineraryOutcome.Plan, Double) -> Unit,
)

/** Strings and numbers in the request's language, whatever the app's language is. */
internal class ItineraryLocale(base: Context, val language: String) {
    val locale: Locale = Locale.forLanguageTag(language)
    val context: Context = base.createConfigurationContext(Configuration(base.resources.configuration).apply { setLocale(locale) })
    fun s(id: Int, vararg args: Any): String = context.getString(id, *args)
    fun n(value: Int): String = NumberFormat.getInstance(locale).format(value)
    /** A distance: one decimal below 200 km, so 100.4 km never reads as "100" next to a 100 km radius. */
    fun km(value: Double): String =
        if (value >= 200) String.format(locale, "%,.0f", value) else String.format(locale, "%.1f", value)
    /** A radius, always a whole number of kilometres. */
    fun radius(value: Double): String = String.format(locale, "%,.0f", value)
}

/**
 * A reply of suggested stops in the chat. It follows the request's language: an Arabic request
 * gets an Arabic, right-to-left card, an English one an English, left-to-right card.
 */
@Composable
internal fun ItineraryCard(outcome: ItineraryOutcome, actions: ItineraryActions) {
    val base = LocalContext.current
    val lang = outcome.query.language
    val loc = remember(lang, base) { ItineraryLocale(base, lang) }
    CompositionLocalProvider(LocalLayoutDirection provides if (lang == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr) {
        when (outcome) {
            is ItineraryOutcome.Plan -> PlanCard(outcome, loc, actions)
            is ItineraryOutcome.Ambiguous -> AskCard(outcome, loc, actions)
            is ItineraryOutcome.NotItinerary -> Unit
        }
    }
}

@Composable
private fun PlanCard(plan: ItineraryOutcome.Plan, loc: ItineraryLocale, actions: ItineraryActions) {
    val c = esColors()
    val anchorName = plan.anchor?.name(loc.language)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(EsShapes.lg)
            .background(c.surface)
            .border(1.dp, c.line, EsShapes.lg)
            .padding(16.dp),
    ) {
        Text(loc.s(R.string.itinerary_title), style = EsType.title.copy(fontSize = 17.sp), color = c.ice)
        if (plan.radiusKm != null && anchorName != null) {
            Spacer(Modifier.height(4.dp))
            Text(loc.s(R.string.itinerary_scope, loc.radius(plan.radiusKm), anchorName), style = EsType.meta, color = c.iceDim)
            if (plan.radiusIsDefault) {
                Text(loc.s(R.string.itinerary_radius_default, loc.radius(ItineraryPlanner.DEFAULT_NEAR_RADIUS_KM)), style = EsType.meta, color = c.iceMute)
            }
        }
        if (plan.capped) Note(loc.s(R.string.itinerary_capped))

        if (plan.stops.isEmpty()) {
            Note(
                if (plan.radiusKm != null && anchorName != null) loc.s(R.string.itinerary_none, loc.radius(plan.radiusKm), anchorName)
                else loc.s(R.string.itinerary_none_anywhere)
            )
        }
        plan.stops.forEach { stop ->
            Spacer(Modifier.height(14.dp))
            StopRow(stop, plan, loc, actions)
        }

        val wanted = plan.requested.coerceAtMost(ItineraryPlanner.MAX_STOPS)
        if (plan.stops.isNotEmpty() && plan.stops.size < wanted && plan.radiusKm != null) {
            Note(loc.s(R.string.itinerary_shortfall, loc.n(plan.stops.size), loc.n(wanted), loc.radius(plan.radiusKm)))
        }
        if (plan.unknownConcepts.isNotEmpty()) Note(loc.s(R.string.itinerary_unknown))
        if (plan.outsideRadius.isNotEmpty() && plan.radiusKm != null) {
            Note(
                loc.s(R.string.itinerary_outside, loc.radius(plan.radiusKm)) + " " +
                    plan.outsideRadius.joinToString(loc.s(R.string.itinerary_list_separator)) {
                        loc.s(R.string.itinerary_outside_item, it.landmark.localName(loc.context), loc.km(it.distanceKm))
                    }
            )
        }
        if (plan.withoutLocation.isNotEmpty()) {
            Note(loc.s(R.string.itinerary_no_location, plan.withoutLocation.joinToString(loc.s(R.string.itinerary_list_separator)) { it.localName(loc.context) }))
        }

        if (plan.stops.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(loc.s(R.string.itinerary_order_note), style = EsType.meta.copy(fontSize = 12.sp), color = c.iceMute)
        }
        Spacer(Modifier.height(6.dp))
        Text(loc.s(R.string.itinerary_grounded), style = EsType.meta.copy(fontSize = 12.sp, fontStyle = FontStyle.Italic), color = c.iceMute)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 12.dp)) {
            if (plan.stops.isNotEmpty()) PillButton(loc.s(R.string.itinerary_show_map), primary = true) { actions.onShowMap(plan) }
            val r = plan.radiusKm
            if (r != null && plan.stops.size < wanted && r < 300) {
                val wider = (r * 2).coerceAtMost(300.0)
                PillButton(loc.s(R.string.itinerary_widen, loc.radius(wider)), primary = false) { actions.onWiden(plan, wider) }
            }
        }
    }
}

@Composable
private fun StopRow(stop: Stop, plan: ItineraryOutcome.Plan, loc: ItineraryLocale, actions: ItineraryActions) {
    val c = esColors()
    val l = stop.landmark
    val e = stop.explanation
    val uri = LocalUriHandler.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(EsShapes.md)
            .clickable(role = Role.Button) { actions.onOpen(l) },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(28.dp).background(c.red, CircleShape),
            ) {
                Text(loc.n(stop.number), style = EsType.label.copy(fontSize = 13.sp), color = c.onRed)
            }
            Spacer(Modifier.width(10.dp))
            AndroidView(
                factory = { ctx -> ImageView(ctx).apply { scaleType = ImageView.ScaleType.CENTER_CROP } },
                update = { view ->
                    Glide.with(view).load(l.imageUrl).placeholder(R.drawable.placeholder_dark)
                        .error(R.drawable.placeholder_dark).into(view)
                },
                modifier = Modifier.size(44.dp).clip(EsShapes.sm),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(l.localName(loc.context), style = EsType.title.copy(fontSize = 15.sp), color = c.ice, maxLines = 2)
                val where = buildList {
                    add(l.localCity(loc.context))
                    val a = plan.anchor
                    if (a != null && stop.fromAnchorKm != null) add(loc.s(R.string.itinerary_from_start, loc.km(stop.fromAnchorKm), a.name(loc.language)))
                    if (stop.number > 1 && stop.legKm != null) add(loc.s(R.string.itinerary_leg, loc.km(stop.legKm), loc.n(stop.number - 1)))
                }.filter { it.isNotBlank() }
                Text(where.joinToString(" · "), style = EsType.meta.copy(fontSize = 12.sp), color = c.iceMute)
            }
            Icon(painterResource(R.drawable.ic_arrow_right), null, tint = c.redSoft, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.padding(start = 38.dp, top = 8.dp)) {
            e.quote?.let { q ->
                Text("«${q.text}»", style = EsType.body.copy(fontSize = 15.sp, lineHeight = 22.sp), color = c.ice)
                val field = loc.s(if (q.field.startsWith("long")) R.string.itinerary_field_description else R.string.itinerary_field_summary)
                Text(loc.s(R.string.itinerary_quote_source, field), style = EsType.meta.copy(fontSize = 12.sp), color = c.iceMute)
                if (q.otherLanguage) Text(loc.s(R.string.itinerary_other_language), style = EsType.meta.copy(fontSize = 12.sp), color = c.iceMute)
            }
            if (e.matchedTags.isNotEmpty()) {
                Text(
                    loc.s(R.string.itinerary_matches, e.matchedTags.joinToString(loc.s(R.string.itinerary_list_separator))),
                    style = EsType.meta.copy(fontSize = 12.sp), color = c.iceDim,
                )
            }
            if (e.sourceLinks.isNotEmpty()) {
                Text(loc.s(R.string.itinerary_sources), style = EsType.meta.copy(fontSize = 12.sp), color = c.iceDim, modifier = Modifier.padding(top = 4.dp))
                e.sourceLinks.forEach { link ->
                    Text(
                        link.removePrefix("https://").removePrefix("http://").take(60),
                        style = EsType.meta.copy(fontSize = 12.sp, textDecoration = TextDecoration.Underline),
                        color = c.redSoft,
                        maxLines = 1,
                        modifier = Modifier.clickable(role = Role.Button) { runCatching { uri.openUri(link) } },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AskCard(ask: ItineraryOutcome.Ambiguous, loc: ItineraryLocale, actions: ItineraryActions) {
    val c = esColors()
    val prompt = when (ask.reason) {
        ItineraryOutcome.Ambiguous.Reason.SEVERAL_PLACES -> R.string.itinerary_ask_several
        ItineraryOutcome.Ambiguous.Reason.GENERIC_PLACE -> R.string.itinerary_ask_generic
        ItineraryOutcome.Ambiguous.Reason.NEAR_ME -> R.string.itinerary_ask_near_me
        ItineraryOutcome.Ambiguous.Reason.NO_PLACE -> R.string.itinerary_ask_no_place
    }
    Column(Modifier.fillMaxWidth()) {
        Text(loc.s(prompt), style = EsType.body.copy(lineHeight = 25.sp), color = c.ice)
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ask.options.forEach { option ->
                PillButton(option.name(loc.language), primary = false) { actions.onPickAnchor(ask, option) }
            }
        }
    }
}

@Composable
private fun Note(text: String) {
    val c = esColors()
    Text(text, style = EsType.meta, color = c.iceDim, modifier = Modifier.padding(top = 10.dp))
}

@Composable
private fun PillButton(text: String, primary: Boolean, onClick: () -> Unit) {
    val c = esColors()
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .heightIn(min = 40.dp)
            .clip(EsShapes.pill)
            .background(if (primary) c.red else c.surfaceHigh)
            .border(1.dp, if (primary) c.red else c.line, EsShapes.pill)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(text, style = EsType.button.copy(fontSize = 14.sp), color = if (primary) c.onRed else c.ice)
    }
}
