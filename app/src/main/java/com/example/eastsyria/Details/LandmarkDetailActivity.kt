package com.example.eastsyria.Details

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.eastsyria.R
import com.example.eastsyria.databinding.ActivityLandmarkDetailBinding
import com.example.eastsyria.MainPage.Data.Landmark
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

    companion object {
        private const val TAG = "LandmarkDetailActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars()
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        landmarkId = intent.getStringExtra("LANDMARK_ID") ?: ""
        Log.d(TAG, "onCreate: Set landmarkId to: '$landmarkId'")
        window.statusBarColor = ContextCompat.getColor(this, R.color.background_dark)

        binding = ActivityLandmarkDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val landmarkId = intent.getStringExtra("LANDMARK_ID")

        if (landmarkId.isNullOrEmpty()) {
            Toast.makeText(this, "Error: No landmark selected", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupLanguageToggle()
        setupBookmarkButton()
        loadLandmarkDetails(landmarkId)
    }

    private fun setupToolbar() {
        binding.collapsingToolbar.isTitleEnabled = false

        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            setDisplayShowTitleEnabled(false)
            title = " "
        }

        binding.toolbar.title = " "

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.btnShare.setOnClickListener {
            shareLandmark()
        }
    }

    private fun setupBookmarkButton() {
        binding.btnBookmark.setOnClickListener {
            toggleBookmark()
        }
    }

    private fun checkIfSaved() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            updateBookmarkUI(false)
            return
        }

        if (landmarkId.isEmpty()) {
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
            Toast.makeText(this, "Please log in to save landmarks", Toast.LENGTH_SHORT).show()
            return
        }

        if (landmarkId.isEmpty()) {
            Toast.makeText(this, "Error: Invalid landmark ID", Toast.LENGTH_SHORT).show()
            return
        }

        val savedPath = "users/$userId/savedLandmarks/$landmarkId"

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
                    Toast.makeText(this, "Removed from saved", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to remove: ${e.message}", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(this, "Saved successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }



    private fun updateBookmarkUI(saved: Boolean) {
        if (saved) {
            binding.btnBookmark.setImageResource(R.drawable.ic_bookmark_filled)
            binding.btnBookmark.setColorFilter(
                ContextCompat.getColor(this, R.color.orange_accent)
            )
        } else {
            binding.btnBookmark.setImageResource(R.drawable.ic_bookmark)
            binding.btnBookmark.setColorFilter(
                ContextCompat.getColor(this, R.color.white)
            )
        }
    }



    private fun setupLanguageToggle() {
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

    private fun updateLanguageUI() {
        if (isEnglish) {
            binding.btnEnglish.apply {
                setBackgroundColor(ContextCompat.getColor(context, R.color.white))
                setTextColor(ContextCompat.getColor(context, R.color.background_dark))
            }
            binding.btnArabic.apply {
                setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            }
        } else {
            binding.btnArabic.apply {
                setBackgroundColor(ContextCompat.getColor(context, R.color.white))
                setTextColor(ContextCompat.getColor(context, R.color.background_dark))
            }
            binding.btnEnglish.apply {
                setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            }
        }
    }

    private fun loadLandmarkDetails(landmarkId: String) {
        showLoading(true)

        val landmarkRef = database.reference.child("landmarks").child(landmarkId)
        landmarkRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                if (snapshot.exists()) {
                    currentLandmark = snapshot.getValue(Landmark::class.java)
                    currentLandmark?.let {
                        displayLandmarkData()
                        showLoading(false)
                        checkIfSaved()
                    } ?: run {
                        showError("Failed to load landmark details")
                    }
                } else {
                    showError("Landmark not found")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Database error: ${error.message}")
                showError("Failed to load landmark: ${error.message}")
            }
        })
    }


    private fun displayLandmarkData() {
        val landmark = currentLandmark ?: return
        binding.ivHeroImage.setBackgroundColor(Color.parseColor("#FF6B35"))

        Glide.with(this)
            .load(landmark.imageUrl)
            .placeholder(R.drawable.placeholder_destination)
            .error(R.drawable.placeholder_destination)
            .centerCrop()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(binding.ivHeroImage)



        binding.tvCategory.text = if (isEnglish) {
            "${landmark.category.uppercase()} SITE"
        } else {
            "موقع ${landmark.categoryArabic}"
        }


        binding.tvLandmarkName.text = if (isEnglish) landmark.name else landmark.nameArabic
        binding.tvArabicName.text = if (isEnglish) landmark.nameArabic else landmark.name


        binding.tvLocation.text = if (isEnglish) {
            "${landmark.location.city}, ${landmark.location.governorate}"
        } else {
            "${landmark.location.cityArabic}, ${landmark.location.governorateArabic}"
        }


        binding.tvRating.text = landmark.rating.toString()
        binding.tvReviewCount.text = "Based on ${formatReviewCount(landmark.reviewCount)} reviews"


        val details = landmark.details


        binding.tvBuiltIn.text = details["yearBuilt"]?.toString() ?: "N/A"


        binding.tvElevation.text = details["elevation"]?.toString() ?: "N/A"


        binding.tvMaterial.text = details["material"]?.toString() ?: "N/A"


        binding.tvDescription.text = if (isEnglish) {
            landmark.longDescription
        } else {
            landmark.longDescriptionArabic
        }


        binding.btnGetDirections.setOnClickListener {
            openGoogleMaps(landmark.location.latitude, landmark.location.longitude)
        }
    }

    private fun formatReviewCount(count: Int): String {
        return when {
            count >= 1000 -> String.format("%.1fk", count / 1000.0)
            else -> count.toString()
        }
    }


    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            // binding.progressBar.visibility = View.VISIBLE
        } else {
            // binding.progressBar.visibility = View.GONE
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        showLoading(false)
        finish()
    }

    private fun openGoogleMaps(latitude: Double, longitude: Double) {
        try {

            val gmmIntentUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude")


            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.maps.android")


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
            Toast.makeText(this, "Unable to open maps", Toast.LENGTH_SHORT).show()
        }
    }
    private fun shareLandmark() {
        val landmark = currentLandmark ?: return

        val shareText = buildString {
            append("📍 ${landmark.name}\n")
            append("${landmark.nameArabic}\n\n")
            append("📌 Location: ${landmark.location.city}, ${landmark.location.governorate}\n")
            append("⭐ Rating: ${landmark.rating}/5.0 (${formatReviewCount(landmark.reviewCount)} reviews)\n\n")
            append("${landmark.description}\n\n")


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
            startActivity(Intent.createChooser(shareIntent, "Share via"))
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to share", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Error sharing: ${e.message}")
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

