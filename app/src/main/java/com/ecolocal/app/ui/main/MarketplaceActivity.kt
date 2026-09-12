package com.ecolocal.app.ui.main

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecolocal.app.R
import com.ecolocal.app.adapter.MarketplaceCategoryAdapter
import com.ecolocal.app.adapter.MarketplaceProductAdapter
import com.ecolocal.app.data.ListingRepository
import com.ecolocal.app.databinding.ActivityMarketplaceBinding
import com.ecolocal.app.model.MarketCategory
import com.ecolocal.app.ui.marketplace.MarketplaceListingDetailsActivity
import com.ecolocal.app.ui.post.CreatePostTypeActivity
import com.ecolocal.app.util.BottomNavHelper
import com.ecolocal.app.util.NavItem

class MarketplaceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMarketplaceBinding
    private lateinit var productAdapter: MarketplaceProductAdapter

    private var currentFilterChip: String = "All"
    private var currentQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMarketplaceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()
        setupSearch()
        setupFilterChips()
        setupCategoryRow()
        setupProductGrid()
        setupClickListeners()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        resetScrollToTop()
    }

    override fun onResume() {
        super.onResume()
        binding.root.clearFocus()
        filterProducts()
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
            currentQuery = text?.toString()?.trim() ?: ""
            filterProducts()
        }
    }

    private fun filterProducts() {
        if (!::productAdapter.isInitialized) return
        val results = ListingRepository.searchAndFilter(currentQuery, currentFilterChip)
        productAdapter.updateData(results)
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
        binding.rvMarketCircleCategories.adapter = MarketplaceCategoryAdapter(categories) { cat ->
            if (binding.etSearchMarketplace.text.toString().equals(cat.title, ignoreCase = true)) {
                binding.etSearchMarketplace.setText("")
            } else {
                binding.etSearchMarketplace.setText(cat.title)
                binding.etSearchMarketplace.setSelection(cat.title.length)
            }
        }
    }

    private fun setupProductGrid() {
        val products = ListingRepository.searchAndFilter(currentQuery, currentFilterChip)
        binding.rvMarketProducts.layoutManager = GridLayoutManager(this, 2)
        productAdapter = MarketplaceProductAdapter(products) { product ->
            val intent = Intent(this, MarketplaceListingDetailsActivity::class.java).apply {
                putExtra(MarketplaceListingDetailsActivity.EXTRA_LISTING_ID, product.id)
            }
            startActivity(intent)
        }
        binding.rvMarketProducts.adapter = productAdapter
    }

    private fun setupClickListeners() {
        binding.fabPostListing.setOnClickListener {
            val intent = Intent(this, CreatePostTypeActivity::class.java)
            startActivity(intent)
        }

        binding.btnNotifications.setOnClickListener {
            startActivity(Intent(this, com.ecolocal.app.ui.notifications.NotificationsActivity::class.java))
        }

        binding.ivMarketAvatar.setOnClickListener {
            Toast.makeText(this, "User Profile", Toast.LENGTH_SHORT).show()
        }
    }
}
