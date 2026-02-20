package com.example.eastsyria.Admin

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eastsyria.Admin.Data.AdminLandmarkAdapter
import com.example.eastsyria.Admin.Data.LandmarkAdminModel
import com.example.eastsyria.Login.LoginActivity
import com.example.eastsyria.databinding.ActivityMainPageAdminBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MainPageAdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainPageAdminBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var adapter: AdminLandmarkAdapter
    private val allLandmarks = mutableListOf<LandmarkAdminModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainPageAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("landmarks")

        setupRecyclerView()
        loadStats()
        loadLandmarks()
        hideSystemBars()

        binding.btnLogout.setOnClickListener {
            val uid = auth.currentUser?.uid
            auth.signOut()
            if (uid != null) {
                getSharedPreferences("user_prefs", MODE_PRIVATE)
                    .edit()
                    .remove("role_$uid")
                    .apply()
            }
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.btnAddLandmark.setOnClickListener {
            //startActivity(Intent(this, AddLandmarkActivity::class.java))
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterLandmarks(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.tvViewAll.setOnClickListener {
           // startActivity(Intent(this, AllLandmarksAdminActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        adapter = AdminLandmarkAdapter(allLandmarks,
            onEdit = { landmark ->
               // val intent = Intent(this, EditLandmarkActivity::class.java)
               // intent.putExtra("landmarkId", landmark.id)
               // startActivity(intent)
            },
            onDelete = { landmark ->
                database.child(landmark.id).removeValue()
            }
        )
        binding.rvLandmarks.layoutManager = LinearLayoutManager(this)
        binding.rvLandmarks.adapter = adapter
    }

    private fun loadStats() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var total = 0
                var pending = 0
                var active = 0
                for (child in snapshot.children) {
                    total++
                    val status = child.child("status").getValue(String::class.java) ?: ""
                    when (status.lowercase()) {
                        "pending" -> pending++
                        "published" -> active++
                    }
                }
                binding.tvLandmarksCount.text = total.toString()
                binding.tvPendingCount.text = if (pending < 10) "0$pending" else pending.toString()
                binding.tvActiveCount.text = if (active >= 1000) "${active / 1000}.${(active % 1000) / 100}k" else active.toString()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun loadLandmarks() {
        binding.progressBarLandmarks.visibility = View.VISIBLE
        binding.rvLandmarks.visibility = View.GONE

        database.limitToFirst(5).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allLandmarks.clear()
                for (child in snapshot.children) {
                    val landmark = child.getValue(LandmarkAdminModel::class.java)
                    if (landmark != null) {
                        landmark.id = child.key ?: ""
                        allLandmarks.add(landmark)
                    }
                }

                binding.tvShowingCount.text = "Showing ${allLandmarks.size} of ${snapshot.childrenCount}"
                adapter.notifyDataSetChanged()

                binding.progressBarLandmarks.visibility = View.GONE
                binding.rvLandmarks.visibility = View.VISIBLE
            }


            override fun onCancelled(error: DatabaseError) {
                binding.progressBarLandmarks.visibility = View.GONE
                binding.rvLandmarks.visibility = View.VISIBLE
                Toast.makeText(this@MainPageAdminActivity, "Failed to load landmarks: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun filterLandmarks(query: String) {
        val filtered = allLandmarks.filter {
            it.name.contains(query, ignoreCase = true) || it.location.city.contains(query, ignoreCase = true)
        }
        adapter.updateList(filtered)
    }
    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.apply {
            hide(WindowInsetsCompat.Type.navigationBars())

            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
