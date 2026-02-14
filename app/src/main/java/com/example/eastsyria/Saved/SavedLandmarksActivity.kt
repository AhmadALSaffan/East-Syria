package com.example.eastsyria.Saved

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eastsyria.Details.LandmarkDetailActivity
import com.example.eastsyria.MainPage.Data.Landmark
import com.example.eastsyria.R
import com.example.eastsyria.databinding.ActivitySavedLandmarksBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SavedLandmarksActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySavedLandmarksBinding
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adapter: SavedLandmarkAdapter
    private val savedLandmarks = mutableListOf<Landmark>()

    companion object {
        private const val TAG = "SavedLandmarksActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars()
        window.statusBarColor = ContextCompat.getColor(this, R.color.background_dark)
        binding = ActivitySavedLandmarksBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupSearch()
        loadSavedLandmarks()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = SavedLandmarkAdapter(
            savedLandmarks,
            onItemClick = { landmark ->
                openLandmarkDetail(landmark)
            },
            onBookmarkClick = { landmark ->
                removeBookmark(landmark)
            }
        )

        binding.rvSavedLandmarks.apply {
            adapter = this@SavedLandmarksActivity.adapter
            layoutManager = LinearLayoutManager(this@SavedLandmarksActivity)
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadSavedLandmarks() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            showEmptyState(true)
            Toast.makeText(this, "Please log in to see saved landmarks", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        // Get saved landmark IDs
        database.reference
            .child("users")
            .child(userId)
            .child("savedLandmarks")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d(TAG, "Saved landmarks count: ${snapshot.childrenCount}")

                    if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                        savedLandmarks.clear()
                        adapter.updateData(savedLandmarks)
                        showEmptyState(true)
                        showLoading(false)
                        return
                    }

                    val landmarkIds = mutableListOf<String>()
                    for (child in snapshot.children) {
                        child.key?.let { landmarkIds.add(it) }
                    }

                    // Load landmark details
                    loadLandmarkDetails(landmarkIds)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error loading saved landmarks: ${error.message}")
                    showLoading(false)
                    Toast.makeText(
                        this@SavedLandmarksActivity,
                        "Failed to load saved landmarks",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun loadLandmarkDetails(landmarkIds: List<String>) {
        savedLandmarks.clear()
        var loadedCount = 0

        for (landmarkId in landmarkIds) {
            database.reference
                .child("landmarks")
                .child(landmarkId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val landmark = snapshot.getValue(Landmark::class.java)
                        landmark?.let {
                            it.id = snapshot.key ?: ""
                            savedLandmarks.add(it)
                        }

                        loadedCount++
                        if (loadedCount == landmarkIds.size) {
                            // All landmarks loaded
                            adapter.updateData(savedLandmarks)
                            showEmptyState(savedLandmarks.isEmpty())
                            showLoading(false)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Error loading landmark $landmarkId: ${error.message}")
                        loadedCount++
                        if (loadedCount == landmarkIds.size) {
                            adapter.updateData(savedLandmarks)
                            showEmptyState(savedLandmarks.isEmpty())
                            showLoading(false)
                        }
                    }
                })
        }
    }

    private fun openLandmarkDetail(landmark: Landmark) {
        val intent = Intent(this, LandmarkDetailActivity::class.java)
        intent.putExtra("LANDMARK_ID", landmark.id)
        startActivity(intent)
    }

    private fun removeBookmark(landmark: Landmark) {
        val userId = auth.currentUser?.uid ?: return

        database.reference
            .child("users")
            .child(userId)
            .child("savedLandmarks")
            .child(landmark.id)
            .removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Removed from saved", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to remove: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.rvSavedLandmarks.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    private fun showEmptyState(isEmpty: Boolean) {
        binding.emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvSavedLandmarks.visibility = if (isEmpty) View.GONE else View.VISIBLE
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