package com.ecolocal.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.text.method.SingleLineTransformationMethod
import androidx.appcompat.app.AppCompatActivity
import com.ecolocal.app.R
import com.ecolocal.app.databinding.ActivityRegisterBinding

/**
 * RegisterActivity — Create EcoLocal account screen.
 *
 * Displays the registration form with:
 *  - Profile photo upload circle
 *  - Full Name, Email, Password, Confirm Password, Location/Area fields
 *  - Password visibility toggle
 *  - Create Account button (placeholder for Firebase Auth in Batch 02)
 *  - Navigation back to LoginActivity
 */
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Back arrow → previous screen
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Toggle password visibility
        binding.btnTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                binding.etPassword.transformationMethod = SingleLineTransformationMethod.getInstance()
                binding.btnTogglePassword.setImageResource(R.drawable.ic_eye_off)
            } else {
                binding.etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                binding.btnTogglePassword.setImageResource(R.drawable.ic_eye)
            }
            // Keep cursor at end after toggling
            binding.etPassword.setSelection(binding.etPassword.text?.length ?: 0)
        }

        // Profile photo upload (placeholder — Firebase Storage in Batch 02)
        binding.ivProfilePhoto.setOnClickListener {
            // TODO: Open image picker when Firebase Storage is integrated
        }

        // Create Account (placeholder — Firebase Auth in Batch 02)
        binding.btnCreateAccount.setOnClickListener {
            // TODO: Implement Firebase registration in Batch 02
        }

        // "Sign In" → Login screen
        // overridePendingTransition(fade) prevents the slide-out of RegisterActivity
        // from revealing WelcomeActivity (which lives behind Register in the back stack).
        binding.tvSignIn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            overridePendingTransition(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
        }
    }
}
