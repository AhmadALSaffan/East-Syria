package com.example.eastsyria.CategoryList

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eastsyria.Details.LandmarkDetailActivity
import com.example.eastsyria.Login.LoginActivity
import com.example.eastsyria.R
import com.example.eastsyria.Ui.SkeletonPulse
import com.example.eastsyria.Ui.padBottomForInsets
import com.example.eastsyria.Ui.warmEdgeToEdge
import com.example.eastsyria.databinding.ActivityCategoryListBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class CategoryListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoryListBinding
    private lateinit var adapter: CategoryItemAdapter
    private lateinit var database: DatabaseReference
    private lateinit var pulse: SkeletonPulse

    private val allItems = mutableListOf<CategoryItem>()
    private val filteredItems = mutableListOf<CategoryItem>()
    private val filterOptions = FilterOptions()
    private var categoryName = ""
    private var categoryType = ""
    private var loaded = false

    private var savedRef: DatabaseReference? = null
    private var savedListener: ValueEventListener? = null

    private enum class State { LOADING, LIST, EMPTY, EMPTY_FILTERED, ERROR }

    companion object {
        const val EXTRA_CATEGORY_NAME = "CATEGORY_NAME"
        const val EXTRA_CATEGORY_TYPE = "CATEGORY_TYPE"
        private const val REQUEST_CODE_SEARCH = 100
        private const val TAG = "CategoryListActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        warmEdgeToEdge()
        binding = ActivityCategoryListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        pulse = SkeletonPulse(binding.loadingState)

        database = FirebaseDatabase.getInstance().reference

        categoryName = intent.getStringExtra(EXTRA_CATEGORY_NAME) ?: getString(R.string.categories_title)
        categoryType = intent.getStringExtra(EXTRA_CATEGORY_TYPE) ?: "historical_sites"

        setupHeader()
        setupRecyclerView()
        binding.btnResetFilters.setOnClickListener { resetFilters() }
        loadItemsFromFirebase()
        listenForSavedIds()
    }

    override fun onDestroy() {
        pulse.stop()
        val ref = savedRef
        val listener = savedListener
        if (ref != null && listener != null) ref.removeEventListener(listener)
        super.onDestroy()
    }

    private fun setupHeader() {
        binding.header.setTitle(categoryName)
        binding.header.onBack { finish() }
        binding.header.setIconA(R.drawable.ic_search, getString(R.string.home_search_hint)) {
            showSearchDialog()
        }
        binding.header.setIconB(R.drawable.ic_filter_list, getString(R.string.category_filter)) {
            showFilterDialog()
        }
    }

    private fun setupRecyclerView() {
        adapter = CategoryItemAdapter(
            items = filteredItems,
            onItemClick = { item -> navigateToDetails(item) },
            onBookmarkClick = { item -> toggleBookmark(item) }
        )

        binding.recyclerViewItems.apply {
            layoutManager = LinearLayoutManager(this@CategoryListActivity)
            adapter = this@CategoryListActivity.adapter
            padBottomForInsets(extraDp = 16)
        }
    }

    // ------------------------------------------------------------ filters

    private fun showFilterDialog() {
        val filterDialog = FilterBottomSheet(filterOptions) { newFilter ->
            filterOptions.minRating = newFilter.minRating
            filterOptions.statusFilters.clear()
            filterOptions.statusFilters.addAll(newFilter.statusFilters)
            filterOptions.isFeatured = newFilter.isFeatured
            filterOptions.isTrending = newFilter.isTrending

            applyFilters()
        }
        filterDialog.show(supportFragmentManager, "FilterDialog")
    }

    private fun showSearchDialog() {
        val intent = Intent(this, SearchActivity::class.java).apply {
            putParcelableArrayListExtra(
                SearchActivity.EXTRA_ITEMS,
                ArrayList(allItems) as ArrayList<out Parcelable>
            )
        }
        @Suppress("DEPRECATION")
        startActivityForResult(intent, REQUEST_CODE_SEARCH)
    }

    private fun resetFilters() {
        filterOptions.reset()
        applyFilters()
    }

    private fun applyFilters() {
        filteredItems.clear()
        filteredItems.addAll(allItems.filter { item ->
            var passes = true
            if (filterOptions.minRating > 0.0) passes = passes && item.rating >= filterOptions.minRating
            if (filterOptions.statusFilters.isNotEmpty()) {
                passes = passes && filterOptions.statusFilters.contains(item.status)
            }
            if (filterOptions.isFeatured == true) passes = passes && item.isFeatured
            if (filterOptions.isTrending == true) passes = passes && item.isTrending
            passes
        })
        adapter.updateItems(filteredItems)
        showResults()
    }

    // ------------------------------------------------------------ data

    private fun loadItemsFromFirebase() {
        loaded = false
        showState(State.LOADING)

        database.child("landmarks")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    allItems.clear()

                    for (landmarkSnapshot in snapshot.children) {
                        try {
                            val item = landmarkSnapshot.getValue(CategoryItem::class.java) ?: continue
                            val withId = if (item.id.isEmpty()) item.copy(id = landmarkSnapshot.key.orEmpty()) else item
                            if (withId.status == "published" &&
                                CategoryRules.matches(
                                    categoryType, withId.category, withId.tags, withId.name, withId.isHistorical
                                )
                            ) {
                                allItems.add(withId)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing landmark ${landmarkSnapshot.key}: ${e.message}")
                        }
                    }

                    allItems.sortByDescending { it.rating }
                    loaded = true
                    applyFilters()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error loading data: ${error.message}")
                    showState(State.ERROR)
                }
            })
    }

    /** Which places are saved, read once and kept live, so no row asks the network. */
    private fun listenForSavedIds() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = database.child("users").child(userId).child("savedLandmarks")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                adapter.setSavedIds(snapshot.children.mapNotNull { it.key }.toSet())
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error loading saved ids: ${error.message}")
            }
        }
        savedRef = ref
        savedListener = listener
        ref.addValueEventListener(listener)
    }

    private fun toggleBookmark(item: CategoryItem) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            snack(R.string.detail_login_to_save, R.string.saved_signed_out_action) {
                startActivity(Intent(this, LoginActivity::class.java))
            }
            return
        }
        if (item.id.isEmpty()) return

        val ref = database.child("users").child(userId).child("savedLandmarks").child(item.id)
        // The live listener redraws the row once the write lands.
        ref.get().addOnSuccessListener { current ->
            val request = if (current.exists()) {
                ref.removeValue()
            } else {
                ref.setValue(
                    mapOf(
                        "savedAt" to System.currentTimeMillis(),
                        "landmarkId" to item.id,
                        "name" to item.name,
                        "imageUrl" to item.imageUrl,
                        "category" to item.category
                    )
                )
            }
            request.addOnFailureListener { e ->
                Log.e(TAG, "Failed to update saved list: ${e.message}")
                snack(R.string.detail_save_failed)
            }
        }.addOnFailureListener { snack(R.string.detail_save_failed) }
    }

    private fun navigateToDetails(landmark: CategoryItem) {
        val intent = Intent(this, LandmarkDetailActivity::class.java)
        intent.putExtra("LANDMARK_ID", landmark.id)
        startActivity(intent)
    }

    // ------------------------------------------------------------ states

    private fun showResults() {
        when {
            filteredItems.isNotEmpty() -> showState(State.LIST)
            allItems.isEmpty() -> showState(State.EMPTY)
            else -> showState(State.EMPTY_FILTERED)
        }
    }

    private fun showState(state: State) {
        val list = state == State.LIST
        binding.loadingState.visibility = if (state == State.LOADING) View.VISIBLE else View.GONE
        binding.recyclerViewItems.visibility = if (list) View.VISIBLE else View.GONE
        binding.statePanel.hide()

        val filtering = filterOptions.isActive()
        binding.btnResetFilters.visibility =
            if (filtering && (list || state == State.EMPTY_FILTERED)) View.VISIBLE else View.GONE
        binding.header.iconB.imageTintList = android.content.res.ColorStateList.valueOf(
            ContextCompat.getColor(this, if (filtering) R.color.terracotta_soft else R.color.ivory)
        )

        binding.tvCount.text = when {
            !loaded -> ""
            list -> resources.getQuantityString(
                R.plurals.categories_place_count, filteredItems.size, filteredItems.size
            )

            else -> ""
        }

        when (state) {
            State.EMPTY -> binding.statePanel.show(
                getString(R.string.category_empty_title),
                getString(R.string.hero_empty_body)
            )

            State.EMPTY_FILTERED -> binding.statePanel.show(
                getString(R.string.search_empty_title),
                getString(R.string.category_filtered_empty_body),
                getString(R.string.category_reset_filters)
            ) { resetFilters() }

            State.ERROR -> binding.statePanel.show(
                getString(R.string.search_error_title),
                getString(R.string.search_error_body),
                getString(R.string.action_retry)
            ) { loadItemsFromFirebase() }

            else -> Unit
        }

        if (state == State.LOADING) pulse.start() else pulse.stop()
    }

    private fun snack(message: Int, action: Int? = null, onAction: (() -> Unit)? = null) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).apply {
            setBackgroundTint(ContextCompat.getColor(context, R.color.ivory))
            setTextColor(ContextCompat.getColor(context, R.color.canvas))
            setActionTextColor(ContextCompat.getColor(context, R.color.canvas))
            if (action != null && onAction != null) setAction(action) { onAction() }
            show()
        }
    }
}
