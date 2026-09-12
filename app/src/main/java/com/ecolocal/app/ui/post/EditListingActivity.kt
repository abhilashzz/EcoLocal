package com.ecolocal.app.ui.post

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.ecolocal.app.R
import com.ecolocal.app.data.ListingRepository
import com.ecolocal.app.databinding.ActivityEditListingBinding
import com.ecolocal.app.model.MarketplaceListing
import com.ecolocal.app.util.ImageLoaderHelper

class EditListingActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_LISTING_ID = "extra_listing_id"
    }

    private lateinit var binding: ActivityEditListingBinding
    private var listingId: String? = null
    private var selectedCondition: String = "Good"
    private var selectedImageUri: String? = null

    private val photoPickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                try {
                    contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    // Ignore if persistable permission is not supported
                }
                selectedImageUri = it.toString()
                ImageLoaderHelper.load(binding.ivEditPhotoMain, selectedImageUri, R.drawable.img_mkt_desk)
            }
        }

    private val categories = arrayOf(
        "Furniture",
        "Electronics",
        "Books",
        "Clothing",
        "Home",
        "Other"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditListingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        listingId = intent.getStringExtra(EXTRA_LISTING_ID)
        if (listingId == null) {
            Toast.makeText(this, "Listing ID missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupCategorySpinner()
        setupConditionChips()
        loadListingData()
        setupClickListeners()
    }

    private fun setupCategorySpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        binding.spinnerEditCategory.adapter = adapter
    }

    private fun setupConditionChips() {
        fun updateChipStyles(condition: String) {
            selectedCondition = condition
            val selectedBg = R.drawable.bg_condition_chip_selected
            val unselectedBg = R.drawable.bg_condition_chip_unselected
            val selectedText = ContextCompat.getColor(this, R.color.eco_background_white)
            val unselectedText = ContextCompat.getColor(this, R.color.eco_secondary_dark)

            binding.chipEditLikeNew.apply {
                if (condition == "Like New") {
                    setBackgroundResource(selectedBg)
                    setTextColor(selectedText)
                } else {
                    setBackgroundResource(unselectedBg)
                    setTextColor(unselectedText)
                }
            }

            binding.chipEditGood.apply {
                if (condition == "Good") {
                    setBackgroundResource(selectedBg)
                    setTextColor(selectedText)
                } else {
                    setBackgroundResource(unselectedBg)
                    setTextColor(unselectedText)
                }
            }

            binding.chipEditFair.apply {
                if (condition == "Fair") {
                    setBackgroundResource(selectedBg)
                    setTextColor(selectedText)
                } else {
                    setBackgroundResource(unselectedBg)
                    setTextColor(unselectedText)
                }
            }
        }

        binding.chipEditLikeNew.setOnClickListener { updateChipStyles("Like New") }
        binding.chipEditGood.setOnClickListener { updateChipStyles("Good") }
        binding.chipEditFair.setOnClickListener { updateChipStyles("Fair") }
    }

    private fun loadListingData() {
        val id = listingId ?: return
        val listing = ListingRepository.getById(id) ?: run {
            Toast.makeText(this, "Listing not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (currentUid != null && listing.ownerId.isNotEmpty() && listing.ownerId != currentUid) {
            Toast.makeText(this, "Only the owner can edit this listing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        selectedImageUri = listing.imageUri
        ImageLoaderHelper.load(binding.ivEditPhotoMain, listing.imageUri, listing.imageRes)

        val secImg = listing.secondaryImageRes
        if (secImg != null && secImg != 0) {
            binding.ivEditPhotoSecondary.setImageResource(secImg)
        }

        binding.etEditTitle.setText(listing.title)
        binding.etEditPrice.setText(listing.price.replace("Rs. ", "").replace("Rs.", "").trim())

        val catIndex = categories.indexOf(listing.category)
        if (catIndex != -1) {
            binding.spinnerEditCategory.setSelection(catIndex)
        }

        // Set condition
        selectedCondition = listing.condition
        binding.chipEditLikeNew.performClick()
        if (selectedCondition == "Good") binding.chipEditGood.performClick()
        if (selectedCondition == "Fair") binding.chipEditFair.performClick()
        if (selectedCondition == "Like New") binding.chipEditLikeNew.performClick()

        binding.etEditDescription.setText(listing.description)
        binding.etEditLocation.setText(listing.location)
        binding.switchEditAvailable.isChecked = listing.isAvailable
    }

    private fun setupClickListeners() {
        binding.btnBackEdit.setOnClickListener {
            finish()
        }

        binding.btnEditCancel.setOnClickListener {
            finish()
        }

        binding.cardEditAddPhoto.setOnClickListener {
            photoPickerLauncher.launch("image/*")
        }

        binding.containerEditPhotoMain.setOnClickListener {
            photoPickerLauncher.launch("image/*")
        }

        binding.btnEditDelete.setOnClickListener {
            showDeleteConfirmationDialog()
        }

        binding.btnEditSave.setOnClickListener {
            saveChanges()
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Listing")
            .setMessage("Are you sure you want to delete this listing? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                val id = listingId ?: return@setPositiveButton
                ListingRepository.delete(id)
                Toast.makeText(this, "Listing deleted", Toast.LENGTH_SHORT).show()
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveChanges() {
        val id = listingId ?: return
        val existing = ListingRepository.getById(id) ?: run {
            finish()
            return
        }

        val title = binding.etEditTitle.text.toString().trim()
        val rawPrice = binding.etEditPrice.text.toString().trim()
        val description = binding.etEditDescription.text.toString().trim()
        val location = binding.etEditLocation.text.toString().trim()
        val category = binding.spinnerEditCategory.selectedItem?.toString() ?: existing.category
        val isAvailable = binding.switchEditAvailable.isChecked

        if (title.isEmpty()) {
            binding.etEditTitle.error = "Title is required"
            binding.etEditTitle.requestFocus()
            return
        }

        if (rawPrice.isEmpty()) {
            binding.etEditPrice.error = "Price is required"
            binding.etEditPrice.requestFocus()
            return
        }

        if (description.isEmpty()) {
            binding.etEditDescription.error = "Description is required"
            binding.etEditDescription.requestFocus()
            return
        }

        if (location.isEmpty()) {
            binding.etEditLocation.error = "Location is required"
            binding.etEditLocation.requestFocus()
            return
        }

        val formattedPrice = when {
            rawPrice.equals("FREE", ignoreCase = true) -> "FREE"
            rawPrice.startsWith("Rs.", ignoreCase = true) -> rawPrice
            else -> "Rs. $rawPrice"
        }

        val updated = existing.copy(
            title = title,
            price = formattedPrice,
            category = category,
            condition = selectedCondition,
            description = description,
            locationName = location,
            imageUri = selectedImageUri ?: existing.imageUri,
            isAvailable = isAvailable,
            updatedAt = System.currentTimeMillis()
        )

        ListingRepository.update(updated)
        Toast.makeText(this, "Listing updated successfully", Toast.LENGTH_SHORT).show()
        finish()
    }
}
