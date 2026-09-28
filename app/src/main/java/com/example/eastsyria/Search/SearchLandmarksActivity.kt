package com.example.eastsyria.Search

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.EditorInfo
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.eastsyria.Details.LandmarkDetailActivity
import com.example.eastsyria.MainPage.Data.Landmark
import com.example.eastsyria.R
import com.example.eastsyria.databinding.ActivitySearchLandmarksBinding
import com.google.android.material.chip.Chip
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.math.max

class SearchLandmarksActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchLandmarksBinding
    private val database = FirebaseDatabase.getInstance()
    private lateinit var adapter: SearchResultAdapter
    private val allLandmarks = mutableListOf<Landmark>()

    /** Null means every category. */
    private var currentCategory: String? = null
    private var loaded = false
    private var pulseAnimator: ValueAnimator? = null

    private enum class State { LOADING, RESULTS, EMPTY, ERROR }

    companion object {
        private const val TAG = "SearchLandmarksActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        binding = ActivitySearchLandmarksBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInsets()
        setupViews()
        setupRecyclerView()
        setupSearch()
        loadAllLandmarks()

        binding.etSearch.requestFocus()
        binding.etSearch.post {
            WindowCompat.getInsetsController(window, binding.etSearch)
                .show(WindowInsetsCompat.Type.ime())
        }
    }

    override fun onDestroy() {
        stopPulse()
        super.onDestroy()
    }

    /** The list and the message panel lift above the keyboard and the gesture bar. */
    private fun setupInsets() {
        val gap = dp(10)
        val listGap = dp(16)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val bottom = max(bars.bottom, ime.bottom)
            binding.topBar.updatePadding(top = bars.top + gap)
            binding.rvSearchResults.updatePadding(bottom = bottom + listGap)
            binding.messageState.updatePadding(bottom = bottom)
            insets
        }
    }

    private fun setupViews() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnClear.setOnClickListener { binding.etSearch.text.clear() }
        binding.btnStateAction.setOnClickListener {
            if (!loaded) loadAllLandmarks() else resetSearch()
        }
    }

    private fun setupRecyclerView() {
        adapter = SearchResultAdapter(allLandmarks) { landmark ->
            openLandmarkDetail(landmark)
        }

        binding.rvSearchResults.apply {
            adapter = this@SearchLandmarksActivity.adapter
            layoutManager = LinearLayoutManager(this@SearchLandmarksActivity)
            // Reading the results is done with the keyboard out of the way.
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                        WindowCompat.getInsetsController(window, binding.etSearch)
                            .hide(WindowInsetsCompat.Type.ime())
                    }
                }
            })
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.btnClear.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
                performSearch()
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                WindowCompat.getInsetsController(window, binding.etSearch)
                    .hide(WindowInsetsCompat.Type.ime())
                true
            } else {
                false
            }
        }
    }

    // ------------------------------------------------------------ filters

    /** One chip per category that exists in the catalog, plus "All". */
    private fun buildFilters() {
        val group = binding.chipGroup
        group.setOnCheckedStateChangeListener(null)
        group.removeAllViews()
        currentCategory = null

        val categories = adapter.categories()
        // A single category makes the row a decision with one answer.
        binding.filterScrollView.visibility = if (categories.size < 2) View.GONE else View.VISIBLE
        if (categories.size < 2) return

        addChip(getString(R.string.search_filter_all), null, checked = true)
        categories.forEach { addChip(it, it, checked = false) }

        group.setOnCheckedStateChangeListener { _, checkedIds ->
            val chip = checkedIds.firstOrNull()?.let { group.findViewById<Chip>(it) }
            currentCategory = chip?.tag as? String
            performSearch()
        }
    }

    private fun addChip(label: String, category: String?, checked: Boolean) {
        val chip = LayoutInflater.from(this)
            .inflate(R.layout.item_filter_chip, binding.chipGroup, false) as Chip
        chip.id = View.generateViewId()
        chip.text = label
        chip.tag = category
        chip.isChecked = checked
        binding.chipGroup.addView(chip)
    }

    private fun resetSearch() {
        binding.etSearch.text.clear()
        (binding.chipGroup.getChildAt(0) as? Chip)?.isChecked = true
        currentCategory = null
        performSearch()
    }

    // ------------------------------------------------------------ data

    private fun loadAllLandmarks() {
        loaded = false
        showState(State.LOADING)

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
                    loaded = true
                    buildFilters()
                    performSearch()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error loading landmarks: ${error.message}")
                    showState(State.ERROR)
                }
            })
    }

    private fun performSearch() {
        if (!loaded) return
        val query = binding.etSearch.text.toString()
        adapter.filter(query, currentCategory)
        updateResultsCount(query)
        showState(if (adapter.getFilteredCount() == 0) State.EMPTY else State.RESULTS)
    }

    private fun updateResultsCount(query: String) {
        val count = adapter.getFilteredCount()
        val q = query.trim()
        binding.tvResultsCount.text = if (q.isEmpty()) {
            resources.getQuantityString(R.plurals.search_count_all, count, count)
        } else {
            resources.getQuantityString(R.plurals.search_count_query, count, count, q)
        }
    }

    private fun openLandmarkDetail(landmark: Landmark) {
        val intent = Intent(this, LandmarkDetailActivity::class.java)
        intent.putExtra("LANDMARK_ID", landmark.id)
        startActivity(intent)
    }

    // ------------------------------------------------------------ states

    private fun showState(state: State) {
        val results = state == State.RESULTS
        val message = state == State.EMPTY || state == State.ERROR

        binding.loadingState.visibility = if (state == State.LOADING) View.VISIBLE else View.GONE
        binding.rvSearchResults.visibility = if (results) View.VISIBLE else View.GONE
        binding.messageState.visibility = if (message) View.VISIBLE else View.GONE
        binding.tvResultsCount.visibility =
            if (results) View.VISIBLE else View.GONE

        when (state) {
            State.EMPTY -> {
                val q = binding.etSearch.text.toString().trim()
                binding.tvStateTitle.setText(R.string.search_empty_title)
                val emptyCatalog = q.isEmpty() && currentCategory == null
                when {
                    q.isNotEmpty() ->
                        binding.tvStateBody.text = getString(R.string.search_empty_body_query, q)

                    emptyCatalog -> binding.tvStateBody.setText(R.string.hero_empty_body)
                    else -> binding.tvStateBody.setText(R.string.search_empty_body_filter)
                }
                binding.btnStateAction.setText(R.string.search_clear)
                // Clearing does nothing when the catalog itself has nothing in it.
                binding.btnStateAction.visibility = if (emptyCatalog) View.GONE else View.VISIBLE
            }

            State.ERROR -> {
                binding.tvStateTitle.setText(R.string.search_error_title)
                binding.tvStateBody.setText(R.string.search_error_body)
                binding.btnStateAction.setText(R.string.action_retry)
                binding.btnStateAction.visibility = View.VISIBLE
            }

            else -> Unit
        }

        if (state == State.LOADING) startPulse() else stopPulse()
    }

    /** The row silhouettes breathe; nothing runs when the system has animations off. */
    private fun startPulse() {
        if (pulseAnimator != null || !ValueAnimator.areAnimatorsEnabled()) return
        pulseAnimator = ValueAnimator.ofFloat(1f, 0.55f).apply {
            duration = 900
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { binding.loadingState.alpha = it.animatedValue as Float }
            start()
        }
    }

    private fun stopPulse() {
        pulseAnimator?.cancel()
        pulseAnimator = null
        binding.loadingState.alpha = 1f
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
