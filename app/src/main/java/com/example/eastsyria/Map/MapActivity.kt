package com.example.eastsyria.Map

import com.example.eastsyria.Ui.isNightMode
import com.example.eastsyria.Search.categoryKey
import com.example.eastsyria.Ui.localCategory
import com.example.eastsyria.Ui.localDescription
import com.example.eastsyria.Ui.localName
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.bumptech.glide.Glide
import com.example.eastsyria.Details.LandmarkDetailActivity
import com.example.eastsyria.MainPage.Data.Landmark
import com.example.eastsyria.R
import com.example.eastsyria.Ui.warmEdgeToEdge
import com.example.eastsyria.databinding.ActivityMapBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Locale

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapBinding
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val database = FirebaseDatabase.getInstance()
    private val landmarks = mutableListOf<Landmark>()
    private val markers = mutableListOf<Marker>()
    private var selectedLandmark: Landmark? = null
    private var userLocation: Location? = null
    private var activeCategory: String? = null
    private var sheetFilters = FilterOptions()
    private var bottomInset = 0

    private var landmarksRef: DatabaseReference? = null
    private var landmarksListener: ValueEventListener? = null

    private val pinIcon: BitmapDescriptor by lazy { bitmapFromVector(R.drawable.ic_map_pin_v3) }

    /** Set when the AI guide opens the map to show suggested stops. */
    private class ItineraryView(val ids: List<String>, val anchor: LatLng?, val anchorName: String?, val radiusKm: Double?)
    private var itinerary: ItineraryView? = null

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val TAG = "MapActivity"

        /** Suggested stops, in visiting order. */
        const val EXTRA_ITINERARY_IDS = "ITINERARY_IDS"
        const val EXTRA_ANCHOR_LAT = "ITINERARY_ANCHOR_LAT"
        const val EXTRA_ANCHOR_LNG = "ITINERARY_ANCHOR_LNG"
        const val EXTRA_ANCHOR_NAME = "ITINERARY_ANCHOR_NAME"
        const val EXTRA_RADIUS_KM = "ITINERARY_RADIUS_KM"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        warmEdgeToEdge()

        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        intent.getStringArrayListExtra(EXTRA_ITINERARY_IDS)?.let { ids ->
            itinerary = ItineraryView(
                ids = ids,
                anchor = if (intent.hasExtra(EXTRA_ANCHOR_LAT)) LatLng(intent.getDoubleExtra(EXTRA_ANCHOR_LAT, 0.0), intent.getDoubleExtra(EXTRA_ANCHOR_LNG, 0.0)) else null,
                anchorName = intent.getStringExtra(EXTRA_ANCHOR_NAME),
                radiusKm = if (intent.hasExtra(EXTRA_RADIUS_KM)) intent.getDoubleExtra(EXTRA_RADIUS_KM, 0.0) else null,
            )
            // The stops are the whole view: no search or kinds of place to narrow them.
            binding.searchPill.visibility = View.GONE
            binding.filterScrollView.visibility = View.GONE
        }

        setupInsets()
        setupMap()
        setupClickListeners()
        setupSearch()
    }

    override fun onDestroy() {
        val ref = landmarksRef
        val listener = landmarksListener
        if (ref != null && listener != null) ref.removeEventListener(listener)
        super.onDestroy()
    }

    /** Instruments sit below the status bar; the card and controls clear the gesture bar. */
    private fun setupInsets() {
        val topGap = (10 * resources.displayMetrics.density).toInt()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            bottomInset = bars.bottom
            binding.topArea.updatePadding(top = topGap + bars.top)
            updateBottomMargins()
            insets
        }
    }

    /** With the card up the controls ride on it; without it they clear the gesture bar. */
    private fun updateBottomMargins() {
        val gap = (16 * resources.displayMetrics.density).toInt()
        binding.landmarkBottomCard.updateLayoutParams<ConstraintLayout.LayoutParams> {
            bottomMargin = gap + bottomInset
        }
        val cardShowing = binding.landmarkBottomCard.visibility == View.VISIBLE
        binding.mapControls.updateLayoutParams<ConstraintLayout.LayoutParams> {
            bottomMargin = if (cardShowing) gap else gap + bottomInset
        }
    }

    private fun setupMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        applyDarkMapStyle()

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(35.8, 40.0), 7.5f))

        googleMap.uiSettings.isZoomControlsEnabled = false
        googleMap.uiSettings.isMapToolbarEnabled = false
        // The SDK draws its own my-location button top-right, which duplicated
        // the teal one in the control column and sat over the search field.
        googleMap.uiSettings.isMyLocationButtonEnabled = false

        googleMap.setOnMarkerClickListener { marker ->
            (marker.tag as? Landmark)?.let {
                showLandmarkCard(it)
                googleMap.animateCamera(CameraUpdateFactory.newLatLng(marker.position))
            }
            true
        }
        googleMap.setOnMapClickListener { hideLandmarkCard() }

        enableMyLocation()
        loadLandmarks()
    }

    // ------------------------------------------------------------ location

    private fun hasLocationPermission() = ActivityCompat.checkSelfPermission(
        this, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    private fun enableMyLocation() {
        if (hasLocationPermission()) {
            googleMap.isMyLocationEnabled = true
            getUserLocation()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun getUserLocation(onFix: ((Location) -> Unit)? = null) {
        if (!hasLocationPermission()) return
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                userLocation = it
                onFix?.invoke(it)
            } ?: run { if (onFix != null) snack(R.string.map_location_unavailable) }
        }
    }

    // ------------------------------------------------------------ data

    private fun loadLandmarks() {
        setLoading(true)

        val ref = database.reference.child("landmarks")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                landmarks.clear()

                for (landmarkSnapshot in snapshot.children) {
                    val landmark = try {
                        landmarkSnapshot.getValue(Landmark::class.java)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing landmark ${landmarkSnapshot.key}: ${e.message}")
                        null
                    }
                    if (landmark != null && landmark.status == "published") {
                        landmark.id = landmarkSnapshot.key ?: ""
                        landmarks.add(landmark)
                    }
                }

                setLoading(false)
                if (itinerary != null) {
                    showItinerary()
                } else {
                    buildCategoryChips()
                    applyCategoryFilter()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to load landmarks: ${error.message}")
                setLoading(false)
                binding.tvReadout.setText(R.string.map_readout_failed)
                Snackbar.make(binding.root, R.string.map_load_failed, Snackbar.LENGTH_INDEFINITE)
                    .setBackgroundTint(ContextCompat.getColor(this@MapActivity, R.color.ivory))
                    .setTextColor(ContextCompat.getColor(this@MapActivity, R.color.canvas))
                    .setActionTextColor(ContextCompat.getColor(this@MapActivity, R.color.canvas))
                    .setAction(R.string.action_retry) { loadLandmarks() }
                    .show()
            }
        }
        landmarksRef?.let { old -> landmarksListener?.let { old.removeEventListener(it) } }
        landmarksRef = ref
        landmarksListener = listener
        ref.addValueEventListener(listener)
    }

    private fun setLoading(loading: Boolean) {
        binding.mapProgress.visibility = if (loading) View.VISIBLE else View.GONE
        if (loading) binding.tvReadout.setText(R.string.map_readout_loading)
    }

    // ------------------------------------------------------------ markers and card

    private fun addMarkerForLandmark(landmark: Landmark) {
        if (landmark.location.latitude == 0.0 && landmark.location.longitude == 0.0) return
        val marker = googleMap.addMarker(
            MarkerOptions()
                .position(LatLng(landmark.location.latitude, landmark.location.longitude))
                .title(landmark.localName(this))
                .icon(pinIcon)
                .anchor(0.5f, 1f)
        )
        marker?.tag = landmark
        marker?.let { markers.add(it) }
    }

    private fun clearMarkers() {
        markers.forEach { it.remove() }
        markers.clear()
    }

    /** Vector to bitmap, so the pin is drawn in the app's own colours. */
    private fun bitmapFromVector(@DrawableRes id: Int): BitmapDescriptor {
        val drawable = ContextCompat.getDrawable(this, id)!!
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun showLandmarkCard(landmark: Landmark) {
        selectedLandmark = landmark
        binding.landmarkBottomCard.visibility = View.VISIBLE
        updateBottomMargins()

        binding.tvLandmarkTitle.text = landmark.localName(this)
        binding.tvLandmarkDescription.text = landmark.localDescription(this)
        binding.tvLandmarkDescription.visibility =
            if (landmark.description.isBlank()) View.GONE else View.VISIBLE

        userLocation?.let { userLoc ->
            val distance = FloatArray(1)
            Location.distanceBetween(
                userLoc.latitude,
                userLoc.longitude,
                landmark.location.latitude,
                landmark.location.longitude,
                distance
            )
            val distanceKm = distance[0] / 1000
            // A tenth of a kilometre is meaningful at 8 km and noise at 2000.
            val reading = if (distanceKm >= 100f) {
                String.format(Locale.getDefault(), "%,.0f", distanceKm)
            } else {
                String.format(Locale.getDefault(), "%.1f", distanceKm)
            }
            binding.tvDistance.visibility = View.VISIBLE
            binding.tvDistance.text = getString(R.string.map_distance, reading)
        } ?: run {
            // Without a fix there is no distance to state, so state nothing.
            binding.tvDistance.visibility = View.GONE
        }

        Glide.with(this)
            .load(landmark.imageUrl)
            .placeholder(R.drawable.placeholder_dark)
            .error(R.drawable.placeholder_dark)
            .centerCrop()
            .into(binding.ivLandmarkSmall)

        binding.landmarkBottomCard.setOnClickListener {
            startActivity(
                Intent(this, LandmarkDetailActivity::class.java)
                    .putExtra("LANDMARK_ID", landmark.id)
            )
        }
    }

    private fun hideLandmarkCard() {
        binding.landmarkBottomCard.visibility = View.GONE
        selectedLandmark = null
        updateBottomMargins()
    }

    // ------------------------------------------------------------ suggested stops

    /**
     * The AI guide's suggested stops: numbered pins in visiting order joined by dashed straight
     * lines from the start, and the "near" radius drawn as a circle. Straight lines on purpose:
     * the app has no road routing, so it shows no roads or travel times.
     */
    private fun showItinerary() {
        val plan = itinerary ?: return
        clearMarkers()
        googleMap.clear()
        val byId = landmarks.associateBy { it.id }
        val stops = plan.ids.mapNotNull { byId[it] }
        val ink = ContextCompat.getColor(this, R.color.terracotta)
        val density = resources.displayMetrics.density
        val bounds = LatLngBounds.builder()
        var included = false

        plan.anchor?.let { start ->
            plan.radiusKm?.let { r ->
                googleMap.addCircle(
                    CircleOptions().center(start).radius(r * 1000)
                        .strokeColor(ink).strokeWidth(2 * density)
                        .fillColor(android.graphics.Color.argb(18, android.graphics.Color.red(ink), android.graphics.Color.green(ink), android.graphics.Color.blue(ink)))
                )
            }
            googleMap.addMarker(
                MarkerOptions().position(start).anchor(0.5f, 0.5f).zIndex(0f)
                    .title(plan.anchorName?.let { getString(R.string.itinerary_map_start, it) })
                    .icon(numberedPin(null))
            )
            bounds.include(start); included = true
        }

        val path = PolylineOptions().color(ink).width(3 * density).pattern(listOf(Dash(18 * density), Gap(10 * density)))
        plan.anchor?.let { path.add(it) }
        stops.forEachIndexed { i, l ->
            val at = LatLng(l.location.latitude, l.location.longitude)
            path.add(at)
            googleMap.addMarker(
                MarkerOptions().position(at).anchor(0.5f, 0.5f).zIndex(1f + i).title(l.localName(this)).icon(numberedPin(i + 1))
            )?.also { it.tag = l; markers.add(it) }
            bounds.include(at); included = true
        }
        if (stops.isNotEmpty()) googleMap.addPolyline(path)

        binding.tvReadout.text = if (plan.anchorName != null && plan.radiusKm != null) {
            getString(R.string.itinerary_map_readout, String.format(Locale.getDefault(), "%.0f", plan.radiusKm), plan.anchorName)
        } else getString(R.string.itinerary_map_readout_plain)

        if (included) {
            binding.root.post {
                runCatching {
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), (64 * density).toInt()))
                }
            }
        }
    }

    /** A round pin with the stop's number, or a small ring for the starting point. */
    private fun numberedPin(number: Int?): BitmapDescriptor {
        val d = resources.displayMetrics.density
        val size = ((if (number == null) 18 else 30) * d).toInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val ink = ContextCompat.getColor(this, R.color.terracotta)
        val onInk = ContextCompat.getColor(this, R.color.on_accent)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        val r = size / 2f
        paint.color = onInk
        canvas.drawCircle(r, r, r, paint)
        paint.color = ink
        canvas.drawCircle(r, r, r - 2 * d, paint)
        paint.color = onInk
        if (number == null) {
            canvas.drawCircle(r, r, r / 2.5f, paint)
        } else {
            paint.textAlign = android.graphics.Paint.Align.CENTER
            paint.textSize = 14 * d
            paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
            val label = java.text.NumberFormat.getInstance(Locale.getDefault()).format(number)
            canvas.drawText(label, r, r - (paint.descent() + paint.ascent()) / 2, paint)
        }
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    // ------------------------------------------------------------ controls

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.fabMyLocation.setOnClickListener {
            if (!hasLocationPermission()) {
                enableMyLocation()
                return@setOnClickListener
            }
            getUserLocation { location ->
                googleMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 12f)
                )
            }
        }

        binding.btnFilter.setOnClickListener {
            val categories = landmarks.map { it.category.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .sorted()
            MapFiltersBottomSheet(categories, sheetFilters, userLocation != null) { options ->
                applySheetFilters(options)
            }.show(supportFragmentManager, MapFiltersBottomSheet.TAG)
        }

        binding.fabZoomIn.setOnClickListener {
            if (::googleMap.isInitialized) googleMap.animateCamera(CameraUpdateFactory.zoomIn())
        }
        binding.fabZoomOut.setOnClickListener {
            if (::googleMap.isInitialized) googleMap.animateCamera(CameraUpdateFactory.zoomOut())
        }

        binding.fabLayers.setOnClickListener {
            if (!::googleMap.isInitialized) return@setOnClickListener
            if (googleMap.mapType == GoogleMap.MAP_TYPE_NORMAL) {
                googleMap.setMapStyle(null)
                googleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
            } else {
                googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
                applyDarkMapStyle()
            }
        }
    }

    private fun applyDarkMapStyle() {
        try {
            val applied = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, if (isNightMode()) R.raw.map_style else R.raw.map_style_light))
            if (!applied) Log.e(TAG, "Map style parsing failed")
        } catch (e: Exception) {
            Log.e(TAG, "Map style error: ${e.message}", e)
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyCategoryFilter()
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    // ------------------------------------------------------------ filtering

    /**
     * Category chips built from the catalog itself.
     *
     * These were three fixed labels wired to unrelated values — "Ancient
     * Cities" filtered category=="Historical", "Fortresses" filtered
     * "Archaeological", and "Roman Era" filtered "Nature". One was checked at
     * launch and there was no way back to the full set, so the map silently
     * opened showing a subset of the catalog.
     */
    private fun buildCategoryChips() {
        val group = binding.chipGroup
        group.removeAllViews()
        val inflater = LayoutInflater.from(this)

        fun addChip(label: String, category: String?) {
            val chip = inflater.inflate(R.layout.item_map_chip, group, false) as Chip
            chip.text = label
            chip.isChecked = category == activeCategory
            chip.setOnClickListener {
                activeCategory = category
                // The chip rail and the sheet are one filter: picking here clears the sheet.
                sheetFilters = FilterOptions()
                applyCategoryFilter()
            }
            group.addView(chip)
        }

        addChip(getString(R.string.map_all), null)

        // One chip per kind of place ("Archaeological Site" joins "Archaeological"), labelled in
        // the app's language, most common first.
        landmarks.filter { categoryKey(it.category).isNotEmpty() }
            .groupBy { categoryKey(it.category) }
            .entries
            .sortedByDescending { it.value.size }
            .forEach { (key, items) ->
                val label = items.first().localCategory(this).substringBefore(" Site").trim()
                addChip(getString(R.string.map_chip_count, label, items.size), key)
            }
    }

    /** Category, search and the sheet's filters compose; none silently drops another. */
    private fun applyCategoryFilter() {
        if (!::googleMap.isInitialized || itinerary != null) return
        val query = binding.etSearch.text?.toString().orEmpty().trim()

        var shown = landmarks.filter { landmark ->
            val matchesCategory =
                activeCategory == null || categoryKey(landmark.category) == activeCategory
            val matchesQuery = query.isBlank() ||
                    landmark.name.contains(query, ignoreCase = true) ||
                    landmark.nameArabic.contains(query) ||
                    landmark.location.city.contains(query, ignoreCase = true) ||
                    landmark.location.cityArabic.contains(query)
            matchesCategory && matchesQuery
        }

        val filters = sheetFilters
        if (filters.categories.isNotEmpty()) {
            shown = shown.filter { l -> filters.categories.any { it.equals(l.category.trim(), ignoreCase = true) } }
        }
        val here = userLocation
        val maxDistance = filters.maxDistance
        if (maxDistance != null && here != null) {
            shown = shown.filter { landmark ->
                val distance = FloatArray(1)
                Location.distanceBetween(
                    here.latitude, here.longitude,
                    landmark.location.latitude, landmark.location.longitude,
                    distance
                )
                distance[0] / 1000 <= maxDistance
            }
        }
        if (filters.minRating > 0) shown = shown.filter { it.rating >= filters.minRating }
        if (filters.featuredOnly) shown = shown.filter { it.isFeatured }
        if (filters.trendingOnly) shown = shown.filter { it.isTrending }

        clearMarkers()
        shown.forEach { addMarkerForLandmark(it) }
        if (selectedLandmark != null && shown.none { it.id == selectedLandmark?.id }) hideLandmarkCard()

        // The filter icon lights up while the sheet is narrowing the map.
        binding.btnFilter.imageTintList = android.content.res.ColorStateList.valueOf(
            ContextCompat.getColor(
                this,
                if (filters.isActive()) R.color.terracotta else R.color.ivory_dim
            )
        )
        updateReadout(shown.size)
    }

    private fun applySheetFilters(options: FilterOptions) {
        sheetFilters = options
        // The sheet supersedes the chip rail; reset it so the row never claims a
        // category the map is no longer restricted to.
        activeCategory = null
        buildCategoryChips()
        applyCategoryFilter()
    }

    /** A count that vanishes after two seconds is not a reading. */
    private fun updateReadout(shown: Int) {
        binding.tvReadout.text = getString(R.string.map_readout, shown, landmarks.size)
    }

    private fun snack(message: Int) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
            .setBackgroundTint(ContextCompat.getColor(this, R.color.ivory))
            .setTextColor(ContextCompat.getColor(this, R.color.canvas))
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (::googleMap.isInitialized) enableMyLocation()
            } else {
                snack(R.string.map_location_permission)
            }
        }
    }
}
