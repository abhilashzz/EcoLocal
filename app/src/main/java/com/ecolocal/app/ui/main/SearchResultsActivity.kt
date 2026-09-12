package com.ecolocal.app.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.GridLayoutManager
import com.ecolocal.app.adapter.MarketplaceProductAdapter
import com.ecolocal.app.data.ListingRepository
import com.ecolocal.app.databinding.ActivitySearchResultsBinding
import com.ecolocal.app.ui.marketplace.MarketplaceListingDetailsActivity

class SearchResultsActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_QUERY = "extra_query"
    }

    private lateinit var binding: ActivitySearchResultsBinding
    private lateinit var adapter: MarketplaceProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchResultsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val initialQuery = intent.getStringExtra(EXTRA_QUERY) ?: ""

        setupRecyclerView()
        setupListeners()

        if (initialQuery.isNotEmpty()) {
            binding.etSearchQuery.setText(initialQuery)
            binding.etSearchQuery.setSelection(initialQuery.length)
        }
        performSearch(initialQuery)
    }

    private fun setupRecyclerView() {
        adapter = MarketplaceProductAdapter(emptyList()) { product ->
            val intent = Intent(this, MarketplaceListingDetailsActivity::class.java).apply {
                putExtra(MarketplaceListingDetailsActivity.EXTRA_LISTING_ID, product.id)
            }
            startActivity(intent)
        }
        binding.rvSearchResults.layoutManager = GridLayoutManager(this, 2)
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
            performSearch(query)
        }

        binding.etSearchQuery.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                imm?.hideSoftInputFromWindow(binding.etSearchQuery.windowToken, 0)
                true
            } else {
                false
            }
        }
    }

    private fun performSearch(query: String) {
        val results = ListingRepository.searchAndFilter(query, "All")
        adapter.updateData(results)

        if (query.isEmpty()) {
            binding.tvSearchResultsCount.text = "Showing all ${results.size} listings"
        } else {
            binding.tvSearchResultsCount.text = "${results.size} results for \"$query\""
        }

        if (results.isEmpty()) {
            binding.rvSearchResults.visibility = View.GONE
            binding.layoutNoResults.visibility = View.VISIBLE
            binding.tvNoResultsTitle.text = if (query.isEmpty()) "No listings available" else "No results for \"$query\""
        } else {
            binding.rvSearchResults.visibility = View.VISIBLE
            binding.layoutNoResults.visibility = View.GONE
        }
    }
}
