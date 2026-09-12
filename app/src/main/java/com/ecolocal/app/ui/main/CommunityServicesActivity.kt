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
import com.ecolocal.app.databinding.ActivityCommunityServicesBinding
import com.ecolocal.app.model.CommunityServiceItem
import com.ecolocal.app.ui.chat.ChatActivity
import com.ecolocal.app.ui.notifications.NotificationsActivity
import com.ecolocal.app.util.BottomNavHelper
import com.ecolocal.app.util.NavItem

class CommunityServicesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCommunityServicesBinding
    private lateinit var adapter: CommunityServicesAdapter

    private var selectedCategory: String = "All"
    private var selectedTab: String = "ALL" // "ALL" (Offers/Board) or "REQUESTS"
    private var searchQuery: String = ""

    private val allItems = listOf(
        CommunityServiceItem.Offer(
            id = "srv_1",
            title = "Science & Maths Tutor",
            location = "Malabe",
            price = "Rs. 1,500/session",
            status = "Available",
            imageRes = R.drawable.img_service_tutor,
            providerName = "Kamal Fernando",
            category = "Tutoring"
        ),
        CommunityServiceItem.Request(
            id = "srv_2",
            title = "Need help fixing a leaking tap",
            location = "Kaduwela",
            timeText = "Needed this week",
            requesterName = "Saman Kumara",
            category = "Repairs",
            imageRes = R.drawable.img_service_lawn
        ),
        CommunityServiceItem.Offer(
            id = "srv_3",
            title = "Weekend Lawn Mowing",
            location = "Rajagiriya",
            price = "Rs. 800/hr",
            status = "Available",
            imageRes = R.drawable.img_service_lawn,
            providerName = "Nadeesha Silva",
            category = "Gardening"
        ),
        CommunityServiceItem.Request(
            id = "srv_4",
            title = "Physics A/L revision guidance",
            location = "Malabe",
            timeText = "Needed urgently",
            requesterName = "Chamara Perera",
            category = "Tutoring",
            imageRes = R.drawable.img_service_tutor
        ),
        CommunityServiceItem.Offer(
            id = "srv_5",
            title = "Electric Fan & Appliance Repairs",
            location = "Battaramulla",
            price = "Rs. 1,000/job",
            status = "Available",
            imageRes = R.drawable.img_service_tutor,
            providerName = "Sunil Wickrama",
            category = "Repairs"
        ),
        CommunityServiceItem.Request(
            id = "srv_6",
            title = "Garden weeding and clearing help",
            location = "Athurugiriya",
            timeText = "This Saturday",
            requesterName = "Priya Jayasuriya",
            category = "Gardening",
            imageRes = R.drawable.img_service_lawn
        ),
        CommunityServiceItem.Offer(
            id = "srv_7",
            title = "English Conversational Practice",
            location = "Nugegoda",
            price = "FREE",
            status = "Available",
            imageRes = R.drawable.img_service_tutor,
            providerName = "Sanduni Alwis",
            category = "Tutoring"
        ),
        CommunityServiceItem.Request(
            id = "srv_8",
            title = "Bicycle brake repair assistance",
            location = "Kaduwela",
            timeText = "Flexible",
            requesterName = "Dinesh Bandara",
            category = "Repairs",
            imageRes = R.drawable.img_service_lawn
        ),
        CommunityServiceItem.Offer(
            id = "srv_9",
            title = "Organic Home Compost Supply",
            location = "Malabe",
            price = "FREE",
            status = "Available",
            imageRes = R.drawable.img_service_lawn,
            providerName = "Ranjith Silva",
            category = "Gardening"
        ),
        CommunityServiceItem.Request(
            id = "srv_10",
            title = "Moving heavy boxes to upstairs store",
            location = "Rajagiriya",
            timeText = "Tomorrow evening",
            requesterName = "Kasun Fernando",
            category = "Volunteering",
            imageRes = R.drawable.img_service_lawn
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommunityServicesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()
        setupServicesList()
        setupFilterChips()
        setupSegmentedControl()
        setupSearch()
        setupClickListeners()
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
        adapter = CommunityServicesAdapter(
            items = allItems,
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
            selectedTab = "ALL"
            binding.tabServiceOffers.setBackgroundResource(R.drawable.bg_segmented_tab_selected)
            binding.tabServiceOffers.setTextColor(ContextCompat.getColor(this, R.color.eco_text_primary))
            binding.tabHelpRequests.background = null
            binding.tabHelpRequests.setTextColor(ContextCompat.getColor(this, R.color.eco_text_secondary))
            applyFilters()
        }

        binding.tabHelpRequests.setOnClickListener {
            selectedTab = "REQUESTS"
            binding.tabHelpRequests.setBackgroundResource(R.drawable.bg_segmented_tab_selected)
            binding.tabHelpRequests.setTextColor(ContextCompat.getColor(this, R.color.eco_text_primary))
            binding.tabServiceOffers.background = null
            binding.tabServiceOffers.setTextColor(ContextCompat.getColor(this, R.color.eco_text_secondary))
            applyFilters()
        }
    }

    private fun applyFilters() {
        val filtered = allItems.filter { item ->
            // Tab filter
            val matchesTab = when (selectedTab) {
                "REQUESTS" -> item is CommunityServiceItem.Request
                else -> true // "ALL" shows offers and requests together as in Figma
            }

            // Category filter
            val itemCategory = when (item) {
                is CommunityServiceItem.Offer -> item.category
                is CommunityServiceItem.Request -> item.category
            }
            val matchesCategory = if (selectedCategory.equals("All", ignoreCase = true)) {
                true
            } else {
                itemCategory.equals(selectedCategory, ignoreCase = true)
            }

            // Search query filter
            val matchesSearch = if (searchQuery.isEmpty()) {
                true
            } else {
                val title = when (item) {
                    is CommunityServiceItem.Offer -> item.title
                    is CommunityServiceItem.Request -> item.title
                }
                val location = when (item) {
                    is CommunityServiceItem.Offer -> item.location
                    is CommunityServiceItem.Request -> item.location
                }
                title.contains(searchQuery, ignoreCase = true) ||
                location.contains(searchQuery, ignoreCase = true) ||
                itemCategory.contains(searchQuery, ignoreCase = true)
            }

            matchesTab && matchesCategory && matchesSearch
        }

        adapter.updateData(filtered)
    }

    private fun setupClickListeners() {
        binding.btnNotificationsServices.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }

        binding.ivServicesAvatar.setOnClickListener {
            Toast.makeText(this, "Logged in as Nimal Perera (Malabe)", Toast.LENGTH_SHORT).show()
        }
    }
}
