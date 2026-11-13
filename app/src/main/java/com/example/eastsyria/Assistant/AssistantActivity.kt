package com.example.eastsyria.Assistant

import android.content.Intent
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import com.example.eastsyria.Details.LandmarkDetailActivity
import com.example.eastsyria.Itinerary.ItineraryCheck
import com.example.eastsyria.Itinerary.ItineraryOutcome
import com.example.eastsyria.Itinerary.ItineraryPlanner
import com.example.eastsyria.Map.MapActivity
import com.google.firebase.auth.FirebaseAuth
import android.util.Log
import com.example.eastsyria.MainPage.Data.Landmark
import com.example.eastsyria.R
import com.example.eastsyria.Ui.AppLanguage
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch

/**
 * The AI guide: a chat about travel and heritage in Syria. Replies come from the `assistant` edge
 * function (DeepSeek, grounded in the landmark catalog); landmarks it mentions show as cards that
 * open their page.
 *
 * Requests for nearby stops ("بدي ٣ أماكن قريبة من دير الزور مرتبطة بتاريخ الفرات") are answered on
 * the phone by [ItineraryPlanner] from the same catalog, without the model: retrieved records,
 * verified coordinates and quotes from the records only. They don't use a free question.
 */
class AssistantActivity : AppCompatActivity() {

    private val messages = mutableStateListOf<ChatMessage>()
    private val thinking = mutableStateOf(false)
    private val remaining = mutableStateOf<Int?>(null)
    private val landmarks = mutableStateMapOf<String, Landmark>()
    private var catalogLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        loadCatalog()
        lifecycleScope.launch { AssistantClient.remaining()?.let { remaining.value = it } }
        setContent {
            AssistantScreen(
                messages = messages,
                thinking = thinking.value,
                remaining = remaining.value,
                landmarks = landmarks,
                onSend = ::send,
                onOpenLandmark = { l ->
                    startActivity(Intent(this, LandmarkDetailActivity::class.java).putExtra("LANDMARK_ID", l.id))
                },
                itineraryActions = ItineraryActions(
                    onOpen = { l -> startActivity(Intent(this, LandmarkDetailActivity::class.java).putExtra("LANDMARK_ID", l.id)) },
                    onShowMap = ::showOnMap,
                    onPickAnchor = { ask, anchor ->
                        messages += ChatMessage(fromUser = true, text = anchor.name(ask.query.language))
                        answerItinerary(ItineraryPlanner.plan(ask.query.text, catalog(), anchorOverride = anchor))
                    },
                    onWiden = { plan, radius ->
                        messages += ChatMessage(fromUser = true, text = ItineraryLocale(this, plan.query.language)
                            .let { it.s(R.string.itinerary_widen, it.radius(radius)) })
                        answerItinerary(ItineraryPlanner.plan(plan.query.text, catalog(), anchorOverride = plan.anchor, radiusOverride = radius))
                    },
                ),
                onBack = { finish() },
            )
        }
    }

    /** The landmark cards are drawn from the app's own catalog (kept on disk by Firebase). */
    private fun loadCatalog() {
        FirebaseDatabase.getInstance().reference.child("landmarks").get()
            .addOnSuccessListener { snap ->
                snap.children.forEach { child ->
                    val key = child.key ?: return@forEach
                    child.getValue(Landmark::class.java)?.let { it.id = key; landmarks[key] = it }
                }
                catalogLoaded = true
            }
    }

    private fun catalog(): List<Landmark> = landmarks.values.toList()

    private fun send(text: String) {
        if (thinking.value) return
        messages += ChatMessage(fromUser = true, text = text)

        // Requests for stops are answered from the catalog itself, on the phone.
        val outcome = ItineraryPlanner.plan(text, catalog())
        if (outcome !is ItineraryOutcome.NotItinerary) {
            when {
                FirebaseAuth.getInstance().currentUser == null -> messages += error(R.string.assistant_err_signed_out)
                !catalogLoaded -> messages += error(R.string.itinerary_catalog_loading)
                else -> answerItinerary(outcome)
            }
            return
        }
        thinking.value = true
        val history = messages.filterNot { it.isError }.map { Turn(if (it.fromUser) "user" else "assistant", it.text) }

        lifecycleScope.launch {
            val result = AssistantClient.ask(history, AppLanguage.current())
            thinking.value = false
            when (result) {
                is AssistantResult.Reply -> if (result.remaining >= 0) remaining.value = result.remaining
                AssistantResult.DailyLimit -> remaining.value = 0
                else -> {}
            }
            messages += when (result) {
                is AssistantResult.Reply -> ChatMessage(false, result.text, result.landmarkIds)
                AssistantResult.SignedOut -> error(R.string.assistant_err_signed_out)
                AssistantResult.DailyLimit -> error(R.string.assistant_err_limit)
                AssistantResult.NotConfigured -> error(R.string.assistant_err_not_configured)
                AssistantResult.Offline -> error(R.string.err_offline)
                AssistantResult.Failed -> error(R.string.assistant_err_failed)
            }
        }
    }

    /** Shows a plan (after checking every stop against its record) or a question about where to start. */
    private fun answerItinerary(outcome: ItineraryOutcome) {
        val shown = if (outcome is ItineraryOutcome.Plan) {
            val reports = ItineraryCheck.verify(outcome, catalog())
            reports.filterNot { it.supported }.forEach { Log.w("Itinerary", "dropped ${it.landmarkId}: ${it.problems}") }
            val ok = reports.filter { it.supported }.map { it.landmarkId }.toSet()
            outcome.copy(stops = outcome.stops.filter { it.landmark.id in ok })
        } else outcome
        val loc = ItineraryLocale(this, shown.query.language)
        // A plain-text version keeps the conversation readable for the model on follow-up questions.
        val summary = when (shown) {
            is ItineraryOutcome.Plan -> loc.s(
                R.string.itinerary_summary,
                shown.stops.joinToString(loc.s(R.string.itinerary_list_separator)) { "${loc.n(it.number)}. ${it.landmark.name} / ${it.landmark.nameArabic}" },
            )
            is ItineraryOutcome.Ambiguous -> shown.options.joinToString(" / ") { it.name(shown.query.language) }
            is ItineraryOutcome.NotItinerary -> ""
        }
        messages += ChatMessage(fromUser = false, text = summary, itinerary = shown)
    }

    /** Opens the map with the stops numbered in order, the start and the radius. */
    private fun showOnMap(plan: ItineraryOutcome.Plan) {
        val intent = Intent(this, MapActivity::class.java)
            .putStringArrayListExtra(MapActivity.EXTRA_ITINERARY_IDS, ArrayList(plan.stops.map { it.landmark.id }))
        plan.anchor?.let { a ->
            intent.putExtra(MapActivity.EXTRA_ANCHOR_LAT, a.lat)
                .putExtra(MapActivity.EXTRA_ANCHOR_LNG, a.lng)
                .putExtra(MapActivity.EXTRA_ANCHOR_NAME, a.name(com.example.eastsyria.Ui.AppLanguage.current()))
        }
        plan.radiusKm?.let { intent.putExtra(MapActivity.EXTRA_RADIUS_KM, it) }
        startActivity(intent)
    }

    private fun error(res: Int) = ChatMessage(fromUser = false, text = getString(res), isError = true)
}
