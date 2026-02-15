package com.example.eastsyria.CategoryList

import CategoryItemAdapter
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eastsyria.Details.LandmarkDetailActivity
import com.example.eastsyria.MainPage.Data.Landmark
import com.example.eastsyria.MainPage.MainPageActivity
import com.example.eastsyria.R
import com.example.eastsyria.Saved.SavedLandmarksActivity
import com.example.eastsyria.Search.SearchLandmarksActivity
import com.example.eastsyria.databinding.ActivityCategoryListBinding
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

    private val allItems = mutableListOf<CategoryItem>()
    private val filteredItems = mutableListOf<CategoryItem>()
    private val filterOptions = FilterOptions()
    private var categoryName = ""
    private var categoryType = ""

    companion object {
        const val EXTRA_CATEGORY_NAME = "CATEGORY_NAME"
        const val EXTRA_CATEGORY_TYPE = "CATEGORY_TYPE"
        private const val REQUEST_CODE_SEARCH = 100

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoryListBinding.inflate(layoutInflater)
        setContentView(binding.root)


        database = FirebaseDatabase.getInstance().reference


        categoryName = intent.getStringExtra(EXTRA_CATEGORY_NAME) ?: "Category"
        categoryType = intent.getStringExtra(EXTRA_CATEGORY_TYPE) ?: "historical_sites"

        binding.tvCategoryTitle.text = categoryName

        setupRecyclerView()
        loadItemsFromFirebase()
        setupClickListeners()
        setupBottomNavigation()
    }

    private fun setupRecyclerView() {
        adapter = CategoryItemAdapter(
            items = filteredItems,
            onItemClick = { item ->
                navigateToDetails(item)
            }
        )

        binding.recyclerViewItems.apply {
            layoutManager = LinearLayoutManager(this@CategoryListActivity)
            adapter = this@CategoryListActivity.adapter
            setHasFixedSize(true)
        }
    }


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
        startActivityForResult(intent, REQUEST_CODE_SEARCH)
    }

    private fun applyFilters() {
        filteredItems.clear()

        val results = allItems.filter { item ->
            var passes = true


            if (filterOptions.minRating > 0.0) {
                passes = passes && item.rating >= filterOptions.minRating
            }


            if (filterOptions.statusFilters.isNotEmpty()) {
                passes = passes && filterOptions.statusFilters.contains(item.status)
            }


            if (filterOptions.isFeatured == true) {
                passes = passes && item.isFeatured
            }


            if (filterOptions.isTrending == true) {
                passes = passes && item.isTrending
            }

            passes
        }

        filteredItems.addAll(results)
        adapter.updateItems(filteredItems)

        if (filteredItems.isEmpty()) {
            Toast.makeText(this, "No items match the filters", Toast.LENGTH_SHORT).show()
        }
    }


    private fun loadItemsFromFirebase() {
        showLoading(true)

        database.child("landmarks")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    allItems.clear()

                    for (landmarkSnapshot in snapshot.children) {
                        try {
                            val item = landmarkSnapshot.getValue(CategoryItem::class.java)
                            item?.let {
                                if (shouldIncludeItem(it, categoryType)) {
                                    allItems.add(it)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }


                    allItems.sortByDescending { it.rating }


                    filteredItems.clear()
                    filteredItems.addAll(allItems)
                    adapter.updateItems(filteredItems)

                    showLoading(false)

                    if (allItems.isEmpty()) {
                        Toast.makeText(
                            this@CategoryListActivity,
                            "No items found for this category",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showLoading(false)
                    Toast.makeText(
                        this@CategoryListActivity,
                        "Error loading data: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun shouldIncludeItem(item: CategoryItem, categoryType: String): Boolean {
        return when (categoryType) {
            "historical_sites" -> {
                item.category.equals("Historical", ignoreCase = true) ||
                        item.category.equals("Archaeological", ignoreCase = true) ||
                        item.isHistorical
            }
            "nature_rivers" -> {
                item.tags.any {
                    it.contains("river", ignoreCase = true) ||
                            it.contains("lake", ignoreCase = true) ||
                            it.contains("nature", ignoreCase = true)
                } ||
                        item.name.contains("river", ignoreCase = true) ||
                        item.name.contains("lake", ignoreCase = true)
            }
            "local_culture" -> {
                item.tags.any {
                    it.contains("culture", ignoreCase = true) ||
                            it.contains("traditional", ignoreCase = true) ||
                            it.contains("local", ignoreCase = true)
                }
            }
            "museums" -> {
                item.tags.any {
                    it.contains("museum", ignoreCase = true)
                } ||
                        item.name.contains("museum", ignoreCase = true)
            }
            "cuisine" -> {
                item.tags.any {
                    it.contains("food", ignoreCase = true) ||
                            it.contains("cuisine", ignoreCase = true) ||
                            it.contains("dish", ignoreCase = true)
                }
            }
            "handicrafts" -> {
                item.tags.any {
                    it.contains("craft", ignoreCase = true) ||
                            it.contains("artisan", ignoreCase = true) ||
                            it.contains("traditional art", ignoreCase = true)
                }
            }
            else -> true
        }
    }

    private fun showLoading(isLoading: Boolean) {

    }

    private fun toggleBookmark(item: CategoryItem) {
        item.isBookmarked = !item.isBookmarked


        val userId = getCurrentUserId()
        if (userId.isNotEmpty()) {
            database.child("users")
                .child(userId)
                .child("bookmarks")
                .child(item.id)
                .setValue(item.isBookmarked)
                .addOnSuccessListener {
                    val message = if (item.isBookmarked) {
                        "Added to bookmarks"
                    } else {
                        "Removed from bookmarks"
                    }
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to update bookmark", Toast.LENGTH_SHORT).show()
                }
        }

        adapter.notifyDataSetChanged()
    }

    private fun getCurrentUserId(): String {
         return FirebaseAuth.getInstance().currentUser?.uid ?: ""
    }

    private fun navigateToDetails(landmark: CategoryItem) {
        val intent = Intent(this, LandmarkDetailActivity::class.java)
        intent.putExtra("LANDMARK_ID", landmark.id)
        intent.putExtra("LANDMARK_ID", landmark.id)
        startActivity(intent)
    }



    private fun setupClickListeners() {
        binding.apply {
            btnBack.setOnClickListener {
                finish()
            }

            btnFilter.setOnClickListener {
                showFilterDialog()
            }

            btnSearch.setOnClickListener {
                showSearchDialog()
            }

        }
    }


    private fun setupBottomNavigation() {
        binding.bottomNavigation.selectedItemId = R.id.nav_explore

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_explore -> {
                    val intentSaved = Intent(this@CategoryListActivity, MainPageActivity::class.java)
                    startActivity(intentSaved)
                    false
                }
                R.id.nav_saved -> {
                    val intentSaved = Intent(this@CategoryListActivity, SavedLandmarksActivity::class.java)
                    startActivity(intentSaved)
                    false
                }
                R.id.nav_updates -> {
                    false
                }
                R.id.nav_profile -> {
                    false
                }
                else -> false
            }
        }
    }


}