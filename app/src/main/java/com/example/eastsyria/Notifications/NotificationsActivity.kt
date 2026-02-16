package com.example.eastsyria.Notifications

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.eastsyria.Details.LandmarkDetailActivity
import com.example.eastsyria.MainPage.MainPageActivity
import com.example.eastsyria.Notifications.Data.Notification
import com.example.eastsyria.R
import com.example.eastsyria.Saved.SavedLandmarksActivity
import com.example.eastsyria.databinding.ActivityNotificationsBinding
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

    private var notificationsListener: ValueEventListener? = null
    private val notifications = mutableListOf<Notification>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()

        val currentUser = auth.currentUser
        Log.d(TAG, "Current user: ${currentUser?.uid}")

        if (currentUser == null) {
            Toast.makeText(this, "Not logged in!", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupViews()
        setupRecyclerView()
        setupBottomNavigation()
        loadNotifications()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume called - refreshing notifications")
        refreshNotifications()
    }

    private fun setupViews() {
        binding.tvMarkAllRead.setOnClickListener {
            markAllNotificationsAsRead()
        }

        binding.fabMap.setOnClickListener {
            Toast.makeText(this, "Map feature coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        notificationsAdapter = NotificationsAdapter { notification ->
            handleNotificationClick(notification)
        }

        binding.rvNotifications.apply {
            layoutManager = LinearLayoutManager(this@NotificationsActivity)
            adapter = notificationsAdapter

            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    outRect.bottom = 8
                }
            })
        }

        Log.d(TAG, "RecyclerView setup complete")
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.selectedItemId = R.id.nav_updates

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_explore -> {
                    startActivity(Intent(this, MainPageActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_saved -> {
                    startActivity(Intent(this, SavedLandmarksActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_updates -> {
                    true
                }
                R.id.nav_profile -> {
                    Toast.makeText(this, "Profile feature coming soon", Toast.LENGTH_SHORT).show()
                    false
                }
                else -> false
            }
        }
    }

    private fun loadNotifications() {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            Log.e(TAG, "User ID is null - user not logged in!")
            return
        }

        Log.d(TAG, "Loading notifications for user: $userId")
        Log.d(TAG, "Listening to path: notifications/$userId")

        notificationsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(TAG, "========== FIREBASE DATA RECEIVED ==========")
                Log.d(TAG, "Snapshot exists: ${snapshot.exists()}")
                Log.d(TAG, "Snapshot children count: ${snapshot.childrenCount}")

                notifications.clear()

                for (notificationSnapshot in snapshot.children) {
                    val notificationId = notificationSnapshot.key ?: continue
                    val isReadFromFirebase = notificationSnapshot.child("isRead").getValue(Boolean::class.java) ?: false

                    Log.d(TAG, "---")
                    Log.d(TAG, "Notification ID: $notificationId")
                    Log.d(TAG, "isRead from Firebase: $isReadFromFirebase")

                    try {
                        val notification = notificationSnapshot.getValue(Notification::class.java)

                        if (notification != null) {
                            val notificationWithId = notification.copy(id = notificationId)

                            Log.d(TAG, "Title: ${notificationWithId.title}")
                            Log.d(TAG, "isRead in object: ${notificationWithId.isRead}")
                            Log.d(TAG, "Dot visibility: ${if (notificationWithId.isRead) "HIDDEN (GONE)" else "VISIBLE"}")

                            notifications.add(notificationWithId)
                        } else {
                            Log.e(TAG, "Failed to parse notification: $notificationId")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing notification $notificationId: ${e.message}", e)
                    }
                }

                notifications.sortByDescending { it.timestamp }

                Log.d(TAG, "========== TOTAL: ${notifications.size} notifications ==========")

                updateNotificationsList()
                updateUnreadCount()
                updateEmptyState()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to load notifications: ${error.message}")
                Toast.makeText(
                    this@NotificationsActivity,
                    "Failed to load notifications: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        database.child("notifications")
            .child(userId)
            .addValueEventListener(notificationsListener!!)
    }

    private fun refreshNotifications() {
        val userId = auth.currentUser?.uid ?: return

        Log.d(TAG, "========== REFRESHING NOTIFICATIONS ==========")

        database.child("notifications")
            .child(userId)
            .get()
            .addOnSuccessListener { snapshot ->
                Log.d(TAG, "Refresh successful - ${snapshot.childrenCount} notifications")

                notifications.clear()

                for (notificationSnapshot in snapshot.children) {
                    val notificationId = notificationSnapshot.key ?: continue
                    val isReadFromFirebase = notificationSnapshot.child("isRead").getValue(Boolean::class.java) ?: false

                    Log.d(TAG, "Refresh check - $notificationId: isRead=$isReadFromFirebase")

                    val notification = notificationSnapshot.getValue(Notification::class.java)
                    if (notification != null) {
                        val notificationWithId = notification.copy(id = notificationId)
                        notifications.add(notificationWithId)
                    }
                }

                notifications.sortByDescending { it.timestamp }
                updateNotificationsList()
                updateUnreadCount()

                Log.d(TAG, "========== REFRESH COMPLETE ==========")
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Failed to refresh: ${error.message}")
            }
    }

    private fun handleNotificationClick(notification: Notification) {
        Log.d(TAG, "=== Notification Clicked ===")
        Log.d(TAG, "ID: ${notification.id}")
        Log.d(TAG, "Title: ${notification.title}")
        Log.d(TAG, "Type: ${notification.type}")
        Log.d(TAG, "Current isRead: ${notification.isRead}")

        markNotificationAsOpened(notification.id)

        when (notification.type) {
            "LANDMARK", "SITE_UPDATE" -> {
                if (!notification.relatedId.isNullOrEmpty() && notification.relatedId != "null") {
                    navigateToLandmark(notification.relatedId)
                } else {
                    Toast.makeText(
                        this,
                        "No landmark associated with this notification",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            "EVENT" -> {
                Toast.makeText(
                    this,
                    "Event: ${notification.title}\n${notification.description}",
                    Toast.LENGTH_LONG
                ).show()
            }
            "SYSTEM" -> {
                showSystemNotificationDialog(notification)
            }
            else -> {
                Log.w(TAG, "Unknown notification type: ${notification.type}")
                Toast.makeText(this, notification.title, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun markNotificationAsOpened(notificationId: String) {
        if (notificationId.isEmpty()) {
            Log.e(TAG, "Cannot mark notification as opened - empty ID")
            return
        }

        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e(TAG, "Cannot mark notification as opened - user not logged in")
            return
        }

        val currentTime = System.currentTimeMillis()
        val path = "notifications/$userId/$notificationId"

        Log.d(TAG, "Marking notification as opened: $notificationId at $currentTime")

        val updates = mapOf(
            "isRead" to true,
            "openedAt" to currentTime
        )

        database.child(path)
            .updateChildren(updates)
            .addOnSuccessListener {
                Log.d(TAG, "✅ Successfully marked notification as opened: $notificationId")

                val index = notifications.indexOfFirst { it.id == notificationId }
                if (index != -1) {
                    notifications[index] = notifications[index].copy(
                        isRead = true,
                        openedAt = currentTime
                    )
                    updateNotificationsList()
                    updateUnreadCount()
                }
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "❌ Failed to mark as opened: ${error.message}")
            }
    }

    private fun navigateToLandmark(landmarkId: String) {
        Log.d(TAG, "Navigating to landmark: $landmarkId")

        val intent = Intent(this, LandmarkDetailActivity::class.java)
        intent.putExtra("LANDMARK_ID", landmarkId)
        startActivity(intent)
    }

    private fun showSystemNotificationDialog(notification: Notification) {
        AlertDialog.Builder(this)
            .setTitle(notification.title)
            .setMessage(notification.description)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }

    private fun markAllNotificationsAsRead() {
        val userId = auth.currentUser?.uid ?: return

        Log.d(TAG, "Marking all notifications as read")

        val updates = mutableMapOf<String, Any>()
        val currentTime = System.currentTimeMillis()

        notifications.filter { !it.isRead }.forEach { notification ->
            updates["notifications/$userId/${notification.id}/isRead"] = true
            updates["notifications/$userId/${notification.id}/openedAt"] = currentTime
        }

        if (updates.isEmpty()) {
            Toast.makeText(this, "All notifications already read", Toast.LENGTH_SHORT).show()
            return
        }

        database.updateChildren(updates)
            .addOnSuccessListener {
                Log.d(TAG, "Successfully marked all as read")
                Toast.makeText(this, "All marked as read", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Failed to mark all as read: ${error.message}")
                Toast.makeText(this, "Failed to update", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateNotificationsList() {
        Log.d(TAG, "Updating adapter with ${notifications.size} notifications")
        notificationsAdapter.submitList(notifications.toList())
    }

    private fun updateUnreadCount() {
        val unreadCount = notifications.count { !it.isRead }
        binding.tvMarkAllRead.visibility = if (unreadCount > 0) View.VISIBLE else View.GONE

        Log.d(TAG, "Unread count: $unreadCount")
    }

    private fun updateEmptyState() {
        if (notifications.isEmpty()) {
            binding.rvNotifications.visibility = View.GONE
        } else {
            binding.rvNotifications.visibility = View.VISIBLE
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        notificationsListener?.let {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                database.child("notifications").child(userId).removeEventListener(it)
                Log.d(TAG, "Removed notifications listener")
            }
        }
    }

    companion object {
        private const val TAG = "NotificationsActivity"
    }
}
