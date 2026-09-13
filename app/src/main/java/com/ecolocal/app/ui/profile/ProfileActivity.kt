package com.ecolocal.app.ui.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ecolocal.app.R
import com.ecolocal.app.data.EcoPointsRepository
import com.ecolocal.app.data.UserRepository
import com.ecolocal.app.databinding.ActivityProfileBinding
import com.ecolocal.app.model.UserProfile
import com.ecolocal.app.ui.auth.WelcomeActivity
import com.ecolocal.app.ui.settings.SettingsActivity
import com.ecolocal.app.util.BottomNavHelper
import com.ecolocal.app.util.CloudinaryHelper
import com.ecolocal.app.util.ImageLoaderHelper
import com.ecolocal.app.util.NavItem
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding

    private val profileObserver = {
        runOnUiThread {
            loadUserData()
        }
    }

    private val photoPickerLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            uri?.let { uploadProfilePhoto(it) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()
        loadUserData()
        setupClickListeners()
    }

    override fun onStart() {
        super.onStart()
        UserRepository.addProfileChangeListener(profileObserver)
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
    }

    override fun onStop() {
        super.onStop()
        UserRepository.removeProfileChangeListener(profileObserver)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        loadUserData()
        binding.scrollProfile.scrollTo(0, 0)
    }

    private fun setupBottomNavigation() {
        BottomNavHelper.setup(this, binding.bottomNavBar.root, NavItem.PROFILE) {
            binding.scrollProfile.scrollTo(0, 0)
        }
    }

    private fun loadUserData() {
        val auth = FirebaseAuth.getInstance()
        val firebaseUser = auth.currentUser

        if (firebaseUser == null) {
            performLogout()
            return
        }

        val cached = UserRepository.getCurrentUser()
        if (cached != null) {
            bindProfileUI(cached)
        } else {
            val fallback = UserProfile(
                uid = firebaseUser.uid,
                name = firebaseUser.displayName ?: firebaseUser.email?.substringBefore("@") ?: "EcoLocal Member",
                email = firebaseUser.email ?: ""
            )
            bindProfileUI(fallback)
        }

        // Fresh fetch from Firestore users/{uid}
        UserRepository.loadUserProfile(firebaseUser.uid) { freshProfile ->
            if (freshProfile != null) {
                runOnUiThread {
                    bindProfileUI(freshProfile)
                }
            }
        }
    }

    private fun bindProfileUI(profile: UserProfile) {
        val name = profile.fullName
        val email = profile.email
        val location = profile.location.takeIf { it.isNotBlank() } ?: "Sri Lanka"

        binding.tvProfileName.text = name
        binding.tvProfileEmail.text = email
        binding.tvProfileLocation.text = location

        binding.tvDetailName.text = name
        binding.tvDetailEmail.text = email
        binding.tvDetailPhone.text = profile.phone.takeIf { it.isNotBlank() } ?: "Not provided"
        binding.tvDetailAddress.text = profile.address.takeIf { it.isNotBlank() } ?: "Not provided"

        val cityCountry = listOfNotNull(
            profile.city.takeIf { it.isNotBlank() },
            profile.country.takeIf { it.isNotBlank() }
        ).joinToString(", ")
        binding.tvDetailCityCountry.text = cityCountry.ifBlank { "Sri Lanka" }

        binding.tvDetailLocation.text = location

        // Rating
        if (profile.reviewCount > 0 && profile.rating > 0) {
            binding.tvProfileRating.text = "★ ${profile.rating} (${profile.reviewCount} ${if (profile.reviewCount == 1) "review" else "reviews"})"
        } else {
            binding.tvProfileRating.text = "No ratings yet"
        }

        // EcoPoints
        val level = profile.ecoLevel.ifBlank { EcoPointsRepository.calculateLevel(profile.ecoPoints) }
        binding.tvProfileEcopoints.text = "${profile.ecoPoints} EcoPoints • $level"

        // Avatar
        ImageLoaderHelper.loadAvatar(
            binding.ivProfileAvatar,
            profile.profileImageUrl,
            R.drawable.img_avatar_nimal
        )
    }

    private fun setupClickListeners() {
        binding.btnProfileChangePhoto.setOnClickListener {
            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.ivProfileAvatar.setOnClickListener {
            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.btnEditProfile.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
        }

        binding.btnProfileSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        binding.btnSettingsEntry.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        binding.btnLogout.setOnClickListener {
            performLogout()
        }
    }

    private fun uploadProfilePhoto(uri: Uri) {
        binding.ivProfileAvatar.setImageURI(uri)
        Toast.makeText(this, "Uploading avatar to Cloudinary...", Toast.LENGTH_SHORT).show()

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        lifecycleScope.launch {
            val result = CloudinaryHelper.uploadImage(this@ProfileActivity, uri)
            if (result.isSuccess) {
                val secureUrl = result.getOrNull()
                if (!secureUrl.isNullOrBlank()) {
                    UserRepository.updateProfilePhoto(uid, secureUrl,
                        onSuccess = {
                            Toast.makeText(this@ProfileActivity, "Avatar updated successfully!", Toast.LENGTH_SHORT).show()
                            ImageLoaderHelper.loadAvatar(binding.ivProfileAvatar, secureUrl, R.drawable.img_avatar_nimal)
                        },
                        onFailure = { e ->
                            Toast.makeText(this@ProfileActivity, "Failed saving avatar: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            } else {
                val err = result.exceptionOrNull()?.message ?: "Upload failed"
                Toast.makeText(this@ProfileActivity, "Avatar upload failed: $err", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun performLogout() {
        FirebaseAuth.getInstance().signOut()
        UserRepository.clearCache()

        val intent = Intent(this, WelcomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
