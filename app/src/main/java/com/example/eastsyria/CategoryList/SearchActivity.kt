package com.example.eastsyria.CategoryList
import CategoryItemAdapter
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eastsyria.CategoryList.CategoryItem
import com.example.eastsyria.databinding.DialogSearchBinding

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
        binding = DialogSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)


        @Suppress("DEPRECATION")
        allItems = intent.getSerializableExtra(EXTRA_ITEMS) as? ArrayList<CategoryItem> ?: emptyList()


        setupRecyclerView()
        setupSearchBar()
        setupButtons()
    }

    private fun setupRecyclerView() {
        adapter = CategoryItemAdapter(
            items = searchResults,
            onItemClick = { item ->
                val resultIntent = Intent().apply {
                    putExtra(EXTRA_SELECTED_ITEM, item)
                }
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

        binding.btnClearSearch.setOnClickListener {
            binding.etSearch.text.clear()
        }
    }

    private fun performSearch(query: String) {
        searchResults.clear()

        if (query.isEmpty()) {
            binding.layoutEmptySearch.visibility = View.GONE
            binding.recyclerSearchResults.visibility = View.GONE
            return
        }

        val results = allItems.filter { item ->
            item.name.contains(query, ignoreCase = true) ||
                    item.description.contains(query, ignoreCase = true) ||
                    item.location.city.contains(query, ignoreCase = true) ||
                    item.location.governorate.contains(query, ignoreCase = true) ||
                    item.tags.any { it.contains(query, ignoreCase = true) }
        }

        searchResults.addAll(results)
        adapter.updateItems(searchResults)

        if (searchResults.isEmpty()) {
            binding.layoutEmptySearch.visibility = View.VISIBLE
            binding.recyclerSearchResults.visibility = View.GONE
        } else {
            binding.layoutEmptySearch.visibility = View.GONE
            binding.recyclerSearchResults.visibility = View.VISIBLE
        }
    }

    private fun setupButtons() {
        binding.btnBackSearch.setOnClickListener {
            finish()
        }
    }
}
