package com.example.eastsyria.Notifications.Data

data class Notification(
    var id: String = "",
    var type: String = "",
    var title: String = "",
    var description: String = "",
    var timestamp: Long = 0L,
    var isRead: Boolean = false,
    var imageUrl: String = "",
    var isFeatured: Boolean = false,
    var relatedId: String = "",
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
