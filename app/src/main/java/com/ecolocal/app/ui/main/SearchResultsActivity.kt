package com.ecolocal.app.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecolocal.app.R
import com.ecolocal.app.adapter.SearchResultItem
import com.ecolocal.app.adapter.UnifiedSearchResultAdapter
import com.ecolocal.app.data.ListingRepository
import com.ecolocal.app.data.ServiceRepository
import com.ecolocal.app.databinding.ActivitySearchResultsBinding
import com.ecolocal.app.model.CommunityService
import com.ecolocal.app.model.MarketplaceListing
import com.ecolocal.app.ui.marketplace.MarketplaceListingDetailsActivity
import com.ecolocal.app.util.AppPreferences
import com.ecolocal.app.util.LocationHelper
import com.ecolocal.app.util.SmartSearchEngine

class SearchResultsActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_QUERY = "extra_query"
    }

    private lateinit var binding: ActivitySearchResultsBinding
    private lateinit var adapter: UnifiedSearchResultAdapter

    private enum class SearchTab { ALL, MARKETPLACE, SERVICES }
    private var currentTab = SearchTab.ALL
    private var currentQuery = ""
    private var userLat: Double? = null
    private var userLon: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchResultsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentQuery = intent.getStringExtra(EXTRA_QUERY) ?: ""

        setupRecyclerView()
        setupListeners()
        setupTabs()
        fetchUserLocation()

        if (currentQuery.isNotEmpty()) {
            binding.etSearchQuery.setText(currentQuery)
            binding.etSearchQuery.setSelection(currentQuery.length)
            AppPreferences.addRecentSearch(currentQuery)
        }
        performSearch(currentQuery)
    }

    private fun fetchUserLocation() {
        LocationHelper.getDeviceLocation(this) { loc ->
            if (loc != null) {
                userLat = loc.latitude
                userLon = loc.longitude
                performSearch(binding.etSearchQuery.text?.toString() ?: "")
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = UnifiedSearchResultAdapter(
            items = emptyList(),
            onMarketplaceClick = { listing ->
                val intent = Intent(this, MarketplaceListingDetailsActivity::class.java).apply {
                    putExtra(MarketplaceListingDetailsActivity.EXTRA_LISTING_ID, listing.listingId)
                }
                startActivity(intent)
            },
            onServiceClick = { service ->
                val intent = Intent(this, CommunityServicesActivity::class.java).apply {
                    val tabName = if (service.serviceType == "HELP_REQUEST") "HELP_REQUESTS" else "SERVICE_OFFERS"
                    putExtra(CommunityServicesActivity.EXTRA_INITIAL_TAB, tabName)
                }
                startActivity(intent)
            }
        )
        binding.rvSearchResults.layoutManager = LinearLayoutManager(this)
        binding.rvSearchResults.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnSearchBack.setOnClickListener {
            finish()
        }

        binding.btnClearSearch.setOnClickListener {
            binding.etSearchQuery.setText("")
        }

        binding.etSearchQuery.doAfterTextChanged { text ->
            val query = text?.toString()?.trim() ?: ""
            binding.btnClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
            currentQuery = query
            performSearch(query)
        }

        binding.etSearchQuery.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.etSearchQuery.text?.toString()?.trim() ?: ""
                if (query.isNotBlank()) {
                    AppPreferences.addRecentSearch(query)
                }
                val imm = getSystemService(INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                imm?.hideSoftInputFromWindow(binding.etSearchQuery.windowToken, 0)
                performSearch(query)
                true
            } else {
                false
            }
        }
    }

    private fun setupTabs() {
        binding.tabSearchAll.setOnClickListener {
            setTab(SearchTab.ALL)
        }
        binding.tabSearchMarketplace.setOnClickListener {
            setTab(SearchTab.MARKETPLACE)
        }
        binding.tabSearchServices.setOnClickListener {
            setTab(SearchTab.SERVICES)
        }
    }

    private fun setTab(tab: SearchTab) {
        currentTab = tab
        updateTabStyles()
        performSearch(binding.etSearchQuery.text?.toString()?.trim() ?: "")
    }

    private fun updateTabStyles() {
        val selectedBg = R.drawable.bg_chip_selected
        val unselectedBg = R.drawable.bg_chip_unselected
        val selectedColor = ContextCompat.getColor(this, R.color.eco_chip_selected_text)
        val unselectedColor = ContextCompat.getColor(this, R.color.eco_chip_unselected_text)

        binding.tabSearchAll.apply {
            setBackgroundResource(if (currentTab == SearchTab.ALL) selectedBg else unselectedBg)
            setTextColor(if (currentTab == SearchTab.ALL) selectedColor else unselectedColor)
        }
        binding.tabSearchMarketplace.apply {
            setBackgroundResource(if (currentTab == SearchTab.MARKETPLACE) selectedBg else unselectedBg)
            setTextColor(if (currentTab == SearchTab.MARKETPLACE) selectedColor else unselectedColor)
        }
        binding.tabSearchServices.apply {
            setBackgroundResource(if (currentTab == SearchTab.SERVICES) selectedBg else unselectedBg)
            setTextColor(if (currentTab == SearchTab.SERVICES) selectedColor else unselectedColor)
        }
    }

    private fun performSearch(query: String) {
        val recentCategories = AppPreferences.getRecentCategories()

        val scoredMarketplace = if (currentTab != SearchTab.SERVICES) {
            SmartSearchEngine.searchMarketplace(
                query = query,
                userLat = userLat,
                userLon = userLon,
                userPreferredCategories = recentCategories,
                items = ListingRepository.getAll()
            )
        } else {
            emptyList()
        }

        val scoredServices = if (currentTab != SearchTab.MARKETPLACE) {
            SmartSearchEngine.searchServices(
                query = query,
                userLat = userLat,
                userLon = userLon,
                userPreferredCategories = recentCategories,
                items = ServiceRepository.getAll()
            )
        } else {
            emptyList()
        }

        val combined = mutableListOf<SearchResultItem>()

        when (currentTab) {
            SearchTab.ALL -> {
                val mktItems = scoredMarketplace.map {
                    SearchResultItem.Marketplace(it.listing, it.score, it.distanceKm, it.matchReasons)
                }
                val srvItems = scoredServices.map {
                    SearchResultItem.Service(it.service, it.score, it.distanceKm, it.matchReasons)
                }
                combined.addAll(mktItems)
                combined.addAll(srvItems)
                // Sort unified results by score then proximity
                combined.sortWith(
                    compareByDescending<SearchResultItem> {
                        when (it) {
                            is SearchResultItem.Marketplace -> it.score
                            is SearchResultItem.Service -> it.score
                        }
                    }.thenBy {
                        when (it) {
                            is SearchResultItem.Marketplace -> it.distanceKm ?: Double.MAX_VALUE
                            is SearchResultItem.Service -> it.distanceKm ?: Double.MAX_VALUE
                        }
                    }
                )
            }
            SearchTab.MARKETPLACE -> {
                combined.addAll(scoredMarketplace.map {
                    SearchResultItem.Marketplace(it.listing, it.score, it.distanceKm, it.matchReasons)
                })
            }
            SearchTab.SERVICES -> {
                combined.addAll(scoredServices.map {
                    SearchResultItem.Service(it.service, it.score, it.distanceKm, it.matchReasons)
                })
            }
        }

        adapter.updateData(combined)

        if (query.isEmpty()) {
            binding.tvSearchResultsCount.text = "Showing ${combined.size} results"
        } else {
            binding.tvSearchResultsCount.text = "${combined.size} results for \"$query\""
        }

        if (combined.isEmpty()) {
            binding.rvSearchResults.visibility = View.GONE
            binding.layoutNoResults.visibility = View.VISIBLE
            binding.tvNoResultsTitle.text = if (query.isEmpty()) "No listings available" else "No results for \"$query\""
        } else {
            binding.rvSearchResults.visibility = View.VISIBLE
            binding.layoutNoResults.visibility = View.GONE
        }
    }
}
