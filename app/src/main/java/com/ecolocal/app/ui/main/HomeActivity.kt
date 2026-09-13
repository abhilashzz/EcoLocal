package com.ecolocal.app.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecolocal.app.R
import com.ecolocal.app.adapter.EcoMatchAdapter
import com.ecolocal.app.adapter.HomeMarketCategoryAdapter
import com.ecolocal.app.adapter.HomeServiceCategoryAdapter
import com.ecolocal.app.adapter.NearbyListingAdapter
import com.ecolocal.app.data.ChatRepository
import com.ecolocal.app.data.ListingRepository
import com.ecolocal.app.data.UserRepository
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
import com.ecolocal.app.ui.profile.ProfileActivity
import com.ecolocal.app.util.AppPreferences
import com.ecolocal.app.util.BottomNavHelper
import com.ecolocal.app.util.EcoMatchEngine
import com.ecolocal.app.util.ImageLoaderHelper
import com.ecolocal.app.util.LocationHelper
import com.ecolocal.app.util.NavItem
import com.google.firebase.auth.FirebaseAuth

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var nearbyAdapter: NearbyListingAdapter
    private lateinit var ecoMatchAdapter: EcoMatchAdapter
    private lateinit var chatsAdapter: ConversationsAdapter

    private var deviceLat: Double? = null
    private var deviceLon: Double? = null

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()
        setupSearch()
        setupCommunityServicesSection()
        setupLocalMarketplaceSection()
        setupEcoMatchSection()
        setupNearbyRecentSection()
        setupRecentChatsSection()
        setupClickListeners()
        updateHeaderUserInfo()
        requestNotificationPermissionSafely()
        fetchDeviceLocation()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        resetScrollToTop()
    }

    private val homeDataObserver = {
        runOnUiThread {
            refreshNearbyRecentListings()
            refreshEcoMatch()
            refreshRecentChats()
            updateHeaderUserInfo()
        }
    }

    override fun onStart() {
        super.onStart()
        ListingRepository.addChangeListener(homeDataObserver)
        ChatRepository.addConversationObserver(homeDataObserver)
        UserRepository.addProfileChangeListener(homeDataObserver)
        fetchDeviceLocation()
        refreshNearbyRecentListings()
        refreshEcoMatch()
        refreshRecentChats()
        updateHeaderUserInfo()
    }

    override fun onResume() {
        super.onResume()
        fetchDeviceLocation()
        refreshNearbyRecentListings()
        refreshEcoMatch()
        refreshRecentChats()
        updateHeaderUserInfo()
    }

    override fun onStop() {
        super.onStop()
        ListingRepository.removeChangeListener(homeDataObserver)
        ChatRepository.removeConversationObserver(homeDataObserver)
        UserRepository.removeProfileChangeListener(homeDataObserver)
    }

    private fun fetchDeviceLocation() {
        LocationHelper.getDeviceLocation(this) { loc ->
            if (loc != null) {
                deviceLat = loc.latitude
                deviceLon = loc.longitude
                runOnUiThread {
                    refreshNearbyRecentListings()
                    refreshEcoMatch()
                }
            }
        }
    }

    private fun updateHeaderUserInfo() {
        val user = UserRepository.getCurrentUser()
        val firstName = user?.fullName?.split(" ")?.firstOrNull()?.takeIf { it.isNotBlank() } ?: "Neighbor"
        binding.tvGreeting.text = "Good morning, $firstName"
        if (!user?.locationText.isNullOrBlank()) {
            binding.tvLocation.text = user?.locationText
        } else if (!user?.city.isNullOrBlank()) {
            binding.tvLocation.text = user?.city
        } else {
            binding.tvLocation.text = "Sri Lanka"
        }
        ImageLoaderHelper.loadAvatar(binding.ivHomeAvatar, user?.profileImageUrl, R.drawable.img_avatar_nimal)
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
        val launchSearch = {
            val query = binding.etSearchHome.text.toString().trim()
            val intent = Intent(this, SearchResultsActivity::class.java).apply {
                putExtra(SearchResultsActivity.EXTRA_QUERY, query)
            }
            startActivity(intent)
        }

        binding.etSearchHome.setOnClickListener {
            launchSearch()
        }

        binding.etSearchHome.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                launchSearch()
                binding.etSearchHome.clearFocus()
            }
        }

        binding.etSearchHome.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                launchSearch()
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

    private fun setupEcoMatchSection() {
        binding.rvEcomatch.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        ecoMatchAdapter = EcoMatchAdapter(emptyList()) { rec ->
            val intent = Intent(this, MarketplaceListingDetailsActivity::class.java).apply {
                putExtra(MarketplaceListingDetailsActivity.EXTRA_LISTING_ID, rec.listing.listingId)
            }
            startActivity(intent)
        }
        binding.rvEcomatch.adapter = ecoMatchAdapter
        refreshEcoMatch()
    }

    private fun refreshEcoMatch() {
        val allListings = ListingRepository.getAll()
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        val userProfile = UserRepository.getCurrentUser()

        val recommendations = EcoMatchEngine.getRecommendations(
            allListings = allListings,
            currentUserId = currentUid,
            userLat = deviceLat,
            userLon = deviceLon,
            userLocationName = userProfile?.locationText,
            limit = 5
        )
        if (::ecoMatchAdapter.isInitialized) {
            ecoMatchAdapter.updateData(recommendations)
        }
    }

    private fun buildCombinedNearbyListings(): List<NearbyListing> {
        val all = ListingRepository.getAll()
        val isLocationOn = AppPreferences.isLocationSuggestionsEnabled()

        val itemsWithDistance = all.map { item ->
            val coords = LocationHelper.getListingCoordinates(item)
            val distKm = if (isLocationOn && coords != null && deviceLat != null && deviceLon != null) {
                LocationHelper.calculateDistanceKm(deviceLat!!, deviceLon!!, coords.first, coords.second)
            } else null

            val distFormatted = LocationHelper.formatDistance(distKm)
            val displayLoc = if (distFormatted != null) "${item.locationName} • $distFormatted" else item.locationName

            val isOrange = item.price.equals("FREE", ignoreCase = true) || item.listingType.equals("GIVE AWAY", ignoreCase = true)
            val status = if (isOrange) "Giveaway" else "Available"

            Triple(
                NearbyListing(
                    id = item.listingId,
                    title = item.title,
                    description = item.description,
                    price = item.price,
                    isPriceOrange = isOrange,
                    location = displayLoc,
                    statusText = status,
                    isStatusAvailable = item.isAvailable,
                    imageRes = item.imageRes,
                    imageUri = item.imageUrl ?: item.imageUri
                ),
                distKm,
                item.createdAt
            )
        }

        val sorted = if (isLocationOn && deviceLat != null && deviceLon != null) {
            // Prioritize nearby items (< 25km) first by proximity, then others by date
            val nearby = itemsWithDistance.filter { it.second != null && it.second!! <= LocationHelper.NEARBY_RADIUS_KM }
                .sortedBy { it.second }
            val others = itemsWithDistance.filter { it.second == null || it.second!! > LocationHelper.NEARBY_RADIUS_KM }
                .sortedByDescending { it.third }
            nearby + others
        } else {
            itemsWithDistance.sortedByDescending { it.third }
        }

        return sorted.map { it.first }
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
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
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
