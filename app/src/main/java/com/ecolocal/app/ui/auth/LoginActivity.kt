package com.ecolocal.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.text.method.SingleLineTransformationMethod
import androidx.appcompat.app.AppCompatActivity
import com.ecolocal.app.R
import com.ecolocal.app.databinding.ActivityLoginBinding
import com.ecolocal.app.ui.main.HomeActivity

/**
 * LoginActivity — Sign In screen of EcoLocal.
 *
 * Displays the login card with blurred gradient background and:
 *  - Email address input (with envelope icon)
 *  - Password input (with lock + eye-off toggle)
 *  - Forgot Password? link (placeholder)
 *  - Sign In button (placeholder for Firebase Auth in Batch 02)
 *  - "New to EcoLocal? Create Account" → RegisterActivity
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Toggle password visibility
        binding.btnTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                binding.etPassword.transformationMethod = SingleLineTransformationMethod.getInstance()
                binding.btnTogglePassword.setImageResource(R.drawable.ic_eye)
            } else {
                binding.etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                binding.btnTogglePassword.setImageResource(R.drawable.ic_eye_off)
            }
            binding.etPassword.setSelection(binding.etPassword.text?.length ?: 0)
        }

        // Forgot Password (placeholder)
        binding.tvForgotPassword.setOnClickListener {
            // TODO: Implement forgot password flow
        }

        // Sign In (TEMPORARY Batch 02 navigation flow — will be replaced with Firebase Authentication in later batch)
        binding.btnSignIn.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }

        // "Create Account" → Register screen
        // overridePendingTransition(fade) prevents the slide-out of LoginActivity
        // from revealing WelcomeActivity (which lives behind Login in the back stack).
        binding.tvCreateAccount.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
            overridePendingTransition(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
        }
    }
}
