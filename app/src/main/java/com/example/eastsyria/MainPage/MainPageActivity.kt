package com.example.eastsyria.MainPage

import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eastsyria.Details.LandmarkDetailActivity
import com.example.eastsyria.MainPage.Adapters.FeaturedLandmarkAdapter
import com.example.eastsyria.MainPage.Adapters.TrendingDestinationAdapter
import com.example.eastsyria.MainPage.Data.Landmark
import com.example.eastsyria.Map.MapActivity
import com.example.eastsyria.R
import com.example.eastsyria.Saved.SavedLandmarksActivity
import com.example.eastsyria.Search.SearchLandmarksActivity
import com.example.eastsyria.databinding.ActivityMainPageBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database

class MainPageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainPageBinding
    private val auth: FirebaseAuth by lazy { Firebase.auth }
    private val database: FirebaseDatabase by lazy { Firebase.database }

    private lateinit var featuredAdapter: FeaturedLandmarkAdapter
    private lateinit var trendingAdapter: TrendingDestinationAdapter

    private val featuredLandmarks = mutableListOf<Landmark>()
    private val trendingDestinations = mutableListOf<Landmark>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars()
        enableEdgeToEdge()
        window.statusBarColor = ContextCompat.getColor(this, R.color.background_light)
        binding = ActivityMainPageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = ContextCompat.getColor(this, R.color.background_dark)
        window.navigationBarColor = Color.TRANSPARENT

        setupRecyclerViews()
        setupClickListeners()
        loadLandmarksFromFirebase()
    }


    private fun setupRecyclerViews() {

        featuredAdapter = FeaturedLandmarkAdapter(featuredLandmarks) { landmark ->
            openLandmarkDetail(landmark)
        }
        binding.rvFeaturedLandmarks.apply {
            adapter = featuredAdapter
            layoutManager = LinearLayoutManager(this@MainPageActivity, LinearLayoutManager.HORIZONTAL, false)
        }


        trendingAdapter = TrendingDestinationAdapter(trendingDestinations) { landmark ->
            openLandmarkDetail(landmark)
        }
        binding.rvTrendingDestinations.apply {
            adapter = trendingAdapter
            layoutManager = LinearLayoutManager(this@MainPageActivity)
        }
    }

    private fun openLandmarkDetail(landmark: Landmark) {
        val intent = Intent(this, LandmarkDetailActivity::class.java)
        intent.putExtra("LANDMARK_ID", landmark.id)
        intent.putExtra("LANDMARK_ID", landmark.id)
        startActivity(intent)
    }


    private fun loadLandmarksFromFirebase() {
        showLoading(true)

        val landmarksRef = database.reference.child("landmarks")
        landmarksRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                featuredLandmarks.clear()
                trendingDestinations.clear()

                for (landmarkSnapshot in snapshot.children) {
                    try {
                        val landmark = landmarkSnapshot.getValue(Landmark::class.java)
                        if (landmark != null) {

                            landmark.id = landmarkSnapshot.key ?: ""



                            if (landmark.isFeatured) {
                                featuredLandmarks.add(landmark)
                            }

                            if (landmark.isTrending) {
                                trendingDestinations.add(landmark)
                            }
                        } else {
                            Log.e(TAG, "Landmark is null for key: ${landmarkSnapshot.key}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing landmark ${landmarkSnapshot.key}: ${e.message}", e)
                    }
                }


                featuredAdapter.notifyDataSetChanged()
                trendingAdapter.notifyDataSetChanged()

                showLoading(false)

                if (featuredLandmarks.isEmpty() && trendingDestinations.isEmpty()) {
                    showToast("No landmarks found")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showLoading(false)
                showToast("Failed to load landmarks: ${error.message}")
            }
        })
    }



    private fun setupClickListeners() {
        binding.apply {

            etSearch.setOnClickListener {
                startActivity(Intent(this@MainPageActivity, SearchLandmarksActivity::class.java))
            }


            ivBookmark.setOnClickListener {
                val intentSaved = Intent(this@MainPageActivity, SavedLandmarksActivity::class.java)
                startActivity(intentSaved)
            }


            btnHistorical.setOnClickListener {
                filterByCategory("Historical")
            }

            btnNature.setOnClickListener {
                filterByCategory("Nature")
            }

            btnCulture.setOnClickListener {
                filterByCategory("Culture")
            }

            btnMore.setOnClickListener {
                //showToast("More categories")
            }

            tvSeeAllFeatured.setOnClickListener {
                //showToast("View all featured landmarks")
            }

            tvViewMap.setOnClickListener {
                //showToast("Map view")
            }


            fabAdd.setOnClickListener {
               val mapIntent = Intent(this@MainPageActivity, MapActivity::class.java)
                startActivity(mapIntent)
            }

            bottomNavigation.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_explore -> true
                    R.id.nav_saved -> {
                        val intentSaved = Intent(this@MainPageActivity, SavedLandmarksActivity::class.java)
                        startActivity(intentSaved)
                        false
                    }
                    R.id.nav_updates -> {
                        //showToast("Updates")
                        false
                    }
                    R.id.nav_profile -> {
                        //showToast("Profile")
                        false
                    }
                    else -> false
                }
            }

            bottomNavigation.selectedItemId = R.id.nav_explore
        }
    }

    private fun filterByCategory(category: String) {
        showToast("Filter by: $category")
    }

    private fun onLandmarkClick(landmark: Landmark) {
        showToast("Selected: ${landmark.name}")
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.progressFeatured.visibility = View.VISIBLE
            binding.progressTrending.visibility = View.VISIBLE
            binding.rvFeaturedLandmarks.visibility = View.GONE
            binding.rvTrendingDestinations.visibility = View.GONE
        } else {
            binding.progressFeatured.visibility = View.GONE
            binding.progressTrending.visibility = View.GONE
            binding.rvFeaturedLandmarks.visibility = View.VISIBLE
            binding.rvTrendingDestinations.visibility = View.VISIBLE
        }
    }


    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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