package com.ecolocal.app.ui.profile

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ecolocal.app.R
import com.ecolocal.app.data.UserRepository
import com.ecolocal.app.databinding.ActivityProfileBinding
import com.ecolocal.app.ui.auth.WelcomeActivity
import com.ecolocal.app.util.BottomNavHelper
import com.ecolocal.app.util.NavItem
import com.google.firebase.auth.FirebaseAuth

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()
        loadUserData()
        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
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
            // User is signed out, route to Welcome
            performLogout()
            return
        }

        val cached = UserRepository.getCurrentUser()
        bindProfileUI(
            name = cached?.fullName ?: firebaseUser.displayName ?: firebaseUser.email?.substringBefore("@") ?: "EcoLocal Member",
            email = cached?.email ?: firebaseUser.email ?: "",
            location = cached?.location?.takeIf { it.isNotBlank() } ?: "Sri Lanka"
        )

        // Fetch latest Firestore users/{uid} document in background
        UserRepository.loadUserProfile(firebaseUser.uid) { freshProfile ->
            if (freshProfile != null) {
                runOnUiThread {
                    bindProfileUI(
                        name = freshProfile.fullName,
                        email = freshProfile.email,
                        location = freshProfile.location.takeIf { it.isNotBlank() } ?: "Sri Lanka"
                    )
                }
            }
        }
    }

    private fun bindProfileUI(name: String, email: String, location: String) {
        binding.tvProfileName.text = name
        binding.tvProfileEmail.text = email
        binding.tvProfileLocation.text = location

        binding.tvDetailName.text = name
        binding.tvDetailEmail.text = email
        binding.tvDetailLocation.text = location

        binding.ivProfileAvatar.setImageResource(R.drawable.img_avatar_nimal)
    }

    private fun setupClickListeners() {
        binding.btnLogout.setOnClickListener {
            performLogout()
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
