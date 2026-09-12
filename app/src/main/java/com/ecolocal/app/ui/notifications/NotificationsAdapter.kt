package com.ecolocal.app.ui.notifications

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ecolocal.app.R
import com.ecolocal.app.databinding.ItemNotificationBinding
import com.ecolocal.app.model.entity.NotificationEntity

class NotificationsAdapter(
    private var notifications: List<NotificationEntity>,
    private val onItemClick: (NotificationEntity) -> Unit
) : RecyclerView.Adapter<NotificationsAdapter.ViewHolder>() {

    fun updateData(newNotifications: List<NotificationEntity>) {
        notifications = newNotifications
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = notifications.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = notifications[position]
        val context = holder.itemView.context

        holder.binding.tvNotifTitle.text = item.title
        holder.binding.tvNotifMessage.text = item.message
        holder.binding.tvNotifTime.text = formatTimestamp(item.timestamp)

        // Read state
        holder.binding.ivNotifUnreadDot.visibility = if (item.isRead) View.GONE else View.VISIBLE

        // Type icon
        val iconRes = when (item.type.uppercase()) {
            "MARKETPLACE" -> R.drawable.ic_nav_market
            "COMMUNITY" -> R.drawable.ic_hand_help
            "CHAT" -> R.drawable.ic_chat
            "SAVED" -> R.drawable.ic_bookmark
            else -> R.drawable.ic_bell
        }
        holder.binding.ivNotifIcon.setImageResource(iconRes)

        // Card background styling
        if (item.isRead) {
            holder.binding.cardNotification.setCardBackgroundColor(
                ContextCompat.getColor(context, R.color.eco_background_white)
            )
        } else {
            holder.binding.cardNotification.setCardBackgroundColor(
                ContextCompat.getColor(context, R.color.eco_background_white)
            )
        }

        holder.binding.root.setOnClickListener {
            onItemClick(item)
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        return when {
            diff < 60_000L -> "Just now"
            diff < 3600_000L -> "${diff / 60_000L}m ago"
            diff < 86400_000L -> "${diff / 3600_000L}h ago"
            else -> "${diff / 86400_000L}d ago"
        }
    }
}
