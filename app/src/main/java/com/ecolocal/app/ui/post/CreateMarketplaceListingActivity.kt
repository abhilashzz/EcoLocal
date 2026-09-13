package com.ecolocal.app.ui.post

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.ecolocal.app.R
import com.ecolocal.app.data.ListingRepository
import com.ecolocal.app.data.UserRepository
import com.ecolocal.app.databinding.ActivityCreateMarketplaceListingBinding
import com.ecolocal.app.model.MarketplaceListing
import com.ecolocal.app.util.CloudinaryHelper
import com.ecolocal.app.util.ImageLoaderHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class CreateMarketplaceListingActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_POST_TYPE = "extra_post_type"
        const val EXTRA_LISTING_ID = "extra_listing_id"
    }

    private lateinit var binding: ActivityCreateMarketplaceListingBinding
    private var postType: String = "SELL_ITEM"
    private var selectedCondition: String? = null
    private var currentListingId: String? = null
    private var selectedImageUri: String? = null

    private val previewLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                setResult(Activity.RESULT_OK)
                finish()
            }
        }

    private var uploadedImageUrl: String? = null
    private var isUploadingPhoto = false

    private val photoPickerLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            uri?.let { onPhotoSelected(it) }
        }

    private fun onPhotoSelected(uri: Uri) {
        binding.containerPhotoMain.visibility = View.VISIBLE
        binding.ivPhotoMain.setImageURI(uri)
        isUploadingPhoto = true
        Toast.makeText(this, "Uploading image to Cloudinary...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            val result = CloudinaryHelper.uploadImage(this@CreateMarketplaceListingActivity, uri)
            isUploadingPhoto = false
            if (result.isSuccess) {
                val url = result.getOrNull()
                uploadedImageUrl = url
                selectedImageUri = url
                Toast.makeText(this@CreateMarketplaceListingActivity, "Image uploaded successfully!", Toast.LENGTH_SHORT).show()
                ImageLoaderHelper.load(binding.ivPhotoMain, url, R.drawable.img_mkt_desk)
            } else {
                val err = result.exceptionOrNull()?.message ?: "Upload failed"
                Toast.makeText(this@CreateMarketplaceListingActivity, "Upload failed: $err", Toast.LENGTH_LONG).show()
            }
        }
    }

    private val categories = arrayOf(
        "Select category",
        "Furniture",
        "Electronics",
        "Books",
        "Clothing",
        "Home",
        "Other"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateMarketplaceListingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        postType = intent.getStringExtra(EXTRA_POST_TYPE) ?: "SELL_ITEM"
        currentListingId = intent.getStringExtra(EXTRA_LISTING_ID)

        setupCategorySpinner()
        setupConditionChips()
        setupTypeSpecificUI()
        setupClickListeners()

        currentListingId?.let { id ->
            loadExistingListing(id)
        }
    }

    override fun onResume() {
        super.onResume()
        currentListingId?.let { id ->
            loadExistingListing(id)
        }
    }

    private fun setupCategorySpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        binding.spinnerCreateCategory.adapter = adapter
    }

    private fun setupConditionChips() {
        binding.chipCondLikeNew.setOnClickListener { selectCondition("Like New") }
        binding.chipCondGood.setOnClickListener { selectCondition("Good") }
        binding.chipCondFair.setOnClickListener { selectCondition("Fair") }
    }

    private fun selectCondition(condition: String?) {
        selectedCondition = condition
        val selectedBg = R.drawable.bg_condition_chip_selected
        val unselectedBg = R.drawable.bg_condition_chip_unselected
        val selectedText = ContextCompat.getColor(this, R.color.eco_background_white)
        val unselectedText = ContextCompat.getColor(this, R.color.eco_secondary_dark)

        binding.chipCondLikeNew.apply {
            if (condition == "Like New") {
                setBackgroundResource(selectedBg)
                setTextColor(selectedText)
            } else {
                setBackgroundResource(unselectedBg)
                setTextColor(unselectedText)
            }
        }

        binding.chipCondGood.apply {
            if (condition == "Good") {
                setBackgroundResource(selectedBg)
                setTextColor(selectedText)
            } else {
                setBackgroundResource(unselectedBg)
                setTextColor(unselectedText)
            }
        }

        binding.chipCondFair.apply {
            if (condition == "Fair") {
                setBackgroundResource(selectedBg)
                setTextColor(selectedText)
            } else {
                setBackgroundResource(unselectedBg)
                setTextColor(unselectedText)
            }
        }
    }

    private fun setupTypeSpecificUI() {
        when (postType) {
            "GIVE_AWAY", "DONATE_ITEM" -> {
                binding.etCreatePrice.setText("FREE")
                binding.etCreatePrice.isEnabled = false
                binding.etCreatePrice.alpha = 0.75f
            }
            else -> {
                binding.etCreatePrice.isEnabled = true
                binding.etCreatePrice.alpha = 1.0f
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnBackCreate.setOnClickListener {
            finish()
        }

        binding.btnCreateCancel.setOnClickListener {
            finish()
        }

        binding.cardAddPhoto.setOnClickListener {
            photoPickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.containerPhotoMain.setOnClickListener {
            photoPickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.btnCreatePreview.setOnClickListener {
            validateAndProceedToPreview()
        }
    }

    private fun validateAndProceedToPreview() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
        currentFocus?.let { imm?.hideSoftInputFromWindow(it.windowToken, 0) }

        if (isUploadingPhoto) {
            Toast.makeText(this, "Please wait, photo is still uploading...", Toast.LENGTH_SHORT).show()
            return
        }

        val title = binding.etCreateTitle.text.toString().trim()
        val rawPrice = binding.etCreatePrice.text.toString().trim()
        val description = binding.etCreateDescription.text.toString().trim()
        val location = binding.etCreateLocation.text.toString().trim()
        val categoryPos = binding.spinnerCreateCategory.selectedItemPosition
        val category = binding.spinnerCreateCategory.selectedItem?.toString() ?: ""
        val isAvailable = binding.switchCreateAvailable.isChecked

        if (title.isEmpty()) {
            binding.etCreateTitle.error = "Title is required"
            binding.etCreateTitle.requestFocus()
            return
        }

        if (postType == "SELL_ITEM" && rawPrice.isEmpty()) {
            binding.etCreatePrice.error = "Price is required"
            binding.etCreatePrice.requestFocus()
            return
        }

        if (categoryPos == 0 || category == "Select category") {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedCondition.isNullOrEmpty()) {
            Toast.makeText(this, "Please select item condition", Toast.LENGTH_SHORT).show()
            return
        }

        if (description.isEmpty()) {
            binding.etCreateDescription.error = "Description is required"
            binding.etCreateDescription.requestFocus()
            return
        }

        if (location.isEmpty()) {
            binding.etCreateLocation.error = "Location is required"
            binding.etCreateLocation.requestFocus()
            return
        }

        val formattedPrice = when {
            postType == "GIVE_AWAY" || postType == "DONATE_ITEM" || rawPrice.equals("FREE", ignoreCase = true) -> "FREE"
            rawPrice.startsWith("Rs.", ignoreCase = true) -> rawPrice
            else -> "Rs. $rawPrice"
        }

        val listingTypeBadge = when (postType) {
            "GIVE_AWAY" -> "GIVE AWAY"
            "DONATE_ITEM" -> "DONATION"
            else -> "FOR SALE"
        }

        val id = currentListingId ?: "listing_${System.currentTimeMillis()}"
        currentListingId = id

        val currentUser = FirebaseAuth.getInstance().currentUser
        val ownerId = currentUser?.uid ?: "user_anonymous"
        val profile = UserRepository.getCurrentUser()
        val ownerName = profile?.fullName
            ?: currentUser?.displayName
            ?: "EcoLocal User"

        val finalImageUrl = uploadedImageUrl ?: selectedImageUri
        val listing = MarketplaceListing(
            id = id,
            ownerId = ownerId,
            listingType = listingTypeBadge,
            title = title,
            price = formattedPrice,
            category = category,
            condition = selectedCondition ?: "Good",
            description = description,
            location = location,
            imageUrl = finalImageUrl,
            imageUri = finalImageUrl,
            imageRes = if (finalImageUrl.isNullOrEmpty()) R.drawable.img_mkt_desk else 0,
            secondaryImageRes = 0,
            isAvailable = isAvailable,
            sellerName = ownerName,
            sellerAvatarRes = 0,
            memberSince = "Member since 2026",
            status = "ACTIVE"
        )

        // Save into ListingRepository as single source of truth
        if (ListingRepository.getById(id) != null) {
            ListingRepository.update(listing)
        } else {
            ListingRepository.add(listing)
        }

        val intent = Intent(this, PostPreviewActivity::class.java).apply {
            putExtra(PostPreviewActivity.EXTRA_LISTING_ID, id)
        }
        previewLauncher.launch(intent)
    }

    private fun loadExistingListing(id: String) {
        val listing = ListingRepository.getById(id) ?: return
        binding.etCreateTitle.setText(listing.title)
        if (postType == "SELL_ITEM") {
            binding.etCreatePrice.setText(listing.price.replace("Rs. ", "").replace("Rs.", "").trim())
        }
        val catIndex = categories.indexOf(listing.category)
        if (catIndex != -1) {
            binding.spinnerCreateCategory.setSelection(catIndex)
        }
        selectCondition(listing.condition)
        binding.etCreateDescription.setText(listing.description)
        binding.etCreateLocation.setText(listing.location)
        binding.switchCreateAvailable.isChecked = listing.isAvailable

        val existingImg = listing.imageUrl ?: listing.imageUri
        if (!existingImg.isNullOrEmpty()) {
            selectedImageUri = existingImg
            uploadedImageUrl = listing.imageUrl
            binding.containerPhotoMain.visibility = View.VISIBLE
            ImageLoaderHelper.load(binding.ivPhotoMain, selectedImageUri, listing.imageRes)
        } else if (listing.imageRes != 0) {
            binding.containerPhotoMain.visibility = View.VISIBLE
            binding.ivPhotoMain.setImageResource(listing.imageRes)
        }
    }
}
