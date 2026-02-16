package com.example.eastsyria.Notifications

import android.util.Log
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

object NotificationManager {

    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference
    private const val TAG = "NotificationManager"

    /**
     * Send a notification to a specific user
     */
    fun sendNotification(
        userId: String,
        type: String,
        title: String,
        description: String,
        imageUrl: String? = null,
        isFeatured: Boolean = false,
        relatedId: String? = null
    ) {
        if (userId.isEmpty()) {
            Log.e(TAG, "Cannot send notification - userId is empty")
            return
        }

        val notificationId = "notif_${System.currentTimeMillis()}"
        val timestamp = System.currentTimeMillis()

        val notification = mapOf(
            "type" to type,
            "title" to title,
            "description" to description,
            "timestamp" to timestamp,
            "isRead" to false,
            "imageUrl" to (imageUrl ?: ""),  // Convert null to empty string
            "isFeatured" to isFeatured,
            "relatedId" to (relatedId ?: ""),  // Convert null to empty string
            "openedAt" to 0L
        )

        database.child("notifications")
            .child(userId)
            .child(notificationId)
            .setValue(notification)
            .addOnSuccessListener {
                Log.d(TAG, "✅ Notification sent successfully to user: $userId")
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "❌ Failed to send notification: ${error.message}")
            }
    }

    /**
     * Send a landmark notification to a user
     */
    fun sendLandmarkNotification(
        userId: String,
        landmarkId: String,
        landmarkName: String,
        imageUrl: String? = null
    ) {
        sendNotification(
            userId = userId,
            type = "LANDMARK",
            title = "New Landmark: $landmarkName",
            description = "Check out this amazing landmark!",
            imageUrl = imageUrl,
            isFeatured = false,
            relatedId = landmarkId
        )
    }

    /**
     * Send a site update notification to users who saved this landmark
     */
    fun sendSiteUpdateNotification(
        userIds: List<String>,
        landmarkId: String,
        landmarkName: String,
        updateDescription: String,
        imageUrl: String? = null
    ) {
        userIds.forEach { userId ->
            sendNotification(
                userId = userId,
                type = "SITE_UPDATE",
                title = "Update: $landmarkName",
                description = updateDescription,
                imageUrl = imageUrl,
                isFeatured = false,
                relatedId = landmarkId
            )
        }
    }

    /**
     * Send an event notification to all users
     */
    fun sendEventNotification(
        userIds: List<String>,
        eventTitle: String,
        eventDescription: String
    ) {
        userIds.forEach { userId ->
            sendNotification(
                userId = userId,
                type = "EVENT",
                title = eventTitle,
                description = eventDescription,
                imageUrl = null,
                isFeatured = false,
                relatedId = null
            )
        }
    }

    /**
     * Send a system notification to specific users
     */
    fun sendSystemNotification(
        userIds: List<String>,
        title: String,
        message: String
    ) {
        userIds.forEach { userId ->
            sendNotification(
                userId = userId,
                type = "SYSTEM",
                title = title,
                description = message,
                imageUrl = null,
                isFeatured = false,
                relatedId = null
            )
        }
    }

    /**
     * Delete a notification
     */
    fun deleteNotification(userId: String, notificationId: String) {
        database.child("notifications")
            .child(userId)
            .child(notificationId)
            .removeValue()
            .addOnSuccessListener {
                Log.d(TAG, "✅ Notification deleted: $notificationId")
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "❌ Failed to delete notification: ${error.message}")
            }
    }

    /**
     * Delete all notifications for a user
     */
    fun deleteAllNotifications(userId: String) {
        database.child("notifications")
            .child(userId)
            .removeValue()
            .addOnSuccessListener {
                Log.d(TAG, "✅ All notifications deleted for user: $userId")
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "❌ Failed to delete notifications: ${error.message}")
            }
    }
}
