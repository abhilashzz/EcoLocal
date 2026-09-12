package com.ecolocal.app.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecolocal.app.R
import com.ecolocal.app.adapter.HomeMarketCategoryAdapter
import com.ecolocal.app.adapter.HomeServiceCategoryAdapter
import com.ecolocal.app.adapter.NearbyListingAdapter
import com.ecolocal.app.data.ChatRepository
import com.ecolocal.app.data.ListingRepository
import com.ecolocal.app.databinding.ActivityHomeBinding
import com.ecolocal.app.model.MarketCategory
import com.ecolocal.app.model.NearbyListing
import com.ecolocal.app.model.ServiceCategory
import com.ecolocal.app.ui.chat.ChatActivity
import com.ecolocal.app.ui.chat.ChatsActivity
import com.ecolocal.app.ui.chat.ConversationsAdapter
import com.ecolocal.app.ui.marketplace.MarketplaceListingDetailsActivity
import com.ecolocal.app.ui.notifications.NotificationsActivity
import com.ecolocal.app.ui.post.CreatePostTypeActivity
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.ecolocal.app.util.BottomNavHelper
import com.ecolocal.app.util.NavItem

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var nearbyAdapter: NearbyListingAdapter
    private lateinit var chatsAdapter: ConversationsAdapter

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // No crash regardless of grant state
    }

    private fun requestNotificationPermissionSafely() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private val staticNearbyListings = listOf(
        NearbyListing(
            id = "sample_1",
            title = "Math Tutoring – Grade 6–11",
            description = "Experienced tutor offering personalized math lessons for middle and high school students.",
            price = "Rs. 1,500/session",
            isPriceOrange = false,
            location = "Malabe",
            statusText = "Available",
            isStatusAvailable = true,
            imageRes = R.drawable.img_home_tutor
        ),
        NearbyListing(
            id = "sample_2",
            title = "Wooden Study Desk",
            description = "Solid teak desk with 3 drawers. Perfect for studying or home office. Minor scratches.",
            price = "Rs. 8,500",
            isPriceOrange = false,
            location = "Malabe",
            statusText = "Available",
            isStatusAvailable = true,
            imageRes = R.drawable.img_home_desk
        ),
        NearbyListing(
            id = "sample_3",
            title = "Free Textbooks (O/L & A/L)",
            description = "Clean syllabus-aligned textbooks to give away for students preparing for upcoming exams.",
            price = "FREE",
            isPriceOrange = true,
            location = "Kaduwela",
            statusText = "Giveaway",
            isStatusAvailable = false,
            imageRes = R.drawable.img_home_books
        ),
        NearbyListing(
            id = "sample_4",
            title = "Weekend Lawn Mowing Service",
            description = "Reliable lawn maintenance, trimming, and backyard clearing across Malabe and surrounding areas.",
            price = "Rs. 800/hr",
            isPriceOrange = false,
            location = "Rajagiriya",
            statusText = "Available",
            isStatusAvailable = true,
            imageRes = R.drawable.img_service_lawn
        ),
        NearbyListing(
            id = "sample_5",
            title = "1.8L Electric Rice Cooker",
            description = "Lightly used electric rice cooker with non-stick inner pot and steamer basket. In perfect condition.",
            price = "Rs. 4,500",
            isPriceOrange = false,
            location = "Battaramulla",
            statusText = "Available",
            isStatusAvailable = true,
            imageRes = R.drawable.img_mkt_ricecooker
        ),
        NearbyListing(
            id = "sample_6",
            title = "A/L Chemistry & Biology Revision",
            description = "Small group revision sessions with exam paper discussions for local syllabus students.",
            price = "Rs. 1,200/hr",
            isPriceOrange = false,
            location = "Malabe",
            statusText = "Available",
            isStatusAvailable = true,
            imageRes = R.drawable.img_service_tutor
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()
        setupSearch()
        setupCommunityServicesSection()
        setupLocalMarketplaceSection()
        setupNearbyRecentSection()
        setupRecentChatsSection()
        setupClickListeners()
        updateHeaderUserInfo()
        requestNotificationPermissionSafely()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        resetScrollToTop()
    }

    private val homeDataObserver = {
        runOnUiThread {
            refreshNearbyRecentListings()
            refreshRecentChats()
            updateHeaderUserInfo()
        }
    }

    override fun onStart() {
        super.onStart()
        ListingRepository.addChangeListener(homeDataObserver)
        ChatRepository.addConversationObserver(homeDataObserver)
        refreshNearbyRecentListings()
        refreshRecentChats()
        updateHeaderUserInfo()
    }

    override fun onResume() {
        super.onResume()
        refreshNearbyRecentListings()
        refreshRecentChats()
        updateHeaderUserInfo()
    }

    private fun updateHeaderUserInfo() {
        val user = com.ecolocal.app.data.UserRepository.getCurrentUser()
        val firstName = user?.fullName?.split(" ")?.firstOrNull()?.takeIf { it.isNotBlank() } ?: "Neighbor"
        binding.tvGreeting.text = "Good morning, $firstName"
        if (!user?.location.isNullOrEmpty()) {
            binding.tvLocation.text = user?.location
        } else {
            binding.tvLocation.text = "Location not set"
        }
    }

    override fun onStop() {
        super.onStop()
        ListingRepository.removeChangeListener(homeDataObserver)
        ChatRepository.removeConversationObserver(homeDataObserver)
    }

    private fun setupBottomNavigation() {
        BottomNavHelper.setup(this, binding.bottomNavBar.root, NavItem.HOME) {
            resetScrollToTop()
        }
    }

    private fun resetScrollToTop() {
        binding.nestedScrollView.scrollTo(0, 0)
        binding.nestedScrollView.post {
            binding.nestedScrollView.scrollTo(0, 0)
        }
    }

    private fun setupSearch() {
        binding.etSearchHome.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.etSearchHome.text.toString().trim()
                val imm = getSystemService(INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                imm?.hideSoftInputFromWindow(binding.etSearchHome.windowToken, 0)

                val intent = Intent(this, SearchResultsActivity::class.java).apply {
                    putExtra(SearchResultsActivity.EXTRA_QUERY, query)
                }
                startActivity(intent)
                true
            } else {
                false
            }
        }
    }

    private fun setupCommunityServicesSection() {
        val categories = listOf(
            ServiceCategory("1", "Tutoring", R.drawable.ic_cat_tutoring),
            ServiceCategory("2", "Repairs", R.drawable.ic_cat_repairs),
            ServiceCategory("3", "Gardening", R.drawable.ic_cat_gardening),
            ServiceCategory("4", "Volunteering", R.drawable.ic_cat_volunteering),
            ServiceCategory("5", "Delivery", R.drawable.ic_cat_delivery),
            ServiceCategory("6", "Pets", R.drawable.ic_cat_pets)
        )

        binding.rvCommunityCategories.layoutManager = GridLayoutManager(this, 3)
        binding.rvCommunityCategories.adapter = HomeServiceCategoryAdapter(categories) { cat ->
            navigateToServices()
        }
    }

    private fun setupLocalMarketplaceSection() {
        val categories = listOf(
            MarketCategory("1", "Furniture", R.drawable.ic_cat_furniture_mkt),
            MarketCategory("2", "Electronics", R.drawable.ic_cat_electronics_mkt),
            MarketCategory("3", "Books", R.drawable.ic_cat_books_mkt),
            MarketCategory("4", "Clothing", R.drawable.ic_cat_clothing_mkt),
            MarketCategory("5", "Home", R.drawable.ic_cat_home_mkt)
        )

        binding.rvMarketCategories.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvMarketCategories.adapter = HomeMarketCategoryAdapter(categories) { cat ->
            navigateToMarketplace()
        }
    }

    private fun buildCombinedNearbyListings(): List<NearbyListing> {
        return ListingRepository.getAll().map { item ->
            NearbyListing(
                id = item.id,
                title = item.title,
                description = item.description,
                price = item.price,
                isPriceOrange = item.price.equals("FREE", ignoreCase = true) || item.listingType.equals("GIVE AWAY", ignoreCase = true),
                location = item.location,
                statusText = if (item.price.equals("FREE", ignoreCase = true) || item.listingType.equals("GIVE AWAY", ignoreCase = true)) "Giveaway" else "Available",
                isStatusAvailable = item.isAvailable,
                imageRes = item.imageRes,
                imageUri = item.imageUri
            )
        }
    }

    private fun refreshNearbyRecentListings() {
        if (::nearbyAdapter.isInitialized) {
            nearbyAdapter.updateData(buildCombinedNearbyListings())
        }
    }

    private fun setupNearbyRecentSection() {
        val initialList = buildCombinedNearbyListings()
        binding.rvNearbyListings.layoutManager = LinearLayoutManager(this)
        nearbyAdapter = NearbyListingAdapter(initialList) { item ->
            val existsInRepo = ListingRepository.getById(item.id) != null
            if (existsInRepo) {
                val intent = Intent(this, MarketplaceListingDetailsActivity::class.java).apply {
                    putExtra(MarketplaceListingDetailsActivity.EXTRA_LISTING_ID, item.id)
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "Selected: ${item.title}", Toast.LENGTH_SHORT).show()
            }
        }
        binding.rvNearbyListings.adapter = nearbyAdapter
    }

    private fun setupRecentChatsSection() {
        chatsAdapter = ConversationsAdapter(emptyList()) { conv ->
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra(ChatActivity.EXTRA_CONVERSATION_ID, conv.conversationId)
            }
            startActivity(intent)
        }
        binding.rvHomeRecentChats.layoutManager = LinearLayoutManager(this)
        binding.rvHomeRecentChats.adapter = chatsAdapter
        refreshRecentChats()
    }

    private fun refreshRecentChats() {
        if (::chatsAdapter.isInitialized) {
            val conversations = ChatRepository.getAllConversations().take(2)
            chatsAdapter.updateData(conversations)
        }
    }

    private fun setupClickListeners() {
        binding.btnHomeNotifications.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }

        binding.btnViewAllChats.setOnClickListener {
            startActivity(Intent(this, ChatsActivity::class.java))
        }

        binding.btnViewAllServices.setOnClickListener {
            navigateToServices()
        }

        binding.btnBrowseMarketplace.setOnClickListener {
            navigateToMarketplace()
        }

        binding.fabCreatePost.setOnClickListener {
            val intent = Intent(this, CreatePostTypeActivity::class.java)
            startActivity(intent)
        }

        binding.ivHomeAvatar.setOnClickListener {
            val user = com.ecolocal.app.data.UserRepository.getCurrentUser()
            val name = user?.fullName ?: "Community Member"
            val loc = if (user?.location.isNullOrEmpty()) "" else " (${user?.location})"
            Toast.makeText(this, "Profile: $name$loc", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToServices() {
        val intent = Intent(this, CommunityServicesActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        overridePendingTransition(0, 0)
    }

    private fun navigateToMarketplace() {
        val intent = Intent(this, MarketplaceActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        overridePendingTransition(0, 0)
    }
}
