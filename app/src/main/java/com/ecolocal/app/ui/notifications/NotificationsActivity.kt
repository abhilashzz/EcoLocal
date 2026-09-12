package com.ecolocal.app.ui.notifications

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecolocal.app.data.ListingRepository
import com.ecolocal.app.data.NotificationRepository
import com.ecolocal.app.databinding.ActivityNotificationsBinding
import com.ecolocal.app.model.entity.NotificationEntity
import com.ecolocal.app.ui.activity.MyActivityActivity
import com.ecolocal.app.ui.chat.ChatActivity
import com.ecolocal.app.ui.marketplace.MarketplaceListingDetailsActivity

class NotificationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationsBinding
    private lateinit var adapter: NotificationsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupClickListeners()
        loadNotifications()
    }

    override fun onResume() {
        super.onResume()
        loadNotifications()
    }

    private fun setupRecyclerView() {
        adapter = NotificationsAdapter(emptyList()) { item ->
            handleNotificationClick(item)
        }
        binding.rvNotificationsList.layoutManager = LinearLayoutManager(this)
        binding.rvNotificationsList.adapter = adapter
    }

    private fun loadNotifications() {
        val list = NotificationRepository.getAll()
        if (list.isEmpty()) {
            binding.rvNotificationsList.visibility = View.GONE
            binding.layoutEmptyNotifications.visibility = View.VISIBLE
        } else {
            binding.rvNotificationsList.visibility = View.VISIBLE
            binding.layoutEmptyNotifications.visibility = View.GONE
            adapter.updateData(list)
        }
    }

    private fun handleNotificationClick(item: NotificationEntity) {
        NotificationRepository.markAsRead(item.id)
        loadNotifications()

        when {
            item.targetListingId != null -> {
                val listing = ListingRepository.getById(item.targetListingId)
                if (listing != null) {
                    val intent = Intent(this, MarketplaceListingDetailsActivity::class.java).apply {
                        putExtra(MarketplaceListingDetailsActivity.EXTRA_LISTING_ID, listing.id)
                    }
                    startActivity(intent)
                } else {
                    Toast.makeText(this, item.message, Toast.LENGTH_SHORT).show()
                }
            }
            item.targetConversationId != null -> {
                val intent = Intent(this, ChatActivity::class.java).apply {
                    putExtra(ChatActivity.EXTRA_CONVERSATION_ID, item.targetConversationId)
                }
                startActivity(intent)
            }
            item.type == "SAVED" || item.type == "COMMUNITY" -> {
                val intent = Intent(this, MyActivityActivity::class.java)
                startActivity(intent)
            }
            else -> {
                Toast.makeText(this, item.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnNotificationsBack.setOnClickListener {
            finish()
        }

        binding.btnNotificationsMarkAllRead.setOnClickListener {
            NotificationRepository.markAllAsRead()
            loadNotifications()
            Toast.makeText(this, "All notifications marked as read", Toast.LENGTH_SHORT).show()
        }
    }
}
