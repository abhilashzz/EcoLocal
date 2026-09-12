package com.ecolocal.app.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecolocal.app.R
import com.ecolocal.app.adapter.MyPostsAdapter
import com.ecolocal.app.adapter.MyRequestsAdapter
import com.ecolocal.app.adapter.SavedListingsAdapter
import com.ecolocal.app.data.ListingRepository
import com.ecolocal.app.data.NotificationRepository
import com.ecolocal.app.data.RequestRepository
import com.ecolocal.app.data.SavedRepository
import com.ecolocal.app.databinding.ActivityMyActivityBinding
import com.ecolocal.app.model.MarketplaceListing
import com.ecolocal.app.ui.marketplace.MarketplaceListingDetailsActivity
import com.ecolocal.app.ui.notifications.NotificationsActivity
import com.ecolocal.app.ui.post.CreatePostTypeActivity
import com.ecolocal.app.ui.post.EditListingActivity
import com.ecolocal.app.util.BottomNavHelper
import com.ecolocal.app.util.NavItem

class MyActivityActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyActivityBinding
    private lateinit var postsAdapter: MyPostsAdapter
    private lateinit var requestsAdapter: MyRequestsAdapter
    private lateinit var savedAdapter: SavedListingsAdapter
    private var currentTab: String = "POSTS"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNav()
        setupRecyclerViews()
        setupTabs()
        setupClickListeners()
        refreshData()
    }

    override fun onResume() {
        super.onResume()
        refreshData()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        refreshData()
        resetScrollToTop()
    }

    private fun setupBottomNav() {
        BottomNavHelper.setup(
            activity = this,
            navRoot = binding.bottomNavBar.root,
            selected = NavItem.ACTIVITY,
            onReselect = { resetScrollToTop() }
        )
    }

    private fun resetScrollToTop() {
        when (currentTab) {
            "POSTS" -> binding.rvMyPosts.scrollToPosition(0)
            "REQUESTS" -> binding.rvMyRequests.scrollToPosition(0)
            "SAVED" -> binding.rvSavedItems.scrollToPosition(0)
        }
    }

    private fun setupRecyclerViews() {
        // Tab 1: Posts
        postsAdapter = MyPostsAdapter(
            posts = emptyList(),
            onItemClick = { listing ->
                val intent = Intent(this, MarketplaceListingDetailsActivity::class.java).apply {
                    putExtra(MarketplaceListingDetailsActivity.EXTRA_LISTING_ID, listing.id)
                }
                startActivity(intent)
            },
            onEditClick = { listing ->
                val intent = Intent(this, EditListingActivity::class.java).apply {
                    putExtra(EditListingActivity.EXTRA_LISTING_ID, listing.id)
                }
                startActivity(intent)
            },
            onDeleteClick = { listing ->
                showDeleteConfirmationDialog(listing)
            }
        )
        binding.rvMyPosts.layoutManager = LinearLayoutManager(this)
        binding.rvMyPosts.adapter = postsAdapter

        // Tab 2: Requests
        requestsAdapter = MyRequestsAdapter(
            requests = emptyList(),
            onItemClick = { request ->
                if (request.requestType == "MARKETPLACE_INTEREST") {
                    val listing = ListingRepository.getById(request.listingId)
                    if (listing != null) {
                        val intent = Intent(this, MarketplaceListingDetailsActivity::class.java).apply {
                            putExtra(MarketplaceListingDetailsActivity.EXTRA_LISTING_ID, listing.id)
                        }
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "Request for: ${request.listingTitle}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Offer submitted for: ${request.listingTitle}", Toast.LENGTH_SHORT).show()
                }
            }
        )
        binding.rvMyRequests.layoutManager = LinearLayoutManager(this)
        binding.rvMyRequests.adapter = requestsAdapter

        // Tab 3: Saved
        savedAdapter = SavedListingsAdapter(
            listings = emptyList(),
            onItemClick = { listing ->
                val intent = Intent(this, MarketplaceListingDetailsActivity::class.java).apply {
                    putExtra(MarketplaceListingDetailsActivity.EXTRA_LISTING_ID, listing.id)
                }
                startActivity(intent)
            },
            onBookmarkToggle = { listing ->
                SavedRepository.toggleSave(listing.id)
                NotificationRepository.addNotification(
                    title = "Removed from Saved",
                    message = "${listing.title} was removed from your saved items.",
                    type = "SAVED",
                    targetListingId = listing.id
                )
                Toast.makeText(this, "Removed from Saved", Toast.LENGTH_SHORT).show()
                refreshSavedData()
            }
        )
        binding.rvSavedItems.layoutManager = LinearLayoutManager(this)
        binding.rvSavedItems.adapter = savedAdapter
    }

    private fun setupTabs() {
        binding.tabMyPosts.setOnClickListener { switchTab("POSTS") }
        binding.tabMyRequests.setOnClickListener { switchTab("REQUESTS") }
        binding.tabSaved.setOnClickListener { switchTab("SAVED") }
    }

    private fun switchTab(tab: String) {
        currentTab = tab

        val selectedBg = R.drawable.bg_segmented_tab_selected
        val transparent = android.R.color.transparent
        val selectedText = ContextCompat.getColor(this, R.color.eco_green_primary)
        val unselectedText = ContextCompat.getColor(this, R.color.eco_secondary_dark)

        binding.tabMyPosts.setBackgroundResource(if (tab == "POSTS") selectedBg else transparent)
        binding.tabMyPosts.setTextColor(if (tab == "POSTS") selectedText else unselectedText)

        binding.tabMyRequests.setBackgroundResource(if (tab == "REQUESTS") selectedBg else transparent)
        binding.tabMyRequests.setTextColor(if (tab == "REQUESTS") selectedText else unselectedText)

        binding.tabSaved.setBackgroundResource(if (tab == "SAVED") selectedBg else transparent)
        binding.tabSaved.setTextColor(if (tab == "SAVED") selectedText else unselectedText)

        when (tab) {
            "POSTS" -> {
                binding.containerTabPosts.visibility = View.VISIBLE
                binding.containerTabRequests.visibility = View.GONE
                binding.containerTabSaved.visibility = View.GONE
                refreshPostsData()
            }
            "REQUESTS" -> {
                binding.containerTabPosts.visibility = View.GONE
                binding.containerTabRequests.visibility = View.VISIBLE
                binding.containerTabSaved.visibility = View.GONE
                refreshRequestsData()
            }
            "SAVED" -> {
                binding.containerTabPosts.visibility = View.GONE
                binding.containerTabRequests.visibility = View.GONE
                binding.containerTabSaved.visibility = View.VISIBLE
                refreshSavedData()
            }
        }
    }

    private fun refreshData() {
        when (currentTab) {
            "POSTS" -> refreshPostsData()
            "REQUESTS" -> refreshRequestsData()
            "SAVED" -> refreshSavedData()
        }
    }

    private fun refreshPostsData() {
        val userPosts = ListingRepository.getUserPosts("user_nimal")
        if (userPosts.isEmpty()) {
            binding.rvMyPosts.visibility = View.GONE
            binding.layoutEmptyPosts.visibility = View.VISIBLE
        } else {
            binding.rvMyPosts.visibility = View.VISIBLE
            binding.layoutEmptyPosts.visibility = View.GONE
            postsAdapter.updateData(userPosts)
        }
    }

    private fun refreshRequestsData() {
        val allRequests = RequestRepository.getAll()
        if (allRequests.isEmpty()) {
            binding.rvMyRequests.visibility = View.GONE
            binding.layoutEmptyRequests.visibility = View.VISIBLE
        } else {
            binding.rvMyRequests.visibility = View.VISIBLE
            binding.layoutEmptyRequests.visibility = View.GONE
            requestsAdapter.updateData(allRequests)
        }
    }

    private fun refreshSavedData() {
        val savedListings = SavedRepository.getSavedListings()
        if (savedListings.isEmpty()) {
            binding.rvSavedItems.visibility = View.GONE
            binding.layoutEmptySaved.visibility = View.VISIBLE
        } else {
            binding.rvSavedItems.visibility = View.VISIBLE
            binding.layoutEmptySaved.visibility = View.GONE
            savedAdapter.updateData(savedListings)
        }
    }

    private fun showDeleteConfirmationDialog(listing: MarketplaceListing) {
        AlertDialog.Builder(this)
            .setTitle("Delete Listing")
            .setMessage("Are you sure you want to delete \"${listing.title}\"? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                ListingRepository.delete(listing.id)
                NotificationRepository.addNotification(
                    title = "Listing Deleted",
                    message = "\"${listing.title}\" was successfully removed from EcoLocal.",
                    type = "MARKETPLACE"
                )
                Toast.makeText(this, "Listing deleted successfully", Toast.LENGTH_SHORT).show()
                refreshPostsData()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupClickListeners() {
        binding.btnActivityNotifications.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }

        binding.btnActivityFabCreate.setOnClickListener {
            startActivity(Intent(this, CreatePostTypeActivity::class.java))
        }

        binding.btnEmptyCreatePost.setOnClickListener {
            startActivity(Intent(this, CreatePostTypeActivity::class.java))
        }
    }
}
