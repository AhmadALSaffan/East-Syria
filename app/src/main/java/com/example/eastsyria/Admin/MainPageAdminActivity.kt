package com.example.eastsyria.Admin

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eastsyria.Admin.Add.AddLandmarkActivity
import com.example.eastsyria.Admin.Data.AdminLandmarkAdapter
import com.example.eastsyria.Admin.Data.LandmarkAdminModel
import com.example.eastsyria.Admin.Edit.EditLandmarkActivity
import com.example.eastsyria.Admin.Reports.ReportsActivity
import com.example.eastsyria.Login.LoginActivity
import com.example.eastsyria.R
import com.example.eastsyria.Ui.SkeletonPulse
import com.example.eastsyria.Ui.warmEdgeToEdge
import com.example.eastsyria.databinding.ActivityMainPageAdminBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainPageAdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainPageAdminBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var adapter: AdminLandmarkAdapter
    private lateinit var pulse: SkeletonPulse
    private val allLandmarks = mutableListOf<LandmarkAdminModel>()

    private var listener: ValueEventListener? = null
    private var loaded = false

    private enum class State { LOADING, LIST, EMPTY, NO_MATCH, ERROR }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        warmEdgeToEdge()
        binding = ActivityMainPageAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)
        pulse = SkeletonPulse(binding.loadingState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("landmarks")

        setupInsets()
        setupHeader()
        setupRecyclerView()
        setupClickListeners()
        loadLandmarks()
    }

    override fun onDestroy() {
        pulse.stop()
        listener?.let { database.removeEventListener(it) }
        super.onDestroy()
    }

    private fun setupInsets() {
        val base = binding.adminContent.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.adminContent) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.updatePadding(bottom = base + bars.bottom)
            insets
        }
    }

    private fun setupHeader() {
        binding.header.setIconA(R.drawable.ic_logout, getString(R.string.admin_sign_out)) { signOut() }
    }

    private fun setupRecyclerView() {
        adapter = AdminLandmarkAdapter(
            allLandmarks,
            onEdit = { landmark ->
                startActivity(
                    Intent(this, EditLandmarkActivity::class.java).putExtra("landmarkId", landmark.id)
                )
            },
            onDelete = { landmark -> confirmDelete(landmark) }
        )
        binding.rvLandmarks.layoutManager = LinearLayoutManager(this)
        binding.rvLandmarks.adapter = adapter
        binding.rvLandmarks.isNestedScrollingEnabled = false
    }

    private fun setupClickListeners() {
        binding.btnAddLandmark.setOnClickListener {
            startActivity(Intent(this, AddLandmarkActivity::class.java))
        }
        binding.reportsBtn.setOnClickListener {
            startActivity(Intent(this, ReportsActivity::class.java))
        }
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                showList()
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    // ------------------------------------------------------------ data

    /** One live read of the whole catalog: it feeds the figures and the list together. */
    private fun loadLandmarks() {
        loaded = false
        showState(State.LOADING)
        listener?.let { database.removeEventListener(it) }

        val newListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var published = 0
                var pending = 0
                allLandmarks.clear()

                for (child in snapshot.children) {
                    if (child.getValue(Any::class.java) == null) continue
                    val landmark = try {
                        child.getValue(LandmarkAdminModel::class.java)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing landmark ${child.key}: ${e.message}")
                        null
                    } ?: continue
                    landmark.id = child.key ?: ""
                    allLandmarks.add(landmark)
                    when (landmark.status.lowercase()) {
                        "published" -> published++
                        "pending" -> pending++
                    }
                }
                allLandmarks.sortBy { it.name.lowercase() }

                binding.tvLandmarksCount.text = allLandmarks.size.toString()
                binding.tvActiveCount.text = published.toString()
                binding.tvPendingCount.text = pending.toString()

                loaded = true
                adapter.updateList(allLandmarks)
                showList()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to load landmarks: ${error.message}")
                showState(State.ERROR)
            }
        }
        listener = newListener
        database.addValueEventListener(newListener)
    }

    private fun showList() {
        if (!loaded) return
        val query = binding.etSearch.text.toString().trim()
        val filtered = if (query.isEmpty()) {
            allLandmarks
        } else {
            allLandmarks.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.location.city.contains(query, ignoreCase = true)
            }
        }
        adapter.updateList(filtered)

        binding.tvShowingCount.text = if (query.isEmpty()) {
            resources.getQuantityString(R.plurals.search_count_all, filtered.size, filtered.size)
        } else {
            resources.getQuantityString(R.plurals.search_count_query, filtered.size, filtered.size, query)
        }

        showState(
            when {
                allLandmarks.isEmpty() -> State.EMPTY
                filtered.isEmpty() -> State.NO_MATCH
                else -> State.LIST
            }
        )
    }

    // ------------------------------------------------------------ delete

    /** Deleting removes a landmark for every visitor, so it is confirmed and reversible. */
    private fun confirmDelete(landmark: LandmarkAdminModel) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.admin_delete_title, landmark.name))
            .setMessage(R.string.admin_delete_body)
            .setNegativeButton(R.string.admin_delete_cancel, null)
            .setPositiveButton(R.string.admin_delete_confirm) { _, _ -> deleteLandmark(landmark) }
            .show()
    }

    private fun deleteLandmark(landmark: LandmarkAdminModel) {
        val ref = database.child(landmark.id)
        // Keep the record as stored so Undo can put back every field, not just the ones this screen shows.
        ref.get().addOnSuccessListener { snapshot ->
            val backup = snapshot.value
            ref.removeValue()
                .addOnSuccessListener {
                    Snackbar.make(binding.root, R.string.admin_deleted, Snackbar.LENGTH_LONG)
                        .styled()
                        .setAction(R.string.saved_undo) {
                            ref.setValue(backup).addOnSuccessListener {
                                Snackbar.make(binding.root, R.string.admin_restored, Snackbar.LENGTH_SHORT)
                                    .styled()
                                    .show()
                            }
                        }
                        .show()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to delete ${landmark.id}: ${e.message}")
                    Snackbar.make(binding.root, R.string.admin_delete_failed, Snackbar.LENGTH_LONG).styled().show()
                }
        }.addOnFailureListener {
            Snackbar.make(binding.root, R.string.admin_delete_failed, Snackbar.LENGTH_LONG).styled().show()
        }
    }

    private fun Snackbar.styled(): Snackbar = this
        .setBackgroundTint(ContextCompat.getColor(this@MainPageAdminActivity, R.color.surface_high))
        .setTextColor(ContextCompat.getColor(this@MainPageAdminActivity, R.color.ivory))
        .setActionTextColor(ContextCompat.getColor(this@MainPageAdminActivity, R.color.terracotta_soft))

    // ------------------------------------------------------------ states

    private fun showState(state: State) {
        val list = state == State.LIST
        binding.loadingState.visibility = if (state == State.LOADING) View.VISIBLE else View.GONE
        binding.rvLandmarks.visibility = if (list) View.VISIBLE else View.GONE
        binding.tvShowingCount.visibility =
            if (list || state == State.NO_MATCH) View.VISIBLE else View.GONE
        binding.statePanel.hide()

        when (state) {
            State.EMPTY -> binding.statePanel.show(
                getString(R.string.admin_empty_title),
                getString(R.string.admin_empty_body),
                getString(R.string.admin_add_landmark)
            ) { startActivity(Intent(this, AddLandmarkActivity::class.java)) }

            State.NO_MATCH -> binding.statePanel.show(
                getString(R.string.search_empty_title),
                getString(R.string.admin_no_match, binding.etSearch.text.toString().trim()),
                getString(R.string.search_clear)
            ) { binding.etSearch.text.clear() }

            State.ERROR -> binding.statePanel.show(
                getString(R.string.admin_error_title),
                getString(R.string.search_error_body),
                getString(R.string.action_retry)
            ) { loadLandmarks() }

            else -> Unit
        }

        if (state == State.LOADING) pulse.start() else pulse.stop()
    }

    private fun signOut() {
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

    companion object {
        private const val TAG = "MainPageAdminActivity"
    }
}
