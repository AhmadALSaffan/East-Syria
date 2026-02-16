package com.example.eastsyria.Notifications.Data

data class Notification(
    val id: String = "",
    val type: String = "",
    val title: String = "",
    val description: String = "",
    val timestamp: Long = 0L,
    var isRead: Boolean = false,
    val imageUrl: String = "",
    var isFeatured: Boolean = false,
    val relatedId: String = "",
    var openedAt: Long = 0L
) {
    constructor() : this("", "", "", "", 0L, false, "", false, "", 0L)

    fun toNotificationType(): NotificationType {
        return try {
            NotificationType.valueOf(type)
        } catch (e: Exception) {
            NotificationType.SYSTEM
        }
    }
}

enum class NotificationType {
    LANDMARK,
    SITE_UPDATE,
    EVENT,
    SYSTEM
}
