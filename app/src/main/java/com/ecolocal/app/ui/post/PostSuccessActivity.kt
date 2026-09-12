package com.ecolocal.app.ui.post

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ecolocal.app.databinding.ActivityPostSuccessBinding
import com.ecolocal.app.ui.marketplace.MarketplaceListingDetailsActivity

class PostSuccessActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_LISTING_ID = "extra_listing_id"
    }

    private lateinit var binding: ActivityPostSuccessBinding
    private var listingId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostSuccessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        listingId = intent.getStringExtra(EXTRA_LISTING_ID)

        binding.btnSuccessViewPost.setOnClickListener {
            val id = listingId
            if (id != null) {
                val intent = Intent(this, MarketplaceListingDetailsActivity::class.java).apply {
                    putExtra(MarketplaceListingDetailsActivity.EXTRA_LISTING_ID, id)
                }
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Listing ID missing", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        binding.btnSuccessGoActivity.setOnClickListener {
            val intent = Intent(this, com.ecolocal.app.ui.activity.MyActivityActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)
            finish()
        }
    }
}
