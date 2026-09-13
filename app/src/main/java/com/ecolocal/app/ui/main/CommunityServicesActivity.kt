package com.ecolocal.app.ui.main

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecolocal.app.R
import com.ecolocal.app.adapter.CommunityServicesAdapter
import com.ecolocal.app.data.ChatRepository
import com.ecolocal.app.data.NotificationRepository
import com.ecolocal.app.data.RequestRepository
import com.ecolocal.app.data.ServiceRepository
import com.ecolocal.app.data.UserRepository
import com.ecolocal.app.databinding.ActivityCommunityServicesBinding
import com.ecolocal.app.model.CommunityServiceItem
import com.ecolocal.app.ui.chat.ChatActivity
import com.ecolocal.app.ui.notifications.NotificationsActivity
import com.ecolocal.app.util.BottomNavHelper
import com.ecolocal.app.util.ImageLoaderHelper
import com.ecolocal.app.util.NavItem

class CommunityServicesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCommunityServicesBinding
    private lateinit var adapter: CommunityServicesAdapter

    private var selectedCategory: String = "All"
    private var selectedServiceType: String = "SERVICE_OFFER"
    private var searchQuery: String = ""

    private val servicesChangeListener = {
        runOnUiThread {
            applyFilters()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommunityServicesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ServiceRepository.init(this)

        setupBottomNavigation()
        setupServicesList()
        setupFilterChips()
        setupSegmentedControl()
        setupSearch()
        setupClickListeners()

        handleInitialTab(intent)
    }

    private fun handleInitialTab(intent: Intent?) {
        val initialTab = intent?.getStringExtra(EXTRA_INITIAL_TAB)
        if (initialTab == "HELP_REQUESTS" || initialTab == "HELP_REQUEST") {
            binding.tabHelpRequests.performClick()
        } else if (initialTab == "SERVICE_OFFERS" || initialTab == "SERVICE_OFFER") {
            binding.tabServiceOffers.performClick()
        }
    }

    override fun onStart() {
        super.onStart()
        ServiceRepository.addChangeListener(servicesChangeListener)
        loadUserAvatar()
        applyFilters()
    }

    override fun onResume() {
        super.onResume()
        loadUserAvatar()
        applyFilters()
    }

    private fun loadUserAvatar() {
        val user = UserRepository.getCurrentUser()
        ImageLoaderHelper.loadAvatar(
            binding.ivServicesAvatar,
            user?.profileImageUrl,
            user?.avatarRes ?: R.drawable.img_avatar_nimal
        )
    }

    override fun onStop() {
        super.onStop()
        ServiceRepository.removeChangeListener(servicesChangeListener)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        resetScrollToTop()
    }

    private fun setupBottomNavigation() {
        BottomNavHelper.setup(this, binding.bottomNavBar.root, NavItem.SERVICES) {
            resetScrollToTop()
        }
    }

    private fun resetScrollToTop() {
        binding.nestedScrollViewServices.scrollTo(0, 0)
        binding.nestedScrollViewServices.post {
            binding.nestedScrollViewServices.scrollTo(0, 0)
        }
    }

    private fun setupServicesList() {
        val initialItems = ServiceRepository.searchAndFilter(searchQuery, selectedCategory, selectedServiceType)
        adapter = CommunityServicesAdapter(
            items = initialItems,
            onItemClick = { item ->
                handleItemClick(item)
            },
            onOfferHelpClick = { request ->
                handleOfferHelp(request)
            }
        )
        binding.rvCommunityServicesList.layoutManager = LinearLayoutManager(this)
        binding.rvCommunityServicesList.adapter = adapter
    }

    private fun handleOfferHelp(request: CommunityServiceItem.Request) {
        val created = RequestRepository.createServiceHelp(
            serviceId = request.id,
            serviceTitle = request.title,
            imageRes = request.imageRes,
            ownerName = request.requesterName,
            location = request.location,
            category = request.category
        )

        if (created) {
            NotificationRepository.addNotification(
                title = "Help Offer Submitted",
                message = "You offered help for \"${request.title}\" to ${request.requesterName}.",
                type = "COMMUNITY"
            )
            Toast.makeText(this, "Offer sent! View in My Activity > Requests", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "You already offered help for this request.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleItemClick(item: CommunityServiceItem) {
        when (item) {
            is CommunityServiceItem.Offer -> {
                // Open Chat with provider
                val conv = ChatRepository.getOrCreateConversation(
                    participantName = item.providerName,
                    participantAvatarRes = R.drawable.img_avatar_woman,
                    participantRole = "Service Provider",
                    listingId = item.id,
                    listingTitle = item.title
                )
                val intent = Intent(this, ChatActivity::class.java).apply {
                    putExtra(ChatActivity.EXTRA_CONVERSATION_ID, conv.conversationId)
                }
                startActivity(intent)
            }
            is CommunityServiceItem.Request -> {
                handleOfferHelp(item)
            }
        }
    }

    private fun setupSearch() {
        binding.etSearchServices.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString()?.trim().orEmpty()
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupFilterChips() {
        val chips = listOf(
            binding.chipServicesAll to "All",
            binding.chipServicesTutoring to "Tutoring",
            binding.chipServicesRepairs to "Repairs",
            binding.chipServicesGardening to "Gardening"
        )

        fun selectChip(selectedView: TextView, category: String) {
            selectedCategory = category
            chips.forEach { (chip, _) ->
                if (chip == selectedView) {
                    chip.setBackgroundResource(R.drawable.bg_chip_selected)
                    chip.setTextColor(ContextCompat.getColor(this, R.color.eco_chip_selected_text))
                } else {
                    chip.setBackgroundResource(R.drawable.bg_chip_unselected)
                    chip.setTextColor(ContextCompat.getColor(this, R.color.eco_chip_unselected_text))
                }
            }
            applyFilters()
        }

        binding.chipServicesAll.setOnClickListener { selectChip(it as TextView, "All") }
        binding.chipServicesTutoring.setOnClickListener { selectChip(it as TextView, "Tutoring") }
        binding.chipServicesRepairs.setOnClickListener { selectChip(it as TextView, "Repairs") }
        binding.chipServicesGardening.setOnClickListener { selectChip(it as TextView, "Gardening") }
    }

    private fun setupSegmentedControl() {
        binding.tabServiceOffers.setOnClickListener {
            selectedServiceType = "SERVICE_OFFER"
            binding.tabServiceOffers.setBackgroundResource(R.drawable.bg_segmented_tab_selected)
            binding.tabServiceOffers.setTextColor(ContextCompat.getColor(this, R.color.eco_text_primary))
            binding.tabHelpRequests.background = null
            binding.tabHelpRequests.setTextColor(ContextCompat.getColor(this, R.color.eco_text_secondary))
            applyFilters()
        }

        binding.tabHelpRequests.setOnClickListener {
            selectedServiceType = "HELP_REQUEST"
            binding.tabHelpRequests.setBackgroundResource(R.drawable.bg_segmented_tab_selected)
            binding.tabHelpRequests.setTextColor(ContextCompat.getColor(this, R.color.eco_text_primary))
            binding.tabServiceOffers.background = null
            binding.tabServiceOffers.setTextColor(ContextCompat.getColor(this, R.color.eco_text_secondary))
            applyFilters()
        }
    }

    private fun applyFilters() {
        val filtered = ServiceRepository.searchAndFilter(searchQuery, selectedCategory, selectedServiceType)
        if (::adapter.isInitialized) {
            adapter.updateData(filtered)
        }
    }

    private fun setupClickListeners() {
        binding.btnNotificationsServices.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }

        binding.ivServicesAvatar.setOnClickListener {
            startActivity(Intent(this, com.ecolocal.app.ui.profile.ProfileActivity::class.java))
        }
    }

    companion object {
        const val EXTRA_INITIAL_TAB = "extra_initial_tab"
    }
}
