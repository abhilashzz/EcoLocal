package com.ecolocal.app.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ecolocal.app.databinding.ItemChatMessageBinding
import com.ecolocal.app.model.entity.MessageEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatMessagesAdapter(
    private var messages: List<MessageEntity>
) : RecyclerView.Adapter<ChatMessagesAdapter.ViewHolder>() {

    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    fun updateData(newMessages: List<MessageEntity>) {
        messages = newMessages
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: ItemChatMessageBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemChatMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = messages.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = messages[position]
        val formattedTime = timeFormat.format(Date(item.timestamp))

        if (item.isFromMe) {
            holder.binding.layoutMsgOutgoing.visibility = View.VISIBLE
            holder.binding.layoutMsgIncoming.visibility = View.GONE
            holder.binding.tvOutgoingText.text = item.text
            holder.binding.tvOutgoingTime.text = formattedTime
        } else {
            holder.binding.layoutMsgOutgoing.visibility = View.GONE
            holder.binding.layoutMsgIncoming.visibility = View.VISIBLE
            holder.binding.tvIncomingText.text = item.text
            holder.binding.tvIncomingTime.text = formattedTime
        }
    }
}
