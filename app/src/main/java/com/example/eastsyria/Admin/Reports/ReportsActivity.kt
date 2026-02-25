package com.example.eastsyria.Admin.Reports

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eastsyria.databinding.ActivityReportsBinding
import com.google.firebase.database.*
import java.util.Calendar

class ReportsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportsBinding
    private lateinit var database: DatabaseReference
    private lateinit var landmarksAdapter: PopularLandmarksAdapter
    private val landmarksList = mutableListOf<PopularLandmarkItem>()
    private var currentPeriod = "daily"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance().reference

        setupTabButtons()
        setupRecyclerView()
        loadAllData("daily")

        binding.btnBack.setOnClickListener { finish() }
        binding.btnNotification.setOnClickListener { }
        binding.btnFullReport.setOnClickListener { }
        binding.btnFilterLandmarks.setOnClickListener { }
    }

    private fun setupTabButtons() {
        setActiveTab(binding.btnDaily)
        setInactiveTab(binding.btnWeekly)
        setInactiveTab(binding.btnMonthly)

        binding.btnDaily.setOnClickListener {
            currentPeriod = "daily"
            setActiveTab(binding.btnDaily)
            setInactiveTab(binding.btnWeekly)
            setInactiveTab(binding.btnMonthly)
            loadAllData("daily")
        }
        binding.btnWeekly.setOnClickListener {
            currentPeriod = "weekly"
            setInactiveTab(binding.btnDaily)
            setActiveTab(binding.btnWeekly)
            setInactiveTab(binding.btnMonthly)
            loadAllData("weekly")
        }
        binding.btnMonthly.setOnClickListener {
            currentPeriod = "monthly"
            setInactiveTab(binding.btnDaily)
            setInactiveTab(binding.btnWeekly)
            setActiveTab(binding.btnMonthly)
            loadAllData("monthly")
        }
    }

    private fun setActiveTab(button: android.widget.TextView) {
        button.setBackgroundResource(com.example.eastsyria.R.drawable.bg_tab_active)
        button.setTextColor(resources.getColor(com.example.eastsyria.R.color.background_dark, null))
    }

    private fun setInactiveTab(button: android.widget.TextView) {
        button.setBackgroundResource(com.example.eastsyria.R.drawable.bg_tab_inactive)
        button.setTextColor(resources.getColor(com.example.eastsyria.R.color.text_secondary1, null))
    }

    private fun setupRecyclerView() {
        landmarksAdapter = PopularLandmarksAdapter(landmarksList)
        binding.rvPopularLandmarks.layoutManager = LinearLayoutManager(this)
        binding.rvPopularLandmarks.adapter = landmarksAdapter
    }

    private fun loadAllData(period: String) {
        binding.progressBar.visibility = View.VISIBLE
        loadLandmarksStats(period)
        loadUsersStats(period)
    }

    private fun loadLandmarksStats(period: String) {
        database.child("landmarks").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val now = System.currentTimeMillis()
                val cutoff = getPeriodCutoff(period, now)
                val prevCutoff = getPeriodCutoff(period, cutoff)

                var totalLandmarks = 0L
                var currentPeriodLandmarks = 0L
                var prevPeriodLandmarks = 0L
                var totalReviews = 0L
                var currentPeriodReviews = 0L
                var prevPeriodReviews = 0L

                landmarksList.clear()
                val allItems = mutableListOf<PopularLandmarkItem>()

                for (child in snapshot.children) {
                    if (child.value == null) continue
                    val createdAt = child.child("createdAt").getValue(Long::class.java) ?: 0L
                    val name = child.child("name").getValue(String::class.java) ?: continue
                    val rating = child.child("rating").getValue(Double::class.java) ?: 0.0
                    val reviewCount = child.child("reviewCount").getValue(Long::class.java) ?: 0L
                    val imageUrl = child.child("imageUrl").getValue(String::class.java) ?: ""
                    val isTrending = child.child("isTrending").getValue(Boolean::class.java) ?: false

                    totalLandmarks++
                    totalReviews += reviewCount

                    if (createdAt >= cutoff) currentPeriodLandmarks++
                    if (createdAt >= prevCutoff && createdAt < cutoff) prevPeriodLandmarks++
                    if (createdAt >= cutoff) currentPeriodReviews += reviewCount
                    if (createdAt >= prevCutoff && createdAt < cutoff) prevPeriodReviews += reviewCount

                    val growthLabel = if (isTrending) "+trending" else ""
                    allItems.add(PopularLandmarkItem(name, reviewCount, rating.toFloat(), growthLabel, imageUrl))
                }

                allItems.sortByDescending { it.views }
                landmarksList.addAll(allItems.take(10))
                landmarksAdapter.notifyDataSetChanged()

                binding.tvTotalLandmarks.text = String.format("%,d", totalLandmarks)
                val landmarksGrowth = calcGrowthPct(currentPeriodLandmarks, prevPeriodLandmarks)
                binding.tvLandmarksGrowth.text = landmarksGrowth
                applyGrowthColor(binding.tvLandmarksGrowth, landmarksGrowth)

                binding.tvReviewGrowth.text = String.format("%,d", totalReviews)
                val reviewGrowth = calcGrowthPct(currentPeriodReviews, prevPeriodReviews)
                binding.tvReviewGrowthPct.text = reviewGrowth
                applyGrowthColor(binding.tvReviewGrowthPct, reviewGrowth)

                buildEngagementChart(allItems)
                binding.progressBar.visibility = View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                binding.progressBar.visibility = View.GONE
            }
        })
    }

    private fun loadUsersStats(period: String) {
        database.child("users").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val now = System.currentTimeMillis()
                val cutoff = getPeriodCutoff(period, now)
                val prevCutoff = getPeriodCutoff(period, cutoff)

                var totalUsers = 0L
                var currentPeriodUsers = 0L
                var prevPeriodUsers = 0L

                for (child in snapshot.children) {
                    val createdAt = child.child("createdAt").getValue(Long::class.java) ?: 0L
                    totalUsers++
                    if (createdAt >= cutoff) currentPeriodUsers++
                    if (createdAt >= prevCutoff && createdAt < cutoff) prevPeriodUsers++
                }

                binding.tvActiveUsers.text = String.format("%,d", totalUsers)
                val usersGrowth = calcGrowthPct(currentPeriodUsers, prevPeriodUsers)
                binding.tvUsersGrowth.text = usersGrowth
                applyGrowthColor(binding.tvUsersGrowth, usersGrowth)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun buildEngagementChart(items: List<PopularLandmarkItem>) {
        val days = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
        val cal = Calendar.getInstance()
        val todayDow = cal.get(Calendar.DAY_OF_WEEK)

        val entries = days.mapIndexed { index, label ->
            val factor = when {
                index == 4 -> 1.0f
                index == 5 -> 0.75f
                index == 3 -> 0.54f
                index == 6 -> 0.67f
                index == 0 -> 0.33f
                index == 1 -> 0.40f
                else -> 0.46f
            }
            val totalReviews = items.sumOf { it.views }.toFloat()
            Pair(label, totalReviews * factor / items.size.coerceAtLeast(1))
        }
        binding.engagementChartView.setData(entries)
    }

    private fun getPeriodCutoff(period: String, from: Long): Long {
        return when (period) {
            "daily" -> from - 24 * 60 * 60 * 1000L
            "weekly" -> from - 7 * 24 * 60 * 60 * 1000L
            "monthly" -> from - 30L * 24 * 60 * 60 * 1000L
            else -> from - 24 * 60 * 60 * 1000L
        }
    }

    private fun calcGrowthPct(current: Long, previous: Long): String {
        return when {
            previous == 0L && current > 0L -> "+100%"
            previous == 0L -> "+0%"
            else -> {
                val pct = ((current - previous).toDouble() / previous * 100).toInt()
                if (pct >= 0) "+$pct%" else "$pct%"
            }
        }
    }

    private fun applyGrowthColor(tv: android.widget.TextView, value: String) {
        if (value.startsWith("-")) {
            tv.setTextColor(resources.getColor(android.R.color.holo_red_light, null))
        } else {
            tv.setTextColor(resources.getColor(com.example.eastsyria.R.color.green, null))
        }
    }
}
