package com.ecolocal.app.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ecolocal.app.databinding.ActivityWelcomeBinding

/**
 * WelcomeActivity — Entry screen of EcoLocal.
 *
 * Shows the EcoLocal branding, icon grid, tagline, and two navigation paths:
 *  - "Get Started" → RegisterActivity
 *  - "Sign In" → LoginActivity
 *
 * Firebase Authentication is NOT yet integrated (placeholder for Batch 02).
 */
class WelcomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWelcomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
