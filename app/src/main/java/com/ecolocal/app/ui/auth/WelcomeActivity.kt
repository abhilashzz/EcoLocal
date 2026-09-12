package com.ecolocal.app.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ecolocal.app.databinding.ActivityWelcomeBinding
import com.ecolocal.app.ui.main.HomeActivity
import com.google.firebase.auth.FirebaseAuth

/**
 * WelcomeActivity — Entry screen of EcoLocal.
 *
 * Shows the EcoLocal branding, icon grid, tagline, and two navigation paths:
 *  - "Get Started" → RegisterActivity
 *  - "Sign In" → LoginActivity
 *
 * Automatically routes to HomeActivity if a persistent Firebase Auth session exists.
 */
class WelcomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWelcomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Session check: If user is signed in, bypass Welcome and proceed to HomeActivity
        if (FirebaseAuth.getInstance().currentUser != null) {
            val intent = Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
            return
        }

        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // "Get Started" → Register screen
        binding.btnGetStarted.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // "Sign In" text link → Login screen
        binding.tvSignIn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}
