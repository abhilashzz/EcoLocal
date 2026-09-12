package com.ecolocal.app.ui.chat

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecolocal.app.R
import com.ecolocal.app.data.ChatRepository
import com.ecolocal.app.data.ListingRepository
import com.ecolocal.app.data.NotificationRepository
import com.ecolocal.app.databinding.ActivityChatBinding
import com.ecolocal.app.ui.marketplace.MarketplaceListingDetailsActivity

class ChatActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CONVERSATION_ID = "extra_conversation_id"
    }

    private lateinit var binding: ActivityChatBinding
    private lateinit var adapter: ChatMessagesAdapter
    private var conversationId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        conversationId = intent.getStringExtra(EXTRA_CONVERSATION_ID)
        if (conversationId == null) {
            Toast.makeText(this, "Conversation not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupHeader()
        setupRecyclerView()
        setupInput()
        setupClickListeners()
        loadMessages()
    }

    override fun onResume() {
        super.onResume()
        conversationId?.let {
            ChatRepository.markConversationAsRead(it)
        }
        loadMessages()
    }

    private fun setupHeader() {
        val id = conversationId ?: return
        val conv = ChatRepository.getConversationById(id) ?: return

        binding.tvChatHeaderName.text = conv.participantName
        binding.tvChatHeaderStatus.text = "Active • ${conv.participantRole}"

        if (conv.participantAvatarRes != 0) {
            binding.ivChatHeaderAvatar.setImageResource(conv.participantAvatarRes)
        } else {
            binding.ivChatHeaderAvatar.setImageResource(R.drawable.img_avatar_woman)
        }

        if (conv.listingTitle != null) {
            binding.layoutChatListingBanner.visibility = View.VISIBLE
            binding.tvChatListingContext.text = "Regarding: ${conv.listingTitle}"
            binding.layoutChatListingBanner.setOnClickListener {
                conv.listingId?.let { lId ->
                    val listing = ListingRepository.getById(lId)
                    if (listing != null) {
                        val intent = Intent(this, MarketplaceListingDetailsActivity::class.java).apply {
                            putExtra(MarketplaceListingDetailsActivity.EXTRA_LISTING_ID, lId)
                        }
                        startActivity(intent)
                    }
                }
            }
        } else {
            binding.layoutChatListingBanner.visibility = View.GONE
        }
    }

    private fun setupRecyclerView() {
        adapter = ChatMessagesAdapter(emptyList())
        val layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.rvChatMessages.layoutManager = layoutManager
        binding.rvChatMessages.adapter = adapter
    }

    private fun loadMessages() {
        val id = conversationId ?: return
        val messages = ChatRepository.getMessages(id)
        adapter.updateData(messages)
        if (messages.isNotEmpty()) {
            binding.rvChatMessages.scrollToPosition(messages.size - 1)
        }
    }

    private fun setupInput() {
        binding.btnChatSend.setOnClickListener {
            sendMessage()
        }

        binding.etChatInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else {
                false
            }
        }
    }

    private fun sendMessage() {
        val text = binding.etChatInput.text.toString().trim()
        if (text.isEmpty()) return

        val id = conversationId ?: return
        val conv = ChatRepository.getConversationById(id)

        ChatRepository.sendMessage(id, text, isFromMe = true)
        binding.etChatInput.setText("")

        NotificationRepository.addNotification(
            title = "Message Sent",
            message = "To ${conv?.participantName ?: "Neighbor"}: \"$text\"",
            type = "CHAT",
            targetConversationId = id
        )

        loadMessages()
    }

    private fun setupClickListeners() {
        binding.btnChatBack.setOnClickListener {
            finish()
        }
    }
}
