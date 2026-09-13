package com.ecolocal.app.ui.profile

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ecolocal.app.R
import com.ecolocal.app.data.UserRepository
import com.ecolocal.app.databinding.ActivityEditProfileBinding
import com.ecolocal.app.util.CloudinaryHelper
import com.ecolocal.app.util.ImageLoaderHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private var currentProfileImageUrl: String? = null
    private var isUploadingPhoto = false

    private val photoPickerLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            uri?.let { onPhotoSelected(it) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadExistingUserData()
        setupListeners()
    }

    private fun loadExistingUserData() {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user == null) {
            finish()
            return
        }

        val profile = UserRepository.getCurrentUser()
        binding.etEditName.setText(profile?.name ?: user.displayName ?: "")
        binding.etEditEmail.setText(profile?.email ?: user.email ?: "")
        binding.etEditPhone.setText(profile?.phone ?: "")
        binding.etEditAddress.setText(profile?.address ?: "")
        binding.etEditCity.setText(profile?.city ?: "")
        binding.etEditCountry.setText(profile?.country ?: "")
        binding.etEditLocation.setText(profile?.locationText ?: "")

        currentProfileImageUrl = profile?.profileImageUrl
        ImageLoaderHelper.loadAvatar(binding.ivEditAvatar, currentProfileImageUrl, R.drawable.img_avatar_nimal)
    }

    private fun setupListeners() {
        binding.btnEditProfileBack.setOnClickListener {
            finish()
        }

        binding.btnChangeAvatar.setOnClickListener {
            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.ivEditAvatar.setOnClickListener {
            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.btnEditProfileSave.setOnClickListener {
            saveProfileChanges()
        }

        binding.btnEditProfileSaveTop.setOnClickListener {
            saveProfileChanges()
        }
    }

    private fun onPhotoSelected(uri: Uri) {
        binding.ivEditAvatar.setImageURI(uri)
        isUploadingPhoto = true
        Toast.makeText(this, "Uploading profile photo to Cloudinary...", Toast.LENGTH_SHORT).show()

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        lifecycleScope.launch {
            val result = CloudinaryHelper.uploadImage(this@EditProfileActivity, uri)
            isUploadingPhoto = false
            if (result.isSuccess) {
                val secureUrl = result.getOrNull()
                currentProfileImageUrl = secureUrl
                if (!secureUrl.isNullOrBlank()) {
                    UserRepository.updateProfilePhoto(uid, secureUrl,
                        onSuccess = {
                            Toast.makeText(this@EditProfileActivity, "Profile photo updated!", Toast.LENGTH_SHORT).show()
                            ImageLoaderHelper.loadAvatar(binding.ivEditAvatar, secureUrl, R.drawable.img_avatar_nimal)
                        },
                        onFailure = { e ->
                            Toast.makeText(this@EditProfileActivity, "Failed saving photo URL: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            } else {
                val err = result.exceptionOrNull()?.message ?: "Upload failed"
                Toast.makeText(this@EditProfileActivity, "Photo upload failed: $err", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun saveProfileChanges() {
        if (isUploadingPhoto) {
            Toast.makeText(this, "Please wait, photo is still uploading...", Toast.LENGTH_SHORT).show()
            return
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val name = binding.etEditName.text.toString().trim()
        val phone = binding.etEditPhone.text.toString().trim()
        val address = binding.etEditAddress.text.toString().trim()
        val city = binding.etEditCity.text.toString().trim()
        val country = binding.etEditCountry.text.toString().trim()
        val location = binding.etEditLocation.text.toString().trim()

        if (name.isEmpty()) {
            binding.etEditName.error = "Display Name is required"
            binding.etEditName.requestFocus()
            return
        }

        binding.btnEditProfileSave.isEnabled = false
        binding.btnEditProfileSave.text = "Saving..."

        UserRepository.updateUserProfile(
            uid = uid,
            name = name,
            phone = phone,
            address = address,
            city = city,
            country = country,
            locationText = location,
            onSuccess = {
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                finish()
            },
            onFailure = { e ->
                binding.btnEditProfileSave.isEnabled = true
                binding.btnEditProfileSave.text = "Save Changes"
                Toast.makeText(this, "Failed to update profile: ${e.message}", Toast.LENGTH_LONG).show()
            }
        )
    }
}
