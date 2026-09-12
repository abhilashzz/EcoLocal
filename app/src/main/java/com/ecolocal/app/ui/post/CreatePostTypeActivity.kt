package com.ecolocal.app.ui.post

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.ecolocal.app.R
import com.ecolocal.app.databinding.ActivityCreatePostTypeBinding

enum class PostTypeSelection {
    OFFER_SERVICE,
    REQUEST_HELP,
    SELL_ITEM,
    GIVE_AWAY,
    DONATE_ITEM
}

class CreatePostTypeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreatePostTypeBinding
    private var selectedType: PostTypeSelection = PostTypeSelection.SELL_ITEM

    private val createListingLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreatePostTypeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        updateSelectionUI()
    }

    private fun setupClickListeners() {
        binding.btnClosePostType.setOnClickListener {
            finish()
        }

        binding.cardOfferService.setOnClickListener {
            selectedType = PostTypeSelection.OFFER_SERVICE
            updateSelectionUI()
        }

        binding.cardRequestHelp.setOnClickListener {
            selectedType = PostTypeSelection.REQUEST_HELP
            updateSelectionUI()
        }

        binding.cardSellItem.setOnClickListener {
            selectedType = PostTypeSelection.SELL_ITEM
            updateSelectionUI()
        }

        binding.cardGiveAway.setOnClickListener {
            selectedType = PostTypeSelection.GIVE_AWAY
            updateSelectionUI()
        }

        binding.cardDonateItem.setOnClickListener {
            selectedType = PostTypeSelection.DONATE_ITEM
            updateSelectionUI()
        }

        binding.btnContinue.setOnClickListener {
            when (selectedType) {
                PostTypeSelection.OFFER_SERVICE,
                PostTypeSelection.REQUEST_HELP -> {
                    Toast.makeText(
                        this,
                        "Service post creation will be implemented in the Community Services creation batch.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                PostTypeSelection.SELL_ITEM,
                PostTypeSelection.GIVE_AWAY,
                PostTypeSelection.DONATE_ITEM -> {
                    val intent = Intent(this, CreateMarketplaceListingActivity::class.java).apply {
                        putExtra(CreateMarketplaceListingActivity.EXTRA_POST_TYPE, selectedType.name)
                    }
                    createListingLauncher.launch(intent)
                }
            }
        }
    }

    private fun updateSelectionUI() {
        fun updateCard(
            card: LinearLayout,
            iconBg: FrameLayout,
            checkIv: ImageView,
            isSelected: Boolean
        ) {
            if (isSelected) {
                card.setBackgroundResource(R.drawable.bg_post_type_selected)
                iconBg.setBackgroundResource(R.drawable.bg_circle_button)
                checkIv.visibility = View.VISIBLE
            } else {
                card.setBackgroundResource(R.drawable.bg_post_type_unselected)
                iconBg.setBackgroundResource(R.drawable.bg_circle_light_mint)
                checkIv.visibility = View.GONE
            }
        }

        updateCard(
            binding.cardOfferService,
            binding.iconBgOfferService,
            binding.ivCheckOfferService,
            selectedType == PostTypeSelection.OFFER_SERVICE
        )

        updateCard(
            binding.cardRequestHelp,
            binding.iconBgRequestHelp,
            binding.ivCheckRequestHelp,
            selectedType == PostTypeSelection.REQUEST_HELP
        )

        updateCard(
            binding.cardSellItem,
            binding.iconBgSellItem,
            binding.ivCheckSellItem,
            selectedType == PostTypeSelection.SELL_ITEM
        )

        updateCard(
            binding.cardGiveAway,
            binding.iconBgGiveAway,
            binding.ivCheckGiveAway,
            selectedType == PostTypeSelection.GIVE_AWAY
        )

        updateCard(
            binding.cardDonateItem,
            binding.iconBgDonateItem,
            binding.ivCheckDonateItem,
            selectedType == PostTypeSelection.DONATE_ITEM
        )
    }
}
