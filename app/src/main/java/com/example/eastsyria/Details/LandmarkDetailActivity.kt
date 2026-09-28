package com.example.eastsyria.Details

import android.animation.ValueAnimator
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.widget.NestedScrollView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.eastsyria.MainPage.Data.Landmark
import com.example.eastsyria.R
import com.example.eastsyria.databinding.ActivityLandmarkDetailBinding
import com.example.eastsyria.databinding.ItemRecordRowBinding
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LandmarkDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLandmarkDetailBinding
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var currentLandmark: Landmark? = null
    private var isEnglish = true
    private var isSaved = false
    private var landmarkId: String = ""
    private var pulseAnimator: ValueAnimator? = null

    private enum class ScreenState { LOADING, CONTENT, MISSING, ERROR }

    companion object {
        private const val TAG = "LandmarkDetailActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // The photograph runs under both system bars, so their icons stay light.
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        binding = ActivityLandmarkDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        landmarkId = intent.getStringExtra("LANDMARK_ID").orEmpty()

        setupInsets()
        setupScrollBehavior()
        setupControls()
        updateLanguageUI()

        if (landmarkId.isEmpty()) {
            showState(ScreenState.MISSING)
            return
        }
        loadLandmarkDetails(landmarkId)
    }

    override fun onDestroy() {
        stopPulse()
        super.onDestroy()
    }

    // ------------------------------------------------------------ chrome

    private fun setupInsets() {
        val buttonMargin = dp(16)
        val contentPadding = dp(128)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            binding.topBarBg.updateLayoutParams<ViewGroup.LayoutParams> {
                height = dp(64) + bars.top
            }
            binding.barRow.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = bars.top
            }
            binding.btnGetDirections.updateLayoutParams<CoordinatorLayout.LayoutParams> {
                bottomMargin = buttonMargin + bars.bottom
            }
            binding.contentGroup.updatePadding(bottom = contentPadding + bars.bottom)
            insets
        }
    }

    /**
     * The photograph drifts at half speed; the bar gains a solid backing and
     * then the landmark's name as the hero's own name scrolls beneath it.
     */
    private fun setupScrollBehavior() {
        val backingStart = dp(200)
        val backingSpan = dp(90)
        val titleStart = dp(250)
        val titleSpan = dp(70)
        binding.scrollDetail.setOnScrollChangeListener(
            NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
                binding.ivHeroImage.translationY = scrollY * 0.5f
                binding.topBarBg.alpha =
                    ((scrollY - backingStart).toFloat() / backingSpan).coerceIn(0f, 1f)
                binding.tvBarTitle.alpha =
                    ((scrollY - titleStart).toFloat() / titleSpan).coerceIn(0f, 1f)
            }
        )
    }

    private fun setupControls() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnShare.setOnClickListener { shareLandmark() }
        binding.btnBookmark.setOnClickListener { toggleBookmark() }
        binding.btnRetry.setOnClickListener { loadLandmarkDetails(landmarkId) }

        binding.btnEnglish.setOnClickListener {
            isEnglish = true
            updateLanguageUI()
            displayLandmarkData()
        }
        binding.btnArabic.setOnClickListener {
            isEnglish = false
            updateLanguageUI()
            displayLandmarkData()
        }
    }

    // ------------------------------------------------------------ saved

    private fun checkIfSaved() {
        val userId = auth.currentUser?.uid
        if (userId == null || landmarkId.isEmpty()) {
            updateBookmarkUI(false)
            return
        }

        database.reference
            .child("users")
            .child(userId)
            .child("savedLandmarks")
            .child(landmarkId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    isSaved = snapshot.exists()
                    updateBookmarkUI(isSaved)
                }

                override fun onCancelled(error: DatabaseError) {
                    updateBookmarkUI(false)
                }
            })
    }

    private fun toggleBookmark() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            showMessage(R.string.detail_login_to_save)
            return
        }
        if (landmarkId.isEmpty()) return

        val savedRef = database.reference
            .child("users")
            .child(userId)
            .child("savedLandmarks")
            .child(landmarkId)

        if (isSaved) {
            savedRef.removeValue()
                .addOnSuccessListener {
                    isSaved = false
                    updateBookmarkUI(false)
                    showMessage(R.string.detail_unsaved)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to remove saved landmark: ${e.message}")
                    showMessage(R.string.detail_save_failed)
                }
        } else {
            val saveData = mapOf(
                "savedAt" to System.currentTimeMillis(),
                "landmarkId" to landmarkId
            )

            savedRef.setValue(saveData)
                .addOnSuccessListener {
                    isSaved = true
                    updateBookmarkUI(true)
                    showMessage(R.string.detail_saved)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to save landmark: ${e.message}")
                    showMessage(R.string.detail_save_failed)
                }
        }
    }

    private fun updateBookmarkUI(saved: Boolean) {
        val tint = ContextCompat.getColor(
            this,
            if (saved) R.color.terracotta_soft else R.color.ivory
        )
        binding.btnBookmark.setImageResource(
            if (saved) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark
        )
        binding.btnBookmark.imageTintList = ColorStateList.valueOf(tint)
        binding.btnBookmark.contentDescription = getString(
            if (saved) R.string.detail_unbookmark else R.string.detail_bookmark
        )
    }

    /** A transient confirmation, anchored above the directions pill when it is showing. */
    private fun showMessage(resId: Int) {
        Snackbar.make(binding.root, resId, Snackbar.LENGTH_SHORT).apply {
            setBackgroundTint(ContextCompat.getColor(context, R.color.surface_high))
            setTextColor(ContextCompat.getColor(context, R.color.ivory))
            if (binding.btnGetDirections.visibility == View.VISIBLE) {
                setAnchorView(binding.btnGetDirections)
            }
            show()
        }
    }

    // ------------------------------------------------------------ language

    /** The active segment takes the same terracotta wash as the home navigation. */
    private fun updateLanguageUI() {
        val active = ColorStateList.valueOf(0x33E2703A)
        val inactive = ColorStateList.valueOf(0x00000000)
        val activeText = ContextCompat.getColor(this, R.color.terracotta_soft)
        val inactiveText = ContextCompat.getColor(this, R.color.ivory_dim)

        binding.btnEnglish.backgroundTintList = if (isEnglish) active else inactive
        binding.btnEnglish.setTextColor(if (isEnglish) activeText else inactiveText)
        binding.btnArabic.backgroundTintList = if (isEnglish) inactive else active
        binding.btnArabic.setTextColor(if (isEnglish) inactiveText else activeText)

        // Arabic reads right to left, in the face that carries it, and never
        // smaller than the English it replaces.
        binding.tvDescription.apply {
            if (isEnglish) {
                layoutDirection = View.LAYOUT_DIRECTION_LTR
                typeface = Typeface.DEFAULT
                textSize = 16f
                setLineSpacing(dp(7).toFloat(), 1f)
            } else {
                layoutDirection = View.LAYOUT_DIRECTION_RTL
                typeface = ResourcesCompat.getFont(context, R.font.amiri_regular)
                textSize = 19f
                setLineSpacing(dp(9).toFloat(), 1f)
            }
        }
    }

    // ------------------------------------------------------------ data

    private fun loadLandmarkDetails(landmarkId: String) {
        showState(ScreenState.LOADING)

        val landmarkRef = database.reference.child("landmarks").child(landmarkId)
        landmarkRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    showState(ScreenState.MISSING)
                    return
                }
                val landmark = snapshot.getValue(Landmark::class.java)
                if (landmark == null) {
                    showState(ScreenState.ERROR)
                    return
                }
                currentLandmark = landmark
                displayLandmarkData()
                showState(ScreenState.CONTENT)
                checkIfSaved()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Database error: ${error.message}")
                showState(ScreenState.ERROR)
            }
        })
    }

    private fun displayLandmarkData() {
        val landmark = currentLandmark ?: return

        Glide.with(this)
            .load(landmark.imageUrl)
            .placeholder(R.drawable.placeholder_dark)
            .error(R.drawable.placeholder_dark)
            .centerCrop()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .transition(DrawableTransitionOptions.withCrossFade(250))
            .into(binding.ivHeroImage)

        val primary = if (isEnglish) landmark.name else landmark.nameArabic
        val secondary = if (isEnglish) landmark.nameArabic else landmark.name
        binding.tvLandmarkName.text = primary
        binding.tvBarTitle.text = primary
        binding.tvArabicName.text = secondary
        binding.tvArabicName.visibility =
            if (secondary.isBlank()) View.GONE else View.VISIBLE

        // Was "${category} SITE", which rendered "CUISINE SITE" for a kitchen.
        val category = (if (isEnglish) landmark.category else landmark.categoryArabic).trim()
        val place = if (isEnglish) {
            joinPlace(landmark.location.city, landmark.location.governorate)
        } else {
            joinPlace(landmark.location.cityArabic, landmark.location.governorateArabic)
        }
        binding.tvMeta.text = when {
            category.isNotEmpty() && place.isNotEmpty() ->
                getString(R.string.home_meta_separator, category, place)

            category.isNotEmpty() -> category
            else -> place
        }
        binding.tvMeta.visibility = if (binding.tvMeta.text.isEmpty()) View.GONE else View.VISIBLE

        // A curator's mark, not a review score. It hides when unset rather
        // than printing 0.0, and there is no review count because nothing in
        // this app can author a review.
        if (landmark.rating > 0.0) {
            val rating = String.format("%.1f", landmark.rating)
            binding.tvRating.text = rating
            binding.layoutRating.contentDescription = getString(R.string.detail_rating, rating)
            binding.layoutRating.visibility = View.VISIBLE
        } else {
            binding.layoutRating.visibility = View.GONE
        }

        val description = (if (isEnglish) landmark.longDescription else landmark.longDescriptionArabic).trim()
        binding.tvDescription.text = description
        binding.tvDescription.visibility = if (description.isEmpty()) View.GONE else View.VISIBLE

        buildRecordTables(landmark)
        renderTags(landmark)

        // Coordinates of 0,0 mean the record has none; a button that opens the
        // Gulf of Guinea is a control that lies.
        val hasCoordinates =
            landmark.location.latitude != 0.0 || landmark.location.longitude != 0.0
        binding.btnGetDirections.visibility = if (hasCoordinates) View.VISIBLE else View.GONE
        binding.btnGetDirections.setOnClickListener {
            openGoogleMaps(landmark.location.latitude, landmark.location.longitude)
        }
    }

    private fun joinPlace(city: String, governorate: String): String {
        val a = city.trim()
        val b = governorate.trim()
        return when {
            a.isNotEmpty() && b.isNotEmpty() && a != b -> "$a, $b"
            a.isNotEmpty() -> a
            else -> b
        }
    }

    /**
     * The record, built from real data only.
     *
     * The three hardcoded "Architectural Features" cards and the hardcoded
     * "Historical Significance" panel this replaces were Al-Rahba's text
     * rendered on every landmark in the catalog.
     *
     * The record renders whatever the admin actually put in `details`, so a
     * new key needs no code change. A fact with no value is never added, which
     * is why nothing here ever reads "N/A".
     */
    private fun buildRecordTables(landmark: Landmark) {
        val record = binding.recordTable
        val visiting = binding.visitTable
        record.removeAllViews()
        visiting.removeAllViews()

        // Known keys first, in a deliberate order; anything else the admin
        // added follows, alphabetically, with its key humanised.
        val knownOrder = linkedMapOf(
            "yearBuilt" to getString(R.string.record_year_built),
            "elevation" to getString(R.string.record_elevation),
            "material" to getString(R.string.record_material)
        )

        knownOrder.forEach { (key, label) ->
            addRecordRow(record, label, landmark.details[key])
        }
        landmark.details.keys
            .filter { it !in knownOrder.keys }
            .sorted()
            .forEach { key ->
                addRecordRow(record, humaniseKey(key), landmark.details[key])
            }

        // The practical layer carries an icon: these are the facts someone
        // standing at the gate is looking for.
        addRecordRow(visiting, getString(R.string.record_visiting_hours), landmark.visitingHours, R.drawable.ic_clock)
        addRecordRow(visiting, getString(R.string.record_entry_fee), landmark.entryFee, R.drawable.ic_ticket)
        addRecordRow(visiting, getString(R.string.record_best_time), landmark.bestTimeToVisit, R.drawable.ic_season)

        // A section with no rows is a section that should not be on screen.
        binding.recordSection.visibility =
            if (record.childCount == 0) View.GONE else View.VISIBLE
        binding.visitSection.visibility =
            if (visiting.childCount == 0) View.GONE else View.VISIBLE

        dropTrailingDivider(record)
        dropTrailingDivider(visiting)
    }

    private fun addRecordRow(
        container: LinearLayout,
        label: String,
        value: Any?,
        @DrawableRes icon: Int? = null
    ) {
        val text = formatDetailValue(value)
        if (text.isEmpty() || text.equals("N/A", ignoreCase = true)) return

        val row = ItemRecordRowBinding.inflate(layoutInflater, container, false)
        row.tvRecordLabel.text = label
        row.tvRecordValue.text = text
        if (icon != null) {
            row.ivRecordIcon.setImageResource(icon)
            row.ivRecordIcon.visibility = View.VISIBLE
        }
        container.addView(row.root)
    }

    /**
     * Firebase hands back lists and maps as well as strings. Rendering those
     * with toString() prints Kotlin's brackets — "[Amorite, Aramean]" — so
     * collections are joined as prose instead.
     */
    private fun formatDetailValue(raw: Any?): String = when (raw) {
        null -> ""

        is Collection<*> -> raw
            .mapNotNull { it?.toString()?.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(", ")

        is Map<*, *> -> raw.values
            .mapNotNull { it?.toString()?.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(", ")

        // 1980.0 is a year, not a measurement.
        is Double -> if (raw % 1.0 == 0.0) raw.toLong().toString() else raw.toString()

        is Boolean -> getString(if (raw) R.string.value_yes else R.string.value_no)

        else -> raw.toString().trim()
    }

    private fun dropTrailingDivider(container: LinearLayout) {
        if (container.childCount == 0) return
        container.getChildAt(container.childCount - 1)
            .findViewById<View>(R.id.recordDivider)
            ?.visibility = View.GONE
    }

    /** "yearBuilt" -> "Year built". */
    private fun humaniseKey(key: String): String {
        val spaced = key.replace(Regex("([a-z])([A-Z])"), "$1 $2")
            .replace('_', ' ')
            .trim()
        return spaced.replaceFirstChar { it.uppercase() }
    }

    /**
     * The real tags on the landmark. The screen previously printed a fixed
     * "#medieval #fortress #mamluk #ayyubid #euphrates" string on every entry
     * while these were never rendered at all.
     */
    private fun renderTags(landmark: Landmark) {
        val group = binding.tagGroup
        group.removeAllViews()

        val tags = if (isEnglish) landmark.tags else landmark.tagsArabic
        val usable = tags.map { it.trim() }.filter { it.isNotEmpty() }

        if (usable.isEmpty()) {
            group.visibility = View.GONE
            return
        }

        group.visibility = View.VISIBLE
        val inflater = LayoutInflater.from(this)
        usable.forEach { tag ->
            val chip = inflater.inflate(R.layout.item_tag_chip, group, false) as Chip
            chip.text = tag
            group.addView(chip)
        }
    }

    // ------------------------------------------------------------ states

    private fun showState(state: ScreenState) {
        val content = state == ScreenState.CONTENT
        val loading = state == ScreenState.LOADING
        val panel = state == ScreenState.MISSING || state == ScreenState.ERROR

        binding.heroSkeleton.visibility = if (loading) View.VISIBLE else View.GONE
        binding.heroLabel.visibility = if (content) View.VISIBLE else View.INVISIBLE
        binding.contentGroup.visibility = if (content) View.VISIBLE else View.INVISIBLE
        binding.stateView.visibility = if (panel) View.VISIBLE else View.GONE
        if (!content) binding.btnGetDirections.visibility = View.GONE

        // Nothing to share or save until there is a landmark on screen.
        val actionable = if (content) View.VISIBLE else View.INVISIBLE
        binding.btnShare.visibility = actionable
        binding.btnBookmark.visibility = actionable

        when (state) {
            ScreenState.MISSING -> {
                binding.tvStateTitle.setText(R.string.detail_missing_title)
                binding.tvStateBody.setText(R.string.detail_missing_body)
                binding.btnRetry.visibility = View.GONE
            }

            ScreenState.ERROR -> {
                binding.tvStateTitle.setText(R.string.detail_error_title)
                binding.tvStateBody.setText(R.string.detail_error_body)
                binding.btnRetry.visibility = View.VISIBLE
            }

            else -> Unit
        }

        if (loading) startPulse() else stopPulse()
    }

    private fun startPulse() {
        if (pulseAnimator != null || !ValueAnimator.areAnimatorsEnabled()) return
        pulseAnimator = ValueAnimator.ofFloat(1f, 0.55f).apply {
            duration = 900
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { binding.heroSkeleton.alpha = it.animatedValue as Float }
            start()
        }
    }

    private fun stopPulse() {
        pulseAnimator?.cancel()
        pulseAnimator = null
        binding.heroSkeleton.alpha = 1f
    }

    // ------------------------------------------------------------ actions

    private fun openGoogleMaps(latitude: Double, longitude: Double) {
        try {
            val gmmIntentUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude")

            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            // Was "com.google.maps.android" — the maps-utils *library* package,
            // which no app declares, so resolveActivity always returned null
            // and this silently fell through to the browser every time.
            mapIntent.setPackage("com.google.android.apps.maps")

            if (mapIntent.resolveActivity(packageManager) != null) {
                startActivity(mapIntent)
            } else {
                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
                )
                startActivity(browserIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to open maps: ${e.message}")
            showMessage(R.string.detail_maps_failed)
        }
    }

    private fun shareLandmark() {
        val landmark = currentLandmark ?: return

        val shareText = buildString {
            append("📍 ${landmark.name}\n")
            append("${landmark.nameArabic}\n\n")
            append("📌 Location: ${joinPlace(landmark.location.city, landmark.location.governorate)}\n")
            // No review count here either — it would carry the invented number
            // out of the app and into someone else's inbox.
            if (landmark.rating > 0.0) {
                append("⭐ Rating: ${String.format("%.1f", landmark.rating)}/5.0\n")
            }
            append("\n${landmark.description}\n\n")

            val mapsUrl = "https://www.google.com/maps/search/?api=1&query=${landmark.location.latitude},${landmark.location.longitude}"
            append("🗺️ View on Map: $mapsUrl\n\n")

            append("Discover more Syrian landmarks with East Syria app")
        }

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "Check out ${landmark.name}")
        }

        try {
            startActivity(Intent.createChooser(shareIntent, getString(R.string.detail_share_chooser)))
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing: ${e.message}")
            showMessage(R.string.detail_share_failed)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
