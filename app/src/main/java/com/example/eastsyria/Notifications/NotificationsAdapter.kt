package com.example.eastsyria.Notifications

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.eastsyria.Notifications.Data.Notification
import com.example.eastsyria.Notifications.Data.NotificationType
import com.example.eastsyria.R
import com.example.eastsyria.databinding.ItemNotificationBinding
import com.example.eastsyria.databinding.ItemNotificationHeaderBinding

/**
 * Updates grouped into Recent (the last day) and Earlier. Headers and rows are
 * separate layouts; the old version inflated one layout for both and hid half
 * of it.
 */
class NotificationsAdapter(
    private val onNotificationClick: (Notification) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val notifications = mutableListOf<NotificationItem>()

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_NOTIFICATION = 1
    }

    sealed class NotificationItem {
        data class Header(val titleRes: Int) : NotificationItem()
        data class NotificationData(val notification: Notification) : NotificationItem()
    }

    fun submitList(newNotifications: List<Notification>) {
        notifications.clear()

        val sorted = newNotifications.sortedByDescending { it.timestamp }
        val oneDayAgo = System.currentTimeMillis() - DateUtils.DAY_IN_MILLIS

        val recent = sorted.filter { it.timestamp > oneDayAgo }
        val earlier = sorted.filter { it.timestamp <= oneDayAgo }

        if (recent.isNotEmpty()) {
            notifications.add(NotificationItem.Header(R.string.updates_recent))
            notifications.addAll(recent.map { NotificationItem.NotificationData(it) })
        }
        if (earlier.isNotEmpty()) {
            notifications.add(NotificationItem.Header(R.string.updates_earlier))
            notifications.addAll(earlier.map { NotificationItem.NotificationData(it) })
        }

        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int = when (notifications[position]) {
        is NotificationItem.Header -> VIEW_TYPE_HEADER
        is NotificationItem.NotificationData -> VIEW_TYPE_NOTIFICATION
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_HEADER) {
            HeaderViewHolder(ItemNotificationHeaderBinding.inflate(inflater, parent, false))
        } else {
            NotificationViewHolder(ItemNotificationBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = notifications[position]) {
            is NotificationItem.Header -> (holder as HeaderViewHolder).bind(item.titleRes)
            is NotificationItem.NotificationData ->
                (holder as NotificationViewHolder).bind(item.notification, onNotificationClick)
        }
    }

    override fun getItemCount() = notifications.size

    class HeaderViewHolder(private val binding: ItemNotificationHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(titleRes: Int) {
            binding.tvSectionHeader.setText(titleRes)
        }
    }

    class NotificationViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: Notification, onClick: (Notification) -> Unit) {
            val context = binding.root.context
            binding.apply {
                tvNotificationTitle.text = notification.title
                tvNotificationDescription.text = notification.description
                tvNotificationDescription.visibility =
                    if (notification.description.isBlank()) View.GONE else View.VISIBLE

                // A real, localised relative time, rather than hand-built "5m ago".
                tvNotificationTime.text = DateUtils.getRelativeTimeSpanString(
                    notification.timestamp,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
                )

                // Read updates recede; unread ones keep full weight and a dot.
                unreadIndicator.visibility = if (notification.isRead) View.GONE else View.VISIBLE
                tvNotificationTitle.setTextColor(
                    ContextCompat.getColor(
                        context,
                        if (notification.isRead) R.color.ivory_dim else R.color.ivory
                    )
                )

                ivNotificationIcon.setImageResource(
                    when (notification.toNotificationType()) {
                        NotificationType.LANDMARK -> R.drawable.ic_map
                        NotificationType.SITE_UPDATE -> R.drawable.ic_camera
                        NotificationType.EVENT -> R.drawable.ic_calendar
                        NotificationType.SYSTEM -> R.drawable.ic_flash
                    }
                )

                if (notification.isFeatured && notification.imageUrl.isNotBlank()) {
                    featuredCard.visibility = View.VISIBLE
                    tvFeaturedTitle.text = notification.title
                    Glide.with(context)
                        .load(notification.imageUrl)
                        .placeholder(R.drawable.placeholder_dark)
                        .error(R.drawable.placeholder_dark)
                        .centerCrop()
                        .transition(DrawableTransitionOptions.withCrossFade(200))
                        .into(ivFeaturedImage)
                } else {
                    featuredCard.visibility = View.GONE
                }

                root.setOnClickListener { onClick(notification) }
            }
        }
    }
}
