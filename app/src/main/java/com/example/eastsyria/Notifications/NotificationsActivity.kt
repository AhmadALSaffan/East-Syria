package com.example.eastsyria.Notifications

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eastsyria.Details.LandmarkDetailActivity
import com.example.eastsyria.Login.LoginActivity
import com.example.eastsyria.Notifications.Data.Notification
import com.example.eastsyria.Nav.NavDock
import com.example.eastsyria.R
import com.example.eastsyria.Ui.SkeletonPulse
import com.example.eastsyria.Ui.padBottomForInsets
import com.example.eastsyria.Ui.warmEdgeToEdge
import com.example.eastsyria.databinding.ActivityNotificationsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class NotificationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationsBinding
    private lateinit var notificationsAdapter: NotificationsAdapter
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var pulse: SkeletonPulse

    private var notificationsRef: DatabaseReference? = null
    private var notificationsListener: ValueEventListener? = null
    private val notifications = mutableListOf<Notification>()

    private enum class State { LOADING, LIST, EMPTY, SIGNED_OUT, ERROR }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        warmEdgeToEdge()
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        pulse = SkeletonPulse(binding.loadingState)

        database = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()

        binding.header.onBack { finish() }
        binding.navDock.attach(this, NavDock.TAB_UPDATES)
        binding.rvNotifications.padBottomForInsets(extraDp = 128)
        setupRecyclerView()

        if (auth.currentUser == null) {
            showState(State.SIGNED_OUT)
            return
        }
        loadNotifications()
    }

    override fun onDestroy() {
        pulse.stop()
        val ref = notificationsRef
        val listener = notificationsListener
        if (ref != null && listener != null) ref.removeEventListener(listener)
        super.onDestroy()
    }

    private fun setupRecyclerView() {
        notificationsAdapter = NotificationsAdapter { notification ->
            handleNotificationClick(notification)
        }
        binding.rvNotifications.apply {
            layoutManager = LinearLayoutManager(this@NotificationsActivity)
            adapter = notificationsAdapter
        }
    }

    // ------------------------------------------------------------ data

    /** Live, so an update read on another screen or device is reflected here. */
    private fun loadNotifications() {
        val userId = auth.currentUser?.uid ?: return
        showState(State.LOADING)

        val ref = database.child("notifications").child(userId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                notifications.clear()
                for (child in snapshot.children) {
                    parseNotification(child)?.let { notifications.add(it) }
                }
                notifications.sortByDescending { it.timestamp }
                notificationsAdapter.submitList(notifications.toList())
                showState(if (notifications.isEmpty()) State.EMPTY else State.LIST)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to load notifications: ${error.message}")
                showState(State.ERROR)
            }
        }
        notificationsRef = ref
        notificationsListener = listener
        ref.addValueEventListener(listener)
    }

    /** Parsed by hand: the stored shape has drifted, so getValue() is not trusted. */
    private fun parseNotification(snapshot: DataSnapshot): Notification? {
        val id = snapshot.key ?: return null
        return Notification().apply {
            this.id = id
            type = snapshot.child("type").getValue(String::class.java) ?: ""
            title = snapshot.child("title").getValue(String::class.java) ?: ""
            description = snapshot.child("description").getValue(String::class.java) ?: ""
            timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L
            isRead = snapshot.child("isRead").getValue(Boolean::class.java) ?: false
            imageUrl = snapshot.child("imageUrl").getValue(String::class.java) ?: ""
            isFeatured = snapshot.child("isFeatured").getValue(Boolean::class.java) ?: false
            relatedId = snapshot.child("relatedId").getValue(String::class.java) ?: ""
            openedAt = snapshot.child("openedAt").getValue(Long::class.java) ?: 0L
        }
    }

    private fun handleNotificationClick(notification: Notification) {
        markNotificationAsOpened(notification.id)

        when (notification.type) {
            "LANDMARK", "SITE_UPDATE" -> {
                if (notification.relatedId.isNotEmpty() && notification.relatedId != "null") {
                    startActivity(
                        Intent(this, LandmarkDetailActivity::class.java)
                            .putExtra("LANDMARK_ID", notification.relatedId)
                    )
                } else {
                    showDialog(notification)
                }
            }

            // Events and system messages have no screen of their own: read them here.
            else -> showDialog(notification)
        }
    }

    private fun showDialog(notification: Notification) {
        MaterialAlertDialogBuilder(this)
            .setTitle(notification.title)
            .setMessage(notification.description)
            .setPositiveButton(R.string.updates_dialog_close, null)
            .show()
    }

    private fun markNotificationAsOpened(notificationId: String) {
        val userId = auth.currentUser?.uid ?: return
        if (notificationId.isEmpty()) return

        database.child("notifications/$userId/$notificationId")
            .updateChildren(mapOf("isRead" to true, "openedAt" to System.currentTimeMillis()))
            .addOnFailureListener { error ->
                Log.e(TAG, "Failed to mark as opened: ${error.message}")
            }
        // The live listener redraws the row, so nothing to patch by hand.
    }

    private fun markAllNotificationsAsRead() {
        val userId = auth.currentUser?.uid ?: return

        val now = System.currentTimeMillis()
        val updates = mutableMapOf<String, Any>()
        notifications.filter { !it.isRead }.forEach { notification ->
            updates["notifications/$userId/${notification.id}/isRead"] = true
            updates["notifications/$userId/${notification.id}/openedAt"] = now
        }
        if (updates.isEmpty()) return

        database.updateChildren(updates)
            .addOnSuccessListener { snack(R.string.updates_all_read) }
            .addOnFailureListener { error ->
                Log.e(TAG, "Failed to mark all as read: ${error.message}")
                snack(R.string.updates_mark_failed)
            }
    }

    // ------------------------------------------------------------ states

    private fun showState(state: State) {
        val list = state == State.LIST
        binding.loadingState.visibility = if (state == State.LOADING) View.VISIBLE else View.GONE
        binding.rvNotifications.visibility = if (list) View.VISIBLE else View.GONE
        binding.statePanel.hide()

        // "Mark all as read" only when there is something to mark.
        val unread = notifications.count { !it.isRead }
        if (list && unread > 0) {
            binding.header.setAction(getString(R.string.updates_mark_all)) { markAllNotificationsAsRead() }
        } else {
            binding.header.setAction(null)
        }

        when (state) {
            State.EMPTY -> binding.statePanel.show(
                getString(R.string.updates_empty_title),
                getString(R.string.updates_empty_body)
            )

            State.SIGNED_OUT -> binding.statePanel.show(
                getString(R.string.updates_signed_out_title),
                getString(R.string.updates_signed_out_body),
                getString(R.string.saved_signed_out_action)
            ) { startActivity(Intent(this, LoginActivity::class.java)) }

            State.ERROR -> binding.statePanel.show(
                getString(R.string.updates_error_title),
                getString(R.string.search_error_body),
                getString(R.string.action_retry)
            ) { loadNotifications() }

            else -> Unit
        }

        if (state == State.LOADING) pulse.start() else pulse.stop()
    }

    private fun snack(message: Int) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
            .setBackgroundTint(ContextCompat.getColor(this, R.color.ivory))
            .setTextColor(ContextCompat.getColor(this, R.color.canvas))
            .show()
    }

    companion object {
        private const val TAG = "NotificationsActivity"
    }
}
