package com.example.eastsyria.Saved

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eastsyria.Details.LandmarkDetailActivity
import com.example.eastsyria.Login.LoginActivity
import com.example.eastsyria.MainPage.Data.Landmark
import com.example.eastsyria.MainPage.MainPageActivity
import com.example.eastsyria.Nav.NavDock
import com.example.eastsyria.R
import com.example.eastsyria.Ui.SkeletonPulse
import com.example.eastsyria.Ui.padBottomForInsets
import com.example.eastsyria.Ui.warmEdgeToEdge
import com.example.eastsyria.databinding.ActivitySavedLandmarksBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SavedLandmarksActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySavedLandmarksBinding
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adapter: SavedLandmarkAdapter
    private lateinit var pulse: SkeletonPulse

    private val savedLandmarks = mutableListOf<Landmark>()

    private var savedRef: DatabaseReference? = null
    private var savedListener: ValueEventListener? = null

    private enum class State { LOADING, LIST, EMPTY, SIGNED_OUT, ERROR }

    companion object {
        private const val TAG = "SavedLandmarksActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        warmEdgeToEdge()
        binding = ActivitySavedLandmarksBinding.inflate(layoutInflater)
        setContentView(binding.root)
        pulse = SkeletonPulse(binding.loadingState)

        binding.header.onBack { finish() }
        binding.navDock.attach(this, NavDock.TAB_SAVED)
        binding.rvSavedLandmarks.padBottomForInsets(extraDp = 128)

        setupRecyclerView()
        setupSearch()
        loadSavedLandmarks()
    }

    override fun onDestroy() {
        pulse.stop()
        detachListener()
        super.onDestroy()
    }

    private fun setupRecyclerView() {
        adapter = SavedLandmarkAdapter(
            savedLandmarks,
            onItemClick = { landmark -> openLandmarkDetail(landmark) },
            onBookmarkClick = { landmark -> removeBookmark(landmark) }
        )

        binding.rvSavedLandmarks.apply {
            adapter = this@SavedLandmarksActivity.adapter
            layoutManager = LinearLayoutManager(this@SavedLandmarksActivity)
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter(s.toString())
                showList()
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    // ------------------------------------------------------------ data

    private fun loadSavedLandmarks() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            showState(State.SIGNED_OUT)
            return
        }

        showState(State.LOADING)
        detachListener()

        // The saved ids, live: removing a bookmark anywhere updates this list.
        val ref = database.reference.child("users").child(userId).child("savedLandmarks")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val savedAt = snapshot.children.mapNotNull { child ->
                    child.key?.let { it to (child.child("savedAt").getValue(Long::class.java) ?: 0L) }
                }.toMap()

                if (savedAt.isEmpty()) {
                    savedLandmarks.clear()
                    adapter.updateData(savedLandmarks)
                    showState(State.EMPTY)
                    return
                }
                fetchLandmarks(savedAt)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error loading saved landmarks: ${error.message}")
                showState(State.ERROR)
            }
        }
        savedRef = ref
        savedListener = listener
        ref.addValueEventListener(listener)
    }

    /** One read of the catalog, matched against the saved ids, rather than one read per landmark. */
    private fun fetchLandmarks(savedAt: Map<String, Long>) {
        database.reference.child("landmarks")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val found = snapshot.children.mapNotNull { child ->
                        val key = child.key ?: return@mapNotNull null
                        if (key !in savedAt) return@mapNotNull null
                        child.getValue(Landmark::class.java)?.also { it.id = key }
                    }.filter { it.status == "published" }
                        .sortedByDescending { savedAt[it.id] ?: 0L }

                    savedLandmarks.clear()
                    savedLandmarks.addAll(found)
                    adapter.updateData(savedLandmarks)
                    if (savedLandmarks.isEmpty()) showState(State.EMPTY) else showList()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error loading catalog: ${error.message}")
                    showState(State.ERROR)
                }
            })
    }

    private fun detachListener() {
        val ref = savedRef
        val listener = savedListener
        if (ref != null && listener != null) ref.removeEventListener(listener)
        savedRef = null
        savedListener = null
    }

    private fun openLandmarkDetail(landmark: Landmark) {
        val intent = Intent(this, LandmarkDetailActivity::class.java)
        intent.putExtra("LANDMARK_ID", landmark.id)
        startActivity(intent)
    }

    /** Removes the bookmark and offers to bring it straight back. */
    private fun removeBookmark(landmark: Landmark) {
        val userId = auth.currentUser?.uid ?: return
        val ref = database.reference
            .child("users").child(userId).child("savedLandmarks").child(landmark.id)

        ref.removeValue()
            .addOnSuccessListener {
                Snackbar.make(binding.root, R.string.saved_removed, Snackbar.LENGTH_LONG)
                    .setBackgroundTint(ContextCompat.getColor(this, R.color.surface_high))
                    .setTextColor(ContextCompat.getColor(this, R.color.ivory))
                    .setActionTextColor(ContextCompat.getColor(this, R.color.terracotta_soft))
                    .setAction(R.string.saved_undo) {
                        ref.setValue(
                            mapOf(
                                "savedAt" to System.currentTimeMillis(),
                                "landmarkId" to landmark.id
                            )
                        )
                    }
                    .show()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to remove saved landmark: ${e.message}")
                Snackbar.make(binding.root, R.string.saved_remove_failed, Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(ContextCompat.getColor(this, R.color.surface_high))
                    .setTextColor(ContextCompat.getColor(this, R.color.ivory))
                    .show()
            }
    }

    // ------------------------------------------------------------ states

    /** The list itself: with matches, or with the search having none. */
    private fun showList() {
        val query = binding.etSearch.text.toString().trim()
        if (adapter.getFilteredCount() == 0 && query.isNotEmpty()) {
            showState(State.LIST)
            binding.rvSavedLandmarks.visibility = View.GONE
            binding.statePanel.show(
                getString(R.string.search_empty_title),
                getString(R.string.saved_no_match, query),
                getString(R.string.search_clear)
            ) { binding.etSearch.text.clear() }
            binding.tvCount.visibility = View.GONE
        } else {
            showState(State.LIST)
        }
    }

    private fun showState(state: State) {
        val list = state == State.LIST
        binding.loadingState.visibility = if (state == State.LOADING) View.VISIBLE else View.GONE
        binding.rvSavedLandmarks.visibility = if (list) View.VISIBLE else View.GONE
        binding.searchPill.visibility = if (list) View.VISIBLE else View.GONE
        binding.tvCount.visibility = if (list) View.VISIBLE else View.GONE
        binding.statePanel.hide()

        when (state) {
            State.LIST -> {
                val count = adapter.getFilteredCount()
                binding.tvCount.text = resources.getQuantityString(R.plurals.saved_count, count, count)
            }

            State.EMPTY -> binding.statePanel.show(
                getString(R.string.saved_empty_title),
                getString(R.string.saved_empty_body),
                getString(R.string.saved_empty_action)
            ) {
                startActivity(
                    Intent(this, MainPageActivity::class.java).apply {
                        putExtra(MainPageActivity.EXTRA_SELECT_EXPLORE, true)
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    }
                )
            }

            State.SIGNED_OUT -> binding.statePanel.show(
                getString(R.string.saved_signed_out_title),
                getString(R.string.saved_signed_out_body),
                getString(R.string.saved_signed_out_action)
            ) { startActivity(Intent(this, LoginActivity::class.java)) }

            State.ERROR -> binding.statePanel.show(
                getString(R.string.saved_error_title),
                getString(R.string.search_error_body),
                getString(R.string.action_retry)
            ) { loadSavedLandmarks() }

            State.LOADING -> Unit
        }

        if (state == State.LOADING) pulse.start() else pulse.stop()
    }
}
