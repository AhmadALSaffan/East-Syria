package com.example.eastsyria.Admin.Reports

import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eastsyria.R
import com.example.eastsyria.Ui.SkeletonPulse
import com.example.eastsyria.Ui.warmEdgeToEdge
import com.example.eastsyria.databinding.ActivityReportsBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Reports, counted from the records themselves.
 *
 * The previous version showed placeholder figures, "views" that were really
 * review counts, and a weekly engagement chart drawn from fixed multipliers
 * (nothing in the app records a view). All of that is gone. What remains can
 * be derived from `createdAt`, `status` and `rating`, and says so.
 */
class ReportsActivity : AppCompatActivity() {

    private enum class Period(val millis: Long, val buckets: Int) {
        DAY(24L * HOUR, 6),
        WEEK(7L * 24 * HOUR, 7),
        MONTH(30L * 24 * HOUR, 6)
    }

    private data class LandmarkRecord(
        val name: String,
        val category: String,
        val status: String,
        val rating: Double,
        val imageUrl: String,
        val createdAt: Long
    )

    private lateinit var binding: ActivityReportsBinding
    private lateinit var database: DatabaseReference
    private lateinit var pulse: SkeletonPulse
    private lateinit var topRatedAdapter: PopularLandmarksAdapter
    private val topRated = mutableListOf<PopularLandmarkItem>()

    private var period = Period.WEEK
    private var landmarks: List<LandmarkRecord> = emptyList()
    private var userCreatedAt: List<Long> = emptyList()
    private var loadedAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        warmEdgeToEdge()
        binding = ActivityReportsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        pulse = SkeletonPulse(binding.loadingState)

        database = FirebaseDatabase.getInstance().reference

        binding.header.onBack { finish() }
        setupInsets()
        setupTabs()
        setupList()
        load()
    }

    override fun onDestroy() {
        pulse.stop()
        super.onDestroy()
    }

    private fun setupInsets() {
        val base = binding.reportsContent.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.reportsContent) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.updatePadding(bottom = base + bars.bottom)
            insets
        }
    }

    private fun setupTabs() {
        val tabs = mapOf(
            binding.btnDaily to Period.DAY,
            binding.btnWeekly to Period.WEEK,
            binding.btnMonthly to Period.MONTH
        )
        tabs.forEach { (view, value) ->
            view.setOnClickListener {
                period = value
                paintTabs()
                render()
            }
        }
        paintTabs()
    }

    private fun paintTabs() {
        listOf(
            binding.btnDaily to Period.DAY,
            binding.btnWeekly to Period.WEEK,
            binding.btnMonthly to Period.MONTH
        ).forEach { (view, value) ->
            val selected = value == period
            view.setBackgroundResource(if (selected) R.drawable.bg_tab_active else R.drawable.bg_tab_inactive)
            view.setTextColor(
                ContextCompat.getColor(this, if (selected) R.color.terracotta_soft else R.color.ivory_dim)
            )
            view.isSelected = selected
        }
    }

    private fun setupList() {
        topRatedAdapter = PopularLandmarksAdapter(topRated)
        binding.rvPopularLandmarks.layoutManager = LinearLayoutManager(this)
        binding.rvPopularLandmarks.adapter = topRatedAdapter
    }

    // ------------------------------------------------------------ data

    /** Both reads have to land before there is a report to draw. */
    private fun load() {
        showLoading()
        var landmarksDone = false
        var usersDone = false
        var failed = false

        fun finishIfReady() {
            if (failed || !landmarksDone || !usersDone) return
            loadedAt = System.currentTimeMillis()
            render()
        }

        fun fail(what: String, error: DatabaseError) {
            if (failed) return
            failed = true
            Log.e(TAG, "Failed to read $what: ${error.message}")
            showError()
        }

        database.child("landmarks").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                landmarks = snapshot.children.mapNotNull { child ->
                    if (child.value == null) return@mapNotNull null
                    val name = child.child("name").getValue(String::class.java) ?: return@mapNotNull null
                    LandmarkRecord(
                        name = name,
                        category = child.child("category").getValue(String::class.java).orEmpty(),
                        status = child.child("status").getValue(String::class.java).orEmpty(),
                        rating = child.child("rating").getValue(Double::class.java) ?: 0.0,
                        imageUrl = child.child("imageUrl").getValue(String::class.java).orEmpty(),
                        createdAt = child.child("createdAt").getValue(Long::class.java) ?: 0L
                    )
                }
                landmarksDone = true
                finishIfReady()
            }

            override fun onCancelled(error: DatabaseError) = fail("landmarks", error)
        })

        database.child("users").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userCreatedAt = snapshot.children.map {
                    it.child("createdAt").getValue(Long::class.java) ?: 0L
                }
                usersDone = true
                finishIfReady()
            }

            override fun onCancelled(error: DatabaseError) = fail("users", error)
        })
    }

    // ------------------------------------------------------------ drawing

    private fun render() {
        val now = loadedAt
        val since = now - period.millis

        binding.tvLastUpdated.text = getString(
            R.string.reports_updated,
            DateFormat.getTimeFormat(this).format(Date(loadedAt))
        )

        binding.tvTotalLandmarks.text = format(landmarks.size)
        binding.tvTotalUsers.text = format(userCreatedAt.size)
        binding.tvPublished.text = format(landmarks.count { it.status.equals("published", true) })
        binding.tvPending.text = format(landmarks.count { it.status.equals("pending", true) })

        val newLandmarks = landmarks.count { it.createdAt >= since }
        val newUsers = userCreatedAt.count { it >= since }
        val deltaRes = when (period) {
            Period.DAY -> R.string.reports_delta_day
            Period.WEEK -> R.string.reports_delta_week
            Period.MONTH -> R.string.reports_delta_month
        }
        binding.tvLandmarksGrowth.text = getString(deltaRes, newLandmarks)
        binding.tvUsersGrowth.text = getString(deltaRes, newUsers)

        drawChart(now)
        drawTopRated()
        showContent()
    }

    /** Counts of new landmarks and new users per bucket of the chosen period. */
    private fun drawChart(now: Long) {
        val bucketMillis = period.millis / period.buckets
        val start = now - period.millis
        val counts = FloatArray(period.buckets)
        (landmarks.map { it.createdAt } + userCreatedAt).forEach { created ->
            if (created < start || created > now) return@forEach
            val index = ((created - start) / bucketMillis).toInt().coerceIn(0, period.buckets - 1)
            counts[index]++
        }

        val labelFormat = when (period) {
            Period.DAY -> SimpleDateFormat("HH:mm", Locale.getDefault())
            Period.WEEK -> SimpleDateFormat("EEE", Locale.getDefault())
            Period.MONTH -> SimpleDateFormat("d MMM", Locale.getDefault())
        }
        val entries = counts.mapIndexed { i, value ->
            labelFormat.format(Date(start + i * bucketMillis)) to value
        }

        binding.engagementChartView.setData(entries)
        val empty = counts.all { it == 0f }
        binding.tvChartEmpty.visibility = if (empty) View.VISIBLE else View.GONE
        binding.engagementChartView.visibility = if (empty) View.INVISIBLE else View.VISIBLE
    }

    private fun drawTopRated() {
        topRated.clear()
        topRated.addAll(
            landmarks.filter { it.rating > 0.0 }
                .sortedByDescending { it.rating }
                .take(5)
                .map { PopularLandmarkItem(it.name, it.category, it.rating.toFloat(), it.imageUrl) }
        )
        topRatedAdapter.notifyDataSetChanged()
        val empty = topRated.isEmpty()
        binding.rvPopularLandmarks.visibility = if (empty) View.GONE else View.VISIBLE
        binding.tvTopRatedEmpty.visibility = if (empty) View.VISIBLE else View.GONE
    }

    private fun format(value: Int): String = String.format(Locale.getDefault(), "%,d", value)

    // ------------------------------------------------------------ states

    private fun showLoading() {
        binding.statePanel.hide()
        binding.figuresGroup.visibility = View.GONE
        binding.loadingState.visibility = View.VISIBLE
        pulse.start()
    }

    private fun showContent() {
        pulse.stop()
        binding.statePanel.hide()
        binding.loadingState.visibility = View.GONE
        binding.figuresGroup.visibility = View.VISIBLE
    }

    private fun showError() {
        pulse.stop()
        binding.loadingState.visibility = View.GONE
        binding.figuresGroup.visibility = View.GONE
        binding.statePanel.show(
            getString(R.string.reports_error_title),
            getString(R.string.search_error_body),
            getString(R.string.action_retry)
        ) { load() }
    }

    companion object {
        private const val TAG = "ReportsActivity"
        private const val HOUR = 60L * 60 * 1000
    }
}
