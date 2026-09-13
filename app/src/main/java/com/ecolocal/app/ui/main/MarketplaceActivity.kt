package com.ecolocal.app.ui.main

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecolocal.app.R
import com.ecolocal.app.adapter.MarketplaceCategoryAdapter
import com.ecolocal.app.adapter.MarketplaceProductAdapter
import com.ecolocal.app.data.ListingRepository
import com.ecolocal.app.data.UserRepository
import com.ecolocal.app.databinding.ActivityMarketplaceBinding
import com.ecolocal.app.model.MarketCategory
import com.ecolocal.app.ui.marketplace.MarketplaceListingDetailsActivity
import com.ecolocal.app.ui.notifications.NotificationsActivity
import com.ecolocal.app.ui.post.CreatePostTypeActivity
import com.ecolocal.app.ui.profile.ProfileActivity
import com.ecolocal.app.util.AppPreferences
import com.ecolocal.app.util.BottomNavHelper
import com.ecolocal.app.util.ImageLoaderHelper
import com.ecolocal.app.util.LocationHelper
import com.ecolocal.app.util.NavItem
import com.ecolocal.app.util.SmartSearchEngine

class MarketplaceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMarketplaceBinding
    private lateinit var productAdapter: MarketplaceProductAdapter
    private lateinit var categoryAdapter: MarketplaceCategoryAdapter

    private var currentFilterChip: String = "All"
    private var currentCategory: String = "All"
    private var currentQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMarketplaceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ListingRepository.init(this)

        setupBottomNavigation()
        setupSearch()
        setupFilterChips()
        setupCategoryRow()
        setupProductGrid()
        setupClickListeners()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        resetScrollToTop()
    }

    private val marketplaceListener = {
        runOnUiThread {
            filterProducts()
        }
    }

    override fun onStart() {
        super.onStart()
        ListingRepository.addChangeListener(marketplaceListener)
        loadUserAvatar()
        filterProducts()
    }

    override fun onResume() {
        super.onResume()
        binding.root.clearFocus()
        loadUserAvatar()
        filterProducts()
    }

    override fun onStop() {
        super.onStop()
        ListingRepository.removeChangeListener(marketplaceListener)
    }

    private fun loadUserAvatar() {
        val user = UserRepository.getCurrentUser()
        ImageLoaderHelper.loadAvatar(
            binding.ivMarketAvatar,
            user?.profileImageUrl,
            user?.avatarRes ?: R.drawable.img_avatar_nimal
        )
    }

    private fun setupBottomNavigation() {
        BottomNavHelper.setup(this, binding.bottomNavBar.root, NavItem.MARKET) {
            resetScrollToTop()
        }
    }

    private fun resetScrollToTop() {
        binding.nestedScrollViewMarket.scrollTo(0, 0)
        binding.nestedScrollViewMarket.post {
            binding.nestedScrollViewMarket.scrollTo(0, 0)
        }
    }

    private fun setupSearch() {
        binding.etSearchMarketplace.doAfterTextChanged { text ->
            currentQuery = text?.toString()?.trim().orEmpty()
            filterProducts()
        }
    }

    private fun filterProducts() {
        if (!::productAdapter.isInitialized) return

        val allListings = ListingRepository.getAll()

        // Get location if enabled in settings
        var userLat: Double? = null
        var userLon: Double? = null
        if (AppPreferences.isLocationSuggestionsEnabled(this)) {
            val coords = LocationHelper.getCachedCoordinates()
            userLat = coords?.first
            userLon = coords?.second
        }

        val scored = SmartSearchEngine.searchMarketplace(
            query = currentQuery,
            categoryFilter = currentCategory,
            typeFilter = currentFilterChip,
            userLat = userLat,
            userLon = userLon,
            items = allListings
        )

        val products = scored.map { it.listing.toMarketProduct() }
        productAdapter.updateData(products)
    }

    private fun setupFilterChips() {
        val chips = listOf(
            binding.chipMarketAll to "All",
            binding.chipMarketSale to "For Sale",
            binding.chipMarketFree to "Free",
            binding.chipMarketDonation to "Donation"
        )

        fun selectChip(selectedChip: TextView, chipType: String) {
            currentFilterChip = chipType
            chips.forEach { (chip, _) ->
                if (chip == selectedChip) {
                    chip.setBackgroundResource(R.drawable.bg_chip_selected)
                    chip.setTextColor(ContextCompat.getColor(this, R.color.eco_chip_selected_text))
                } else {
                    chip.setBackgroundResource(R.drawable.bg_chip_unselected)
                    chip.setTextColor(ContextCompat.getColor(this, R.color.eco_chip_unselected_text))
                }
            }
            filterProducts()
        }

        binding.chipMarketAll.setOnClickListener { selectChip(it as TextView, "All") }
        binding.chipMarketSale.setOnClickListener { selectChip(it as TextView, "For Sale") }
        binding.chipMarketFree.setOnClickListener { selectChip(it as TextView, "Free") }
        binding.chipMarketDonation.setOnClickListener { selectChip(it as TextView, "Donation") }
    }

    private fun setupCategoryRow() {
        val categories = listOf(
            MarketCategory("1", "Furniture", R.drawable.ic_cat_furniture_mkt),
            MarketCategory("2", "Electronics", R.drawable.ic_cat_electronics_mkt),
            MarketCategory("3", "Books", R.drawable.ic_cat_books_mkt),
            MarketCategory("4", "Clothing", R.drawable.ic_cat_clothing_mkt),
            MarketCategory("5", "Home", R.drawable.ic_cat_home_mkt)
        )

        binding.rvMarketCircleCategories.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        categoryAdapter = MarketplaceCategoryAdapter(categories, null) { cat ->
            if (currentCategory.equals(cat.title, ignoreCase = true)) {
                // Toggle off
                currentCategory = "All"
                categoryAdapter.setSelectedCategory(null)
            } else {
                currentCategory = cat.title
                categoryAdapter.setSelectedCategory(cat.title)
            }
            filterProducts()
        }
        binding.rvMarketCircleCategories.adapter = categoryAdapter
    }

    private fun setupProductGrid() {
        productAdapter = MarketplaceProductAdapter(emptyList()) { product ->
            val intent = Intent(this, MarketplaceListingDetailsActivity::class.java).apply {
                putExtra(MarketplaceListingDetailsActivity.EXTRA_LISTING_ID, product.id)
            }
            startActivity(intent)
        }
        binding.rvMarketProducts.layoutManager = GridLayoutManager(this, 2)
        binding.rvMarketProducts.adapter = productAdapter
        filterProducts()
    }

    private fun setupClickListeners() {
        binding.fabPostListing.setOnClickListener {
            val intent = Intent(this, CreatePostTypeActivity::class.java)
            startActivity(intent)
        }

        binding.btnNotifications.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }

        binding.ivMarketAvatar.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }
}
