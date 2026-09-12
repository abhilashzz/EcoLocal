package com.ecolocal.app.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ecolocal.app.R
import com.ecolocal.app.databinding.ItemConversationBinding
import com.ecolocal.app.model.entity.ConversationEntity

class ConversationsAdapter(
    private var conversations: List<ConversationEntity>,
    private val onItemClick: (ConversationEntity) -> Unit
) : RecyclerView.Adapter<ConversationsAdapter.ViewHolder>() {

    fun updateData(newConversations: List<ConversationEntity>) {
        conversations = newConversations
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: ItemConversationBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemConversationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = conversations.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = conversations[position]

        holder.binding.tvConvName.text = item.participantName
        holder.binding.tvConvLastMessage.text = item.lastMessage
        holder.binding.tvConvTime.text = formatTimestamp(item.lastMessageTimestamp)

        if (item.listingTitle != null) {
            holder.binding.tvConvSubtitle.text = "Re: ${item.listingTitle}"
            holder.binding.tvConvSubtitle.visibility = View.VISIBLE
        } else {
            holder.binding.tvConvSubtitle.text = item.participantRole
            holder.binding.tvConvSubtitle.visibility = View.VISIBLE
        }

        if (item.participantAvatarRes != 0) {
            holder.binding.ivConvAvatar.setImageResource(item.participantAvatarRes)
        } else {
            holder.binding.ivConvAvatar.setImageResource(R.drawable.img_avatar_woman)
        }

        if (item.unreadCount > 0) {
            holder.binding.tvConvUnreadBadge.text = item.unreadCount.toString()
            holder.binding.tvConvUnreadBadge.visibility = View.VISIBLE
        } else {
            holder.binding.tvConvUnreadBadge.visibility = View.GONE
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
