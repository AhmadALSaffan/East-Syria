package com.example.eastsyria.MainPage

import android.animation.ValueAnimator
import android.app.ActivityOptions
import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.transition.TransitionManager
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.eastsyria.Categories.CategoriesActivity
import com.example.eastsyria.CategoryList.CategoryListActivity
import com.example.eastsyria.Details.LandmarkDetailActivity
import com.example.eastsyria.MainPage.Adapters.FeaturedLandmarkAdapter
import com.example.eastsyria.MainPage.Adapters.TrendingDestinationAdapter
import com.example.eastsyria.MainPage.Data.Landmark
import com.example.eastsyria.Map.MapActivity
import com.example.eastsyria.Nav.NavDock
import com.example.eastsyria.Notifications.NotificationsActivity
import com.example.eastsyria.Profile.ProfileActivity
import com.example.eastsyria.R
import com.example.eastsyria.Saved.SavedLandmarksActivity
import com.example.eastsyria.Search.SearchLandmarksActivity
import com.example.eastsyria.databinding.ActivityMainPageBinding
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database

class MainPageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainPageBinding

    private lateinit var heroAdapter: FeaturedLandmarkAdapter
    private lateinit var trendingAdapter: TrendingDestinationAdapter

    // What the hero shows: featured landmarks, falling back to trending, then
    // to whatever is published, so the first viewport is never an empty frame
    // while the catalog has something to show.
    private val heroLandmarks = mutableListOf<Landmark>()
    private val trendingDestinations = mutableListOf<Landmark>()

    private var landmarksRef: DatabaseReference? = null
    private var landmarksListener: ValueEventListener? = null

    private var heroPage = -1
    private var pulseAnimator: ValueAnimator? = null

    private enum class HeroState { LOADING, CONTENT, EMPTY, ERROR }

    companion object {
        const val EXTRA_SELECT_EXPLORE = "select_explore"
        private const val MAX_HERO_PAGES = 5
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainPageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        // The hero photograph runs under both system bars, so their icons must
        // stay light whatever the system theme says.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        setupInsets()
        setupHero()
        setupTrending()
        setupNavigation()
        setupClickListeners()
        setupScrollBehavior()
        loadLandmarksFromFirebase()
        handleExploreExtra(intent)
    }

    override fun onResume() {
        super.onResume()
        // Coming back from a tab: the selector glides home to Explore.
        binding.navDock.select(NavDock.TAB_EXPLORE, animate = true)
    }

    override fun onDestroy() {
        stopPulse()
        detachLandmarksListener()
        super.onDestroy()
    }

    private fun handleExploreExtra(intent: Intent) {
        if (intent.getBooleanExtra(EXTRA_SELECT_EXPLORE, false)) {
            binding.navDock.select(NavDock.TAB_EXPLORE, animate = false)
        }
    }

    // ------------------------------------------------------------ chrome

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            binding.topBarBg.updateLayoutParams<ViewGroup.LayoutParams> {
                height = dp(76) + bars.top
            }
            binding.searchPill.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = dp(10) + bars.top
            }
            insets
        }
    }

    private fun setupNavigation() {
        binding.navDock.attach(this, NavDock.TAB_EXPLORE)
        binding.navDock.onReselected = { binding.scrollMain.smoothScrollTo(0, 0) }
    }

    /**
     * The search pill floats over the hero and gains a solid backing once the
     * hero has scrolled away.
     */
    private fun setupScrollBehavior() {
        val fadeStart = dp(170)
        val fadeSpan = dp(80)
        binding.scrollMain.setOnScrollChangeListener(
            androidx.core.widget.NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
                binding.topBarBg.alpha = ((scrollY - fadeStart).toFloat() / fadeSpan).coerceIn(0f, 1f)
            }
        )
    }

    // ------------------------------------------------------------ hero and rails

    private fun setupHero() {
        heroAdapter = FeaturedLandmarkAdapter(heroLandmarks) { landmark ->
            openLandmarkDetail(landmark)
        }
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        val snap = PagerSnapHelper()
        binding.rvFeaturedLandmarks.apply {
            adapter = heroAdapter
            this.layoutManager = layoutManager
            snap.attachToRecyclerView(this)
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    val page = snap.findSnapView(layoutManager)
                        ?.let { layoutManager.getPosition(it) } ?: return
                    setHeroPage(page)
                }
            })
        }
    }

    private fun setupTrending() {
        trendingAdapter = TrendingDestinationAdapter(trendingDestinations) { landmark ->
            openLandmarkDetail(landmark)
        }
        binding.rvTrendingDestinations.apply {
            adapter = trendingAdapter
            layoutManager = LinearLayoutManager(this@MainPageActivity)
        }
    }

    private fun rebuildDots() {
        binding.heroDots.removeAllViews()
        heroPage = -1
        if (heroLandmarks.size < 2) return
        heroLandmarks.indices.forEach { _ ->
            val dot = View(this).apply {
                layoutParams = ViewGroup.MarginLayoutParams(dp(6), dp(6)).apply {
                    marginEnd = dp(6)
                }
                setBackgroundResource(R.drawable.bg_dot)
            }
            binding.heroDots.addView(dot)
        }
        setHeroPage(0)
    }

    private fun setHeroPage(page: Int) {
        if (page == heroPage || page !in 0 until binding.heroDots.childCount) return
        heroPage = page
        TransitionManager.beginDelayedTransition(binding.heroDots)
        for (i in 0 until binding.heroDots.childCount) {
            val dot = binding.heroDots.getChildAt(i)
            val active = i == page
            dot.setBackgroundResource(if (active) R.drawable.bg_dot_active else R.drawable.bg_dot)
            dot.layoutParams = (dot.layoutParams as ViewGroup.MarginLayoutParams).apply {
                width = if (active) dp(22) else dp(6)
            }
        }
    }

    private fun openLandmarkDetail(landmark: Landmark) {
        val intent = Intent(this, LandmarkDetailActivity::class.java)
        intent.putExtra("LANDMARK_ID", landmark.id)
        startActivity(intent)
    }

    // ------------------------------------------------------------ data

    private fun loadLandmarksFromFirebase() {
        detachLandmarksListener()
        showState(HeroState.LOADING)

        val ref = Firebase.database.reference.child("landmarks")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val featured = mutableListOf<Landmark>()
                val trending = mutableListOf<Landmark>()
                val published = mutableListOf<Landmark>()

                for (landmarkSnapshot in snapshot.children) {
                    try {
                        val landmark = landmarkSnapshot.getValue(Landmark::class.java)
                        if (landmark != null && landmark.status == "published") {
                            landmark.id = landmarkSnapshot.key ?: ""
                            published.add(landmark)
                            if (landmark.isFeatured) featured.add(landmark)
                            if (landmark.isTrending) trending.add(landmark)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing landmark ${landmarkSnapshot.key}: ${e.message}", e)
                    }
                }

                val hero = when {
                    featured.isNotEmpty() -> featured
                    trending.isNotEmpty() -> trending
                    else -> published
                }.take(MAX_HERO_PAGES)

                heroLandmarks.clear()
                heroLandmarks.addAll(hero)
                trendingDestinations.clear()
                trendingDestinations.addAll(trending)
                heroAdapter.notifyDataSetChanged()
                trendingAdapter.notifyDataSetChanged()
                rebuildDots()

                if (published.isEmpty()) {
                    showState(HeroState.EMPTY)
                } else {
                    showState(HeroState.CONTENT)
                    binding.tvCatalogCount.text =
                        getString(R.string.home_catalog_count, published.size)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Landmarks load cancelled: ${error.message}")
                showState(HeroState.ERROR)
            }
        }
        landmarksRef = ref
        landmarksListener = listener
        ref.addValueEventListener(listener)
    }

    private fun detachLandmarksListener() {
        val ref = landmarksRef
        val listener = landmarksListener
        if (ref != null && listener != null) ref.removeEventListener(listener)
        landmarksRef = null
        landmarksListener = null
    }

    // ------------------------------------------------------------ states

    private fun showState(state: HeroState) {
        val loading = state == HeroState.LOADING
        val content = state == HeroState.CONTENT

        binding.heroSkeleton.visibility = if (loading) View.VISIBLE else View.GONE
        binding.trendingSkeleton.visibility = if (loading) View.VISIBLE else View.GONE
        binding.rvFeaturedLandmarks.visibility = if (content) View.VISIBLE else View.GONE
        binding.heroDots.visibility = if (content) View.VISIBLE else View.GONE
        binding.sectionTrending.visibility =
            if (content && trendingDestinations.isNotEmpty()) View.VISIBLE else View.GONE
        binding.tvCatalogCount.visibility = if (content) View.VISIBLE else View.GONE

        val showPanel = state == HeroState.EMPTY || state == HeroState.ERROR
        binding.heroState.visibility = if (showPanel) View.VISIBLE else View.GONE
        if (state == HeroState.EMPTY) {
            binding.tvStateTitle.setText(R.string.hero_empty_title)
            binding.tvStateBody.setText(R.string.hero_empty_body)
        } else if (state == HeroState.ERROR) {
            binding.tvStateTitle.setText(R.string.hero_error_title)
            binding.tvStateBody.setText(R.string.hero_error_body)
        }
        binding.btnRetry.visibility = if (state == HeroState.ERROR) View.VISIBLE else View.GONE

        if (loading) startPulse() else stopPulse()
    }

    /** The two skeletons breathe together; nothing runs when animations are off. */
    private fun startPulse() {
        if (pulseAnimator != null || !ValueAnimator.areAnimatorsEnabled()) return
        pulseAnimator = ValueAnimator.ofFloat(1f, 0.55f).apply {
            duration = 900
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                val alpha = it.animatedValue as Float
                binding.heroSkeleton.alpha = alpha
                binding.trendingSkeleton.alpha = alpha
            }
            start()
        }
    }

    private fun stopPulse() {
        pulseAnimator?.cancel()
        pulseAnimator = null
        binding.heroSkeleton.alpha = 1f
        binding.trendingSkeleton.alpha = 1f
    }

    // ------------------------------------------------------------ clicks

    private fun setupClickListeners() {
        binding.apply {
            searchPill.setOnClickListener {
                startActivity(Intent(this@MainPageActivity, SearchLandmarksActivity::class.java))
            }

            btnHistorical.setOnClickListener {
                startCategoryList(getString(R.string.cat_name_historical), "historical_sites")
            }
            btnNature.setOnClickListener {
                startCategoryList(getString(R.string.cat_name_nature), "nature_rivers")
            }
            btnCulture.setOnClickListener {
                startCategoryList(getString(R.string.cat_name_culture), "local_culture")
            }
            btnMuseums.setOnClickListener {
                startCategoryList(getString(R.string.cat_name_museums), "museums")
            }
            btnCuisine.setOnClickListener {
                startCategoryList(getString(R.string.cat_name_cuisine), "cuisine")
            }
            btnCrafts.setOnClickListener {
                startCategoryList(getString(R.string.cat_name_crafts), "handicrafts")
            }
            btnMore.setOnClickListener {
                startActivity(Intent(this@MainPageActivity, CategoriesActivity::class.java))
            }

            tvViewMap.setOnClickListener { openMap() }
            btnRetry.setOnClickListener { loadLandmarksFromFirebase() }
        }
    }

    private fun openMap() {
        startActivity(Intent(this, MapActivity::class.java))
    }

    private fun startCategoryList(name: String, type: String) {
        val intent = Intent(this, CategoryListActivity::class.java).apply {
            putExtra(CategoryListActivity.EXTRA_CATEGORY_NAME, name)
            putExtra(CategoryListActivity.EXTRA_CATEGORY_TYPE, type)
        }
        startActivity(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleExploreExtra(intent)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
