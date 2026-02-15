package com.example.eastsyria.Categories

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.eastsyria.CategoryList.CategoryListActivity
import com.example.eastsyria.MainPage.MainPageActivity
import com.example.eastsyria.Map.MapActivity
import com.example.eastsyria.R
import com.example.eastsyria.Saved.SavedLandmarksActivity
import com.example.eastsyria.databinding.ActivityCategoriesBinding

class CategoriesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoriesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoriesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        setupBottomNavigation()
    }

    // In CategoriesActivity.kt
    private fun setupClickListeners() {
        binding.apply {
            btnBack.setOnClickListener { finish() }

            cardHistoricalSites.setOnClickListener {
                startCategoryList("Historical Sites", "historical_sites")
            }

            cardNatureRivers.setOnClickListener {
                startCategoryList("Nature & Rivers", "nature_rivers")
            }

            cardLocalCulture.setOnClickListener {
                startCategoryList("Local Culture", "local_culture")
            }

            cardMuseums.setOnClickListener {
                startCategoryList("Museums", "museums")
            }

            cardCuisine.setOnClickListener {
                startCategoryList("Cuisine", "cuisine")
            }

            cardHandicrafts.setOnClickListener {
                startCategoryList("Handicrafts", "handicrafts")
            }

            btnExploreMap.setOnClickListener {
                val mapIntent = Intent(this@CategoriesActivity, MapActivity::class.java)
                startActivity(mapIntent)
            }
            fabMap.setOnClickListener {
                val mapIntent = Intent(this@CategoriesActivity, MapActivity::class.java)
                startActivity(mapIntent)
            }
            btnBookmark.setOnClickListener {
                val intent = Intent(this@CategoriesActivity, SavedLandmarksActivity::class.java)
                startActivity(intent)
            }
        }
    }

    private fun startCategoryList(name: String, type: String) {
        val intent = Intent(this, CategoryListActivity::class.java).apply {
            putExtra(CategoryListActivity.EXTRA_CATEGORY_NAME, name)
            putExtra(CategoryListActivity.EXTRA_CATEGORY_TYPE, type)
        }
        startActivity(intent)
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_explore -> {
                    val intentSaved = Intent(this@CategoriesActivity, MainPageActivity::class.java)
                    startActivity(intentSaved)
                    false
                }

                R.id.nav_saved -> {
                    val intentSaved = Intent(this@CategoriesActivity, SavedLandmarksActivity::class.java)
                    startActivity(intentSaved)
                    false
                }
                R.id.nav_profile -> {
                    //val intentSaved = Intent(this@CategoriesActivity, SavedLandmarksActivity::class.java)
                    //startActivity(intentSaved)
                    false
                }
                else -> false
            }
        }
    }
}
