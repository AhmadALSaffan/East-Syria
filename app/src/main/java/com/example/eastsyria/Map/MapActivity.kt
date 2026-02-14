package com.example.eastsyria.Map

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.bumptech.glide.Glide
import com.example.eastsyria.Details.LandmarkDetailActivity
import com.example.eastsyria.MainPage.Data.Landmark
import com.example.eastsyria.R
import com.example.eastsyria.Saved.SavedLandmarksActivity
import com.example.eastsyria.databinding.ActivityMapBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapBinding
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val database = FirebaseDatabase.getInstance()
    private val landmarks = mutableListOf<Landmark>()
    private val markers = mutableListOf<Marker>()
    private var selectedLandmark: Landmark? = null
    private var userLocation: Location? = null

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val TAG = "MapActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars()
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = ContextCompat.getColor(this, R.color.background_dark)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupMap()
        setupClickListeners()
        setupSearch()
        setupFilters()
    }

    private fun setupMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        applyDarkMapStyle()

        try {
            googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style)
            )
        } catch (e: Exception) {
        }
        val eastSyriaCenter = LatLng(35.8, 40.0)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(eastSyriaCenter, 7.5f))


        googleMap.uiSettings.isZoomControlsEnabled = false
        googleMap.uiSettings.isMapToolbarEnabled = false

        googleMap.setOnMarkerClickListener { marker ->
            val landmark = marker.tag as? Landmark
            landmark?.let {
                showLandmarkCard(it)
                googleMap.animateCamera(CameraUpdateFactory.newLatLng(marker.position))
            }
            true
        }


        googleMap.setOnMapClickListener {
            hideLandmarkCard()
        }


        enableMyLocation()

        loadLandmarks()
    }

    private fun enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
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

    private fun getUserLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    userLocation = it
                }
            }
        }
    }

    private fun loadLandmarks() {
        database.reference.child("landmarks")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    landmarks.clear()
                    clearMarkers()

                    for (landmarkSnapshot in snapshot.children) {
                        val landmark = landmarkSnapshot.getValue(Landmark::class.java)
                        landmark?.let {
                            it.id = landmarkSnapshot.key ?: ""
                            landmarks.add(it)
                            addMarkerForLandmark(it)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@MapActivity,
                        "Failed to load landmarks",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun addMarkerForLandmark(landmark: Landmark) {
        if (landmark.location.latitude != 0.0 && landmark.location.longitude != 0.0) {
            val position = LatLng(landmark.location.latitude, landmark.location.longitude)
            val marker = googleMap.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(landmark.name)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
            )
            marker?.tag = landmark
            marker?.let { markers.add(it) }
        }
    }

    private fun clearMarkers() {
        markers.forEach { it.remove() }
        markers.clear()
    }

    private fun showLandmarkCard(landmark: Landmark) {
        selectedLandmark = landmark
        binding.landmarkBottomCard.visibility = View.VISIBLE

        binding.tvLandmarkTitle.text = landmark.name
        binding.tvLandmarkDescription.text = landmark.description


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
            binding.tvDistance.text = String.format("%.1f km away", distanceKm)
        } ?: run {
            binding.tvDistance.text = "Distance unknown"
        }


        Glide.with(this)
            .load(landmark.imageUrl)
            .placeholder(R.drawable.placeholder_destination)
            .into(binding.ivLandmarkSmall)


        binding.landmarkBottomCard.setOnClickListener {
            val intent = Intent(this, LandmarkDetailActivity::class.java)
            intent.putExtra("LANDMARK_ID", landmark.id)
            startActivity(intent)
        }
    }

    private fun hideLandmarkCard() {
        binding.landmarkBottomCard.visibility = View.GONE
        selectedLandmark = null
    }

    private fun setupClickListeners() {
        binding.fabMyLocation.setOnClickListener {
            getUserLocation()
            userLocation?.let {
                val position = LatLng(it.latitude, it.longitude)
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 12f))
            } ?: run {
                Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnFilter.setOnClickListener {
            val filterDialog = MapFiltersBottomSheet { filterOptions ->
                applyFilters(filterOptions)
            }
            filterDialog.show(supportFragmentManager, MapFiltersBottomSheet.TAG)
        }

        binding.fabZoomIn.setOnClickListener {
            googleMap.animateCamera(CameraUpdateFactory.zoomIn())
        }

        binding.fabZoomOut.setOnClickListener {
            googleMap.animateCamera(CameraUpdateFactory.zoomOut())
        }

        binding.fabLayers.setOnClickListener {
            when (googleMap.mapType) {
                GoogleMap.MAP_TYPE_NORMAL -> {
                    googleMap.setMapStyle(null)
                    googleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
                    Toast.makeText(this, "Satellite view", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
                    applyDarkMapStyle()
                    Toast.makeText(this, "Map view", Toast.LENGTH_SHORT).show()
                }
            }
        }



        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_explore -> {
                    finish()
                    true
                }
                R.id.nav_saved-> {
                    val intent = Intent(this@MapActivity, SavedLandmarksActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }

    private fun applyDarkMapStyle() {
        try {
            Log.d(TAG, "Attempting to apply dark map style...")
            val success = googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style)
            )
            if (success) {
                Log.d(TAG, "✅ Dark map style applied successfully!")
            } else {
                Log.e(TAG, "❌ Style parsing failed - JSON might be invalid")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "❌ Can't find map_style.json in res/raw/", e)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error applying style: ", e)
        }
    }


    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterLandmarks(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupFilters() {
        binding.chipAncientCities.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) filterByCategory("Historical")
        }

        binding.chipFortresses.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) filterByCategory("Archaeological")
        }

        binding.chipRomanEra.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) filterByCategory("Nature")
        }
    }

    private fun applyFilters(filterOptions: FilterOptions) {
        clearMarkers()
        var filteredLandmarks: List<Landmark> = landmarks

        if (filterOptions.categories.isNotEmpty()) {
            filteredLandmarks = filteredLandmarks.filter { landmark ->
                filterOptions.categories.contains(landmark.category)
            }
        }

        filterOptions.maxDistance?.let { maxDist ->
            userLocation?.let { userLoc ->
                filteredLandmarks = filteredLandmarks.filter { landmark ->
                    val distance = FloatArray(1)
                    android.location.Location.distanceBetween(
                        userLoc.latitude,
                        userLoc.longitude,
                        landmark.location.latitude,
                        landmark.location.longitude,
                        distance
                    )
                    (distance[0] / 1000) <= maxDist
                }
            }
        }

        if (filterOptions.minRating > 0) {
            filteredLandmarks = filteredLandmarks.filter { it.rating >= filterOptions.minRating }
        }

        if (filterOptions.featuredOnly) {
            filteredLandmarks = filteredLandmarks.filter { it.isFeatured }
        }

        if (filterOptions.trendingOnly) {
            filteredLandmarks = filteredLandmarks.filter { it.isTrending }
        }

        filteredLandmarks.forEach { addMarkerForLandmark(it) }

        Toast.makeText(
            this,
            "${filteredLandmarks.size} landmarks found",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun filterLandmarks(query: String) {
        clearMarkers()
        landmarks.filter {
            it.name.contains(query, ignoreCase = true) ||
                    it.location.city.contains(query, ignoreCase = true)
        }.forEach { addMarkerForLandmark(it) }
    }

    private fun filterByCategory(category: String) {
        clearMarkers()
        landmarks.filter { it.category.equals(category, ignoreCase = true) }
            .forEach { addMarkerForLandmark(it) }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation()
            }
        }
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.apply {
            hide(WindowInsetsCompat.Type.navigationBars())

            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}