package com.ecolocal.app.ui.chat

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecolocal.app.data.ChatRepository
import com.ecolocal.app.databinding.ActivityChatsBinding

class ChatsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatsBinding
    private lateinit var adapter: ConversationsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupClickListeners()
        loadConversations()
    }

    override fun onResume() {
        super.onResume()
        loadConversations()
    }

    private fun setupRecyclerView() {
        adapter = ConversationsAdapter(emptyList()) { conv ->
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra(ChatActivity.EXTRA_CONVERSATION_ID, conv.conversationId)
            }
            startActivity(intent)
        }
        binding.rvChatsList.layoutManager = LinearLayoutManager(this)
        binding.rvChatsList.adapter = adapter
    }

    private fun loadConversations() {
        val list = ChatRepository.getAllConversations()
        if (list.isEmpty()) {
            binding.rvChatsList.visibility = View.GONE
            binding.layoutEmptyChats.visibility = View.VISIBLE
        } else {
            binding.rvChatsList.visibility = View.VISIBLE
            binding.layoutEmptyChats.visibility = View.GONE
            adapter.updateData(list)
        }
    }

    private fun setupClickListeners() {
        binding.btnChatsBack.setOnClickListener {
            finish()
        }
    }
}
