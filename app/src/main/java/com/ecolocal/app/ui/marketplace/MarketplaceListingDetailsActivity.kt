package com.ecolocal.app.ui.marketplace

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.ecolocal.app.R
import com.ecolocal.app.data.ChatRepository
import com.ecolocal.app.data.ListingRepository
import com.ecolocal.app.data.NotificationRepository
import com.ecolocal.app.data.RequestRepository
import com.ecolocal.app.data.SavedRepository
import com.ecolocal.app.databinding.ActivityMarketplaceListingDetailsBinding
import com.ecolocal.app.ui.chat.ChatActivity
import com.ecolocal.app.ui.post.EditListingActivity
import com.ecolocal.app.util.ImageLoaderHelper

class MarketplaceListingDetailsActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_LISTING_ID = "extra_listing_id"
    }

    private lateinit var binding: ActivityMarketplaceListingDetailsBinding
    private var listingId: String? = null
    private var isFavorited: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMarketplaceListingDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        listingId = intent.getStringExtra(EXTRA_LISTING_ID)
        if (listingId == null) {
            Toast.makeText(this, "Listing ID missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        bindListingData()
        updateBookmarkVisual()
        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        // If the listing was deleted from EditListingActivity, finish immediately
        val id = listingId ?: return
        if (ListingRepository.getById(id) == null) {
            finish()
            return
        }
        bindListingData()
        updateBookmarkVisual()
    }

    private fun updateBookmarkVisual() {
        val id = listingId ?: return
        val saved = SavedRepository.isSaved(id)
        if (saved) {
            binding.ivDetailsBookmarkIcon.setColorFilter(
                ContextCompat.getColor(this, R.color.eco_orange)
            )
        } else {
            binding.ivDetailsBookmarkIcon.setColorFilter(
                ContextCompat.getColor(this, R.color.eco_green_primary)
            )
        }
    }

    private fun bindListingData() {
        val id = listingId ?: return
        val listing = ListingRepository.getById(id) ?: run {
            finish()
            return
        }

        ImageLoaderHelper.load(
            binding.ivDetailsHero,
            listing.imageUri,
            listing.imageRes
        )
        binding.tvDetailsBadge.text = listing.listingType
        binding.tvDetailsTitle.text = listing.title
        binding.tvDetailsPrice.text = listing.price

        binding.tvDetailsInfoCondition.text = listing.condition
        binding.tvDetailsInfoLocation.text = listing.location
        binding.tvDetailsInfoStatus.text = if (listing.isAvailable) "Available" else "Sold Out"

        binding.tvDetailsDescription.text = listing.description

        if (listing.sellerAvatarRes != 0) {
            binding.ivDetailsSellerAvatar.setImageResource(listing.sellerAvatarRes)
        } else {
            binding.ivDetailsSellerAvatar.setImageResource(R.drawable.img_avatar_woman)
        }
        binding.tvDetailsSellerName.text = listing.sellerName
        binding.tvDetailsMemberSince.text = listing.memberSince
    }

    private fun setupClickListeners() {
        binding.btnDetailsBack.setOnClickListener {
            finish()
        }

        binding.btnDetailsShare.setOnClickListener {
            val listing = listingId?.let { ListingRepository.getById(it) }
            val shareText = if (listing != null) {
                "Check out this listing on EcoLocal: ${listing.title} (${listing.price}) in ${listing.location}!"
            } else {
                "Check out EcoLocal marketplace!"
            }
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareText)
                type = "text/plain"
            }
            startActivity(Intent.createChooser(sendIntent, "Share Listing"))
        }

        binding.btnDetailsFav.setOnClickListener {
            isFavorited = !isFavorited
            if (isFavorited) {
                binding.ivDetailsHeart.setColorFilter(
                    ContextCompat.getColor(this, R.color.eco_orange)
                )
                Toast.makeText(this, "Added to favorites", Toast.LENGTH_SHORT).show()
            } else {
                binding.ivDetailsHeart.setColorFilter(
                    ContextCompat.getColor(this, R.color.eco_title_dark)
                )
                Toast.makeText(this, "Removed from favorites", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnDetailsMessage.setOnClickListener {
            val listing = listingId?.let { ListingRepository.getById(it) } ?: return@setOnClickListener
            val conv = ChatRepository.getOrCreateConversation(
                participantName = listing.sellerName,
                participantAvatarRes = listing.sellerAvatarRes,
                participantRole = "Seller",
                listingId = listing.id,
                listingTitle = listing.title
            )
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra(ChatActivity.EXTRA_CONVERSATION_ID, conv.conversationId)
            }
            startActivity(intent)
        }

        binding.btnDetailsInterested.setOnClickListener {
            val id = listingId ?: return@setOnClickListener
            val listing = ListingRepository.getById(id) ?: return@setOnClickListener

            val created = RequestRepository.createMarketplaceInterest(
                listingId = listing.id,
                title = listing.title,
                imageRes = listing.imageRes,
                imageUri = listing.imageUri,
                ownerName = listing.sellerName,
                location = listing.location,
                price = listing.price
            )

            if (created) {
                NotificationRepository.addNotification(
                    title = "Request Sent",
                    message = "Your interest in \"${listing.title}\" was sent to ${listing.sellerName}.",
                    type = "MARKETPLACE",
                    targetListingId = listing.id
                )
                Toast.makeText(this, "Interest sent! View in My Activity > Requests", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "You already sent a request for this item.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnDetailsBookmark.setOnClickListener {
            val id = listingId ?: return@setOnClickListener
            val listing = ListingRepository.getById(id) ?: return@setOnClickListener
            val nowSaved = SavedRepository.toggleSave(id)
            updateBookmarkVisual()

            if (nowSaved) {
                NotificationRepository.addNotification(
                    title = "Item Saved",
                    message = "\"${listing.title}\" was added to your Saved items.",
                    type = "SAVED",
                    targetListingId = listing.id
                )
                Toast.makeText(this, "Saved to your bookmarks", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Removed from saved bookmarks", Toast.LENGTH_SHORT).show()
            }
        }

        // Development-only / owner workflow entry point to EditListingActivity
        binding.ivDetailsHero.setOnLongClickListener {
            openEditListing()
            true
        }
        binding.tvDetailsTitle.setOnLongClickListener {
            openEditListing()
            true
        }
    }

    private fun openEditListing() {
        val id = listingId ?: return
        val intent = Intent(this, EditListingActivity::class.java).apply {
            putExtra(EditListingActivity.EXTRA_LISTING_ID, id)
        }
        startActivity(intent)
    }
}
