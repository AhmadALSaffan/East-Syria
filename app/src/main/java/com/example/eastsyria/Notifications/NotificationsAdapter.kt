package com.example.eastsyria.Notifications

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.eastsyria.Notifications.Data.Notification
import com.example.eastsyria.Notifications.Data.NotificationType
import com.example.eastsyria.R

class NotificationsAdapter(
    private val onNotificationClick: (Notification) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val notifications = mutableListOf<NotificationItem>()

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_NOTIFICATION = 1
    }

    sealed class NotificationItem {
        data class Header(val title: String) : NotificationItem()
        data class NotificationData(val notification: Notification) : NotificationItem()
    }

    fun submitList(newNotifications: List<Notification>) {
        notifications.clear()

        val sortedNotifications = newNotifications.sortedByDescending { it.timestamp }
        val now = System.currentTimeMillis()
        val oneDayAgo = now - (24 * 60 * 60 * 1000)

        val recent = sortedNotifications.filter { it.timestamp > oneDayAgo }
        val earlier = sortedNotifications.filter { it.timestamp <= oneDayAgo }

        if (recent.isNotEmpty()) {
            notifications.add(NotificationItem.Header("RECENT"))
            notifications.addAll(recent.map { NotificationItem.NotificationData(it) })
        }

        if (earlier.isNotEmpty()) {
            notifications.add(NotificationItem.Header("EARLIER"))
            notifications.addAll(earlier.map { NotificationItem.NotificationData(it) })
        }

        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (notifications[position]) {
            is NotificationItem.Header -> VIEW_TYPE_HEADER
            is NotificationItem.NotificationData -> VIEW_TYPE_NOTIFICATION
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val view = inflater.inflate(R.layout.item_notification, parent, false)
                HeaderViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.item_notification, parent, false)
                NotificationViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = notifications[position]) {
            is NotificationItem.Header -> {
                (holder as HeaderViewHolder).bind(item.title)
            }
            is NotificationItem.NotificationData -> {
                (holder as NotificationViewHolder).bind(item.notification, onNotificationClick)
            }
        }
    }

    override fun getItemCount() = notifications.size

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvSectionHeader: TextView = view.findViewById(R.id.tvSectionHeader)

        init {
            view.findViewById<View>(R.id.notificationContainer).visibility = View.GONE
            view.findViewById<View>(R.id.featuredCard).visibility = View.GONE
        }

        fun bind(title: String) {
            tvSectionHeader.visibility = View.VISIBLE
            tvSectionHeader.text = title
        }
    }

    class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val ivIcon: ImageView = view.findViewById(R.id.ivNotificationIcon)
        private val tvTitle: TextView = view.findViewById(R.id.tvNotificationTitle)
        private val tvDescription: TextView = view.findViewById(R.id.tvNotificationDescription)
        private val tvTime: TextView = view.findViewById(R.id.tvNotificationTime)
        private val unreadIndicator: View = view.findViewById(R.id.unreadIndicator)
        private val featuredCard: CardView = view.findViewById(R.id.featuredCard)
        private val ivFeaturedImage: ImageView = view.findViewById(R.id.ivFeaturedImage)
        private val tvFeaturedTitle: TextView = view.findViewById(R.id.tvFeaturedTitle)

        init {
            view.findViewById<TextView>(R.id.tvSectionHeader).visibility = View.GONE
        }

        fun bind(notification: Notification, onClick: (Notification) -> Unit) {
            tvTitle.text = notification.title
            tvDescription.text = notification.description
            tvTime.text = getRelativeTime(notification.timestamp)
            unreadIndicator.visibility = if (notification.isRead) View.GONE else View.VISIBLE


            val iconRes = when (notification.toNotificationType()) {
                NotificationType.LANDMARK -> R.drawable.ic_map
                NotificationType.SITE_UPDATE -> R.drawable.ic_camera
                NotificationType.EVENT -> R.drawable.ic_calendar
                NotificationType.SYSTEM -> R.drawable.ic_flash
            }
            ivIcon.setImageResource(iconRes)


            if (notification.isFeatured && !notification.imageUrl.isNullOrEmpty()) {
                featuredCard.visibility = View.VISIBLE
                tvFeaturedTitle.text = notification.title

                Glide.with(itemView.context)
                    .load(notification.imageUrl)
                    .placeholder(R.drawable.placeholder_landmark)
                    .centerCrop()
                    .into(ivFeaturedImage)
            } else {
                featuredCard.visibility = View.GONE
            }

            itemView.setOnClickListener { onClick(notification) }
        }


        private fun getRelativeTime(timestamp: Long): String {
            val diff = System.currentTimeMillis() - timestamp
            val seconds = diff / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24

            return when {
                seconds < 60 -> "${seconds}s ago"
                minutes < 60 -> "${minutes}m ago"
                hours < 24 -> "${hours}h ago"
                days == 1L -> "Yesterday"
                days < 7 -> "${days} days ago"
                else -> "${days / 7} weeks ago"
            }
        }
    }
}
