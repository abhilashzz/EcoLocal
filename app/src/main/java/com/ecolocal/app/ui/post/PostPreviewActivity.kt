package com.ecolocal.app.ui.post

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ecolocal.app.R
import com.ecolocal.app.data.ListingRepository
import com.ecolocal.app.databinding.ActivityPostPreviewBinding

class PostPreviewActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_LISTING_ID = "extra_listing_id"
    }

    private lateinit var binding: ActivityPostPreviewBinding
    private var listingId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        listingId = intent.getStringExtra(EXTRA_LISTING_ID)
        if (listingId == null) {
            Toast.makeText(this, "Error: Listing ID missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        bindListingData()
        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        bindListingData()
    }

    private fun bindListingData() {
        val id = listingId ?: return
        val listing = ListingRepository.getById(id)
        if (listing == null) {
            Toast.makeText(this, "Listing not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        com.ecolocal.app.util.ImageLoaderHelper.load(
            binding.ivPreviewImage,
            listing.imageUri,
            listing.imageRes
        )
        binding.tvPreviewBadge.text = listing.listingType
        binding.tvPreviewCategory.text = listing.category
        binding.tvPreviewPrice.text = listing.price
        binding.tvPreviewTitle.text = listing.title
        binding.tvPreviewCondition.text = "${listing.condition} condition"
        binding.tvPreviewLocation.text = listing.location

        // Seller details
        binding.tvPreviewSellerName.text = listing.sellerName
        val initials = listing.sellerName.split(" ")
            .mapNotNull { it.firstOrNull()?.toString() }
            .take(2)
            .joinToString("")
            .uppercase()
        binding.tvSellerAvatarInitials.text = if (initials.isNotEmpty()) initials else "NP"
    }

    private fun setupClickListeners() {
        binding.btnBackPreview.setOnClickListener {
            finish()
        }

        // "Edit" returns user directly back to the Create form with all values intact
        binding.btnPreviewEdit.setOnClickListener {
            finish()
        }

        // "Publish Post" navigates to PostSuccessActivity
        binding.btnPreviewPublish.setOnClickListener {
            val id = listingId ?: return@setOnClickListener
            val intent = Intent(this, PostSuccessActivity::class.java).apply {
                putExtra(PostSuccessActivity.EXTRA_LISTING_ID, id)
            }
            startActivity(intent)
            setResult(android.app.Activity.RESULT_OK)
            finish()
        }
    }
}
