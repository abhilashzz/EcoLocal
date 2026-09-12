package com.ecolocal.app.ui.post

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ecolocal.app.R
import com.ecolocal.app.data.NotificationRepository
import com.ecolocal.app.data.ServiceRepository
import com.ecolocal.app.data.UserRepository
import com.ecolocal.app.databinding.ActivityCreateCommunityServiceBinding
import com.ecolocal.app.model.CommunityService
import com.google.firebase.auth.FirebaseAuth
import java.util.UUID

class CreateCommunityServiceActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SERVICE_TYPE = "extra_service_type"
        const val SERVICE_OFFER = "SERVICE_OFFER"
        const val HELP_REQUEST = "HELP_REQUEST"
    }

    private lateinit var binding: ActivityCreateCommunityServiceBinding
    private var serviceType: String = SERVICE_OFFER

    private val categories = listOf(
        "Tutoring",
        "Repairs",
        "Gardening",
        "Volunteering",
        "Delivery",
        "Other"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateCommunityServiceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        serviceType = intent.getStringExtra(EXTRA_SERVICE_TYPE) ?: SERVICE_OFFER

        setupUI()
        setupCategorySpinner()
        setupClickListeners()
    }

    private fun setupUI() {
        val currentUser = UserRepository.getCurrentUser()
        if (currentUser != null && currentUser.location.isNotBlank()) {
            binding.etServiceLocation.setText(currentUser.location)
        }

        if (serviceType == HELP_REQUEST) {
            binding.tvHeaderTitle.text = "Request Help"
            binding.tvTitleLabel.text = "Request Title *"
            binding.etServiceTitle.hint = "e.g. Need Math Tutor / Tap Repair"
            binding.layoutServicePrice.visibility = View.GONE
            binding.layoutServiceTime.visibility = View.VISIBLE
            binding.tvPublishText.text = "Publish Help Request"
        } else {
            binding.tvHeaderTitle.text = "Offer a Service"
            binding.tvTitleLabel.text = "Service Title *"
            binding.etServiceTitle.hint = "e.g. Science & Maths Tutor"
            binding.layoutServicePrice.visibility = View.VISIBLE
            binding.layoutServiceTime.visibility = View.GONE
            binding.tvPublishText.text = "Publish Service Offer"
        }
    }

    private fun setupCategorySpinner() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            categories
        )
        binding.spinnerServiceCategory.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.btnBackService.setOnClickListener {
            finish()
        }

        binding.btnPublishService.setOnClickListener {
            publishService()
        }
    }

    private fun publishService() {
        val title = binding.etServiceTitle.text.toString().trim()
        val category = binding.spinnerServiceCategory.selectedItem?.toString() ?: "Other"
        val location = binding.etServiceLocation.text.toString().trim()
        val description = binding.etServiceDescription.text.toString().trim()
        val price = binding.etServicePrice.text.toString().trim()
        val timeText = binding.etServiceTime.text.toString().trim()

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show()
            binding.etServiceTitle.requestFocus()
            return
        }

        if (location.isEmpty()) {
            Toast.makeText(this, "Please enter a location", Toast.LENGTH_SHORT).show()
            binding.etServiceLocation.requestFocus()
            return
        }

        if (description.isEmpty()) {
            Toast.makeText(this, "Please enter a description", Toast.LENGTH_SHORT).show()
            binding.etServiceDescription.requestFocus()
            return
        }

        val auth = FirebaseAuth.getInstance()
        val currentUid = auth.currentUser?.uid ?: ""
        val userProfile = UserRepository.getCurrentUser()
        val ownerName = userProfile?.fullName
            ?: auth.currentUser?.displayName
            ?: "Community Member"

        val defaultImageRes = if (category.equals("Gardening", ignoreCase = true)) {
            R.drawable.img_service_lawn
        } else {
            R.drawable.img_service_tutor
        }

        val service = CommunityService(
            serviceId = "srv_${UUID.randomUUID()}",
            ownerId = currentUid,
            ownerName = ownerName,
            title = title,
            description = description,
            category = category,
            serviceType = serviceType,
            locationName = location,
            price = if (serviceType == SERVICE_OFFER) {
                if (price.isNotBlank()) price else "FREE"
            } else "",
            timeText = if (serviceType == HELP_REQUEST) {
                if (timeText.isNotBlank()) timeText else "Flexible"
            } else "",
            status = "ACTIVE",
            imageRes = defaultImageRes,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        binding.btnPublishService.isEnabled = false

        ServiceRepository.addService(service) { success ->
            runOnUiThread {
                binding.btnPublishService.isEnabled = true
                if (success) {
                    val notifType = if (serviceType == SERVICE_OFFER) "Service Offer" else "Help Request"
                    NotificationRepository.addNotification(
                        title = "$notifType Published",
                        message = "Your $notifType \"$title\" is now live on Community Services.",
                        type = "COMMUNITY"
                    )
                    Toast.makeText(this, "$notifType published successfully!", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this, "Failed to publish service. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
