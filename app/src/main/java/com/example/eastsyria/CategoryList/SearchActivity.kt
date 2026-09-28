package com.example.eastsyria.CategoryList

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eastsyria.R
import com.example.eastsyria.Ui.warmEdgeToEdge
import com.example.eastsyria.databinding.DialogSearchBinding
import kotlin.math.max

/** Search inside one category. Nothing is listed until something is typed. */
class SearchActivity : AppCompatActivity() {

    private lateinit var binding: DialogSearchBinding
    private lateinit var adapter: CategoryItemAdapter
    private var allItems = listOf<CategoryItem>()
    private var searchResults = mutableListOf<CategoryItem>()

    companion object {
        const val EXTRA_ITEMS = "EXTRA_ITEMS"
        const val EXTRA_SELECTED_ITEM = "EXTRA_SELECTED_ITEM"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        warmEdgeToEdge()
        binding = DialogSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        @Suppress("DEPRECATION")
        allItems = intent.getSerializableExtra(EXTRA_ITEMS) as? ArrayList<CategoryItem> ?: emptyList()

        setupInsets()
        setupRecyclerView()
        setupSearchBar()
        binding.btnBackSearch.setOnClickListener { finish() }
    }

    private fun setupInsets() {
        val gap = (10 * resources.displayMetrics.density).toInt()
        val listGap = (16 * resources.displayMetrics.density).toInt()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val bottom = max(bars.bottom, ime.bottom)
            binding.topBar.updatePadding(top = bars.top + gap)
            binding.recyclerSearchResults.updatePadding(bottom = bottom + listGap)
            binding.statePanel.updatePadding(bottom = bottom)
            insets
        }
    }

    private fun setupRecyclerView() {
        adapter = CategoryItemAdapter(
            items = searchResults,
            onItemClick = { item ->
                val resultIntent = Intent().apply { putExtra(EXTRA_SELECTED_ITEM, item) }
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
        )

        binding.recyclerSearchResults.apply {
            layoutManager = LinearLayoutManager(this@SearchActivity)
            this.adapter = this@SearchActivity.adapter
        }
    }

    private fun setupSearchBar() {
        binding.etSearch.requestFocus()

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                binding.btnClearSearch.visibility = if (query.isEmpty()) View.GONE else View.VISIBLE
                performSearch(query)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnClearSearch.setOnClickListener { binding.etSearch.text.clear() }
    }

    private fun performSearch(query: String) {
        searchResults.clear()
        val q = query.trim()

        if (q.isEmpty()) {
            adapter.updateItems(searchResults)
            binding.statePanel.hide()
            return
        }

        searchResults.addAll(allItems.filter { item ->
            item.name.contains(q, ignoreCase = true) ||
                    item.description.contains(q, ignoreCase = true) ||
                    item.location.city.contains(q, ignoreCase = true) ||
                    item.location.governorate.contains(q, ignoreCase = true) ||
                    item.tags.any { it.contains(q, ignoreCase = true) }
        })
        adapter.updateItems(searchResults)

        if (searchResults.isEmpty()) {
            binding.statePanel.show(
                getString(R.string.search_empty_title),
                getString(R.string.search_empty_body_query, q)
            )
        } else {
            binding.statePanel.hide()
        }
    }
}
