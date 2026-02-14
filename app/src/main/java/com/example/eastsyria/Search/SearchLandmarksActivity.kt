package com.example.eastsyria.Search

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eastsyria.Details.LandmarkDetailActivity
import com.example.eastsyria.MainPage.Data.Landmark
import com.example.eastsyria.R
import com.example.eastsyria.databinding.ActivitySearchLandmarksBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SearchLandmarksActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchLandmarksBinding
    private val database = FirebaseDatabase.getInstance()
    private lateinit var adapter: SearchResultAdapter
    private val allLandmarks = mutableListOf<Landmark>()
    private var currentCategory = "All"

    companion object {
        private const val TAG = "SearchLandmarksActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars()
        window.statusBarColor = ContextCompat.getColor(this, R.color.background_dark)
        binding = ActivitySearchLandmarksBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        setupRecyclerView()
        setupSearch()
        setupFilters()
        loadAllLandmarks()

        // Auto-focus search field
        binding.etSearch.requestFocus()
    }

    private fun setupViews() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnClear.setOnClickListener {
            binding.etSearch.text.clear()
        }
    }

    private fun setupRecyclerView() {
        adapter = SearchResultAdapter(allLandmarks) { landmark ->
            openLandmarkDetail(landmark)
        }

        binding.rvSearchResults.apply {
            adapter = this@SearchLandmarksActivity.adapter
            layoutManager = LinearLayoutManager(this@SearchLandmarksActivity)
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                binding.btnClear.visibility = if (query.isEmpty()) View.GONE else View.VISIBLE
                performSearch(query)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(binding.etSearch.text.toString())
                true
            } else {
                false
            }
        }
    }

    private fun setupFilters() {
        binding.chipAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentCategory = "All"
                performSearch(binding.etSearch.text.toString())
            }
        }

        binding.chipHistorical.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentCategory = "Historical"
                performSearch(binding.etSearch.text.toString())
            }
        }

        binding.chipArchaeological.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentCategory = "Archaeological"
                performSearch(binding.etSearch.text.toString())
            }
        }

        binding.chipNature.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentCategory = "Nature"
                performSearch(binding.etSearch.text.toString())
            }
        }
    }

    private fun loadAllLandmarks() {
        showLoading(true)

        database.reference.child("landmarks")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    allLandmarks.clear()

                    for (landmarkSnapshot in snapshot.children) {
                        val landmark = landmarkSnapshot.getValue(Landmark::class.java)
                        landmark?.let {
                            it.id = landmarkSnapshot.key ?: ""
                            allLandmarks.add(it)
                        }
                    }

                    adapter.updateData(allLandmarks)
                    updateResultsCount()
                    showLoading(false)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error loading landmarks: ${error.message}")
                    showLoading(false)
                }
            })
    }

    private fun performSearch(query: String) {
        adapter.filter(query, currentCategory)
        updateResultsCount()

        val isEmpty = adapter.getFilteredCount() == 0
        showEmptyState(isEmpty)
    }

    private fun updateResultsCount() {
        val count = adapter.getFilteredCount()
        binding.tvResultsCount.text = when {
            count == 0 -> "No results"
            count == 1 -> "1 landmark found"
            else -> "$count landmarks found"
        }
    }

    private fun openLandmarkDetail(landmark: Landmark) {
        val intent = Intent(this, LandmarkDetailActivity::class.java)
        intent.putExtra("LANDMARK_ID", landmark.id)
        startActivity(intent)
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.rvSearchResults.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    private fun showEmptyState(isEmpty: Boolean) {
        binding.emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvSearchResults.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.apply {
            // Hide navigation bar
            hide(WindowInsetsCompat.Type.navigationBars())

            // Set behavior so it won't show when swiping
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}