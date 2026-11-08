package com.example.eastsyria.Categories

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.GridLayoutManager
import com.example.eastsyria.CategoryList.CategoryListActivity
import com.example.eastsyria.CategoryList.CategoryRules
import com.example.eastsyria.MainPage.Data.Landmark
import com.example.eastsyria.Map.MapActivity
import com.example.eastsyria.R
import com.example.eastsyria.Saved.SavedLandmarksActivity
import com.example.eastsyria.Ui.warmEdgeToEdge
import com.example.eastsyria.databinding.ActivityCategoriesBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class CategoriesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoriesBinding
    private lateinit var adapter: CategoryTileAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        warmEdgeToEdge()
        binding = ActivityCategoriesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupHeader()
        setupTiles()
        setupInsets()
        binding.btnExploreMap.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }
        loadStats()
    }

    private fun setupHeader() {
        binding.header.onBack { finish() }
        binding.header.setIconA(
            R.drawable.ic_bookmark,
            getString(R.string.home_saved)
        ) { startActivity(Intent(this, SavedLandmarksActivity::class.java)) }
    }

    private fun setupTiles() {
        adapter = CategoryTileAdapter { def ->
            startActivity(
                Intent(this, CategoryListActivity::class.java).apply {
                    putExtra(CategoryListActivity.EXTRA_CATEGORY_NAME, getString(def.name))
                    putExtra(CategoryListActivity.EXTRA_CATEGORY_TYPE, def.type)
                }
            )
        }
        binding.rvCategories.apply {
            layoutManager = GridLayoutManager(this@CategoriesActivity, 2)
            adapter = this@CategoriesActivity.adapter
        }
    }

    private fun setupInsets() {
        val base = binding.categoriesContent.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.categoriesContent) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.updatePadding(bottom = base + bars.bottom)
            insets
        }
    }

    /**
     * One read of the catalog: how many published places each category holds,
     * and the best-rated one's photograph. The tiles are already on screen, so
     * a failure costs the counts, not the screen.
     */
    private fun loadStats() {
        FirebaseDatabase.getInstance().reference.child("landmarks")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val landmarks = snapshot.children.mapNotNull {
                        try {
                            it.getValue(Landmark::class.java)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing landmark ${it.key}: ${e.message}")
                            null
                        }
                    }.filter { it.status == "published" }

                    val stats = CategoryRules.all.associate { def ->
                        val inCategory = landmarks.filter {
                            CategoryRules.matches(def.type, it.category, it.tags, it.name, it.isHistorical)
                        }
                        val best = inCategory
                            .filter { it.imageUrl.isNotBlank() }
                            .maxByOrNull { it.rating }
                        def.type to CategoryStats(inCategory.size, best?.imageUrl.orEmpty())
                    }
                    adapter.updateStats(stats)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error loading category stats: ${error.message}")
                    Snackbar.make(binding.root, R.string.categories_stats_failed, Snackbar.LENGTH_INDEFINITE)
                        .setBackgroundTint(ContextCompat.getColor(this@CategoriesActivity, R.color.ivory))
                        .setTextColor(ContextCompat.getColor(this@CategoriesActivity, R.color.canvas))
                        .setActionTextColor(ContextCompat.getColor(this@CategoriesActivity, R.color.canvas))
                        .setAction(R.string.action_retry) { loadStats() }
                        .show()
                }
            })
    }

    companion object {
        private const val TAG = "CategoriesActivity"
    }
}
