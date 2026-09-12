package com.ecolocal.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.text.method.SingleLineTransformationMethod
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ecolocal.app.R
import com.ecolocal.app.data.UserRepository
import com.ecolocal.app.databinding.ActivityLoginBinding
import com.ecolocal.app.ui.main.HomeActivity
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

/**
 * LoginActivity — Sign In screen of EcoLocal.
 *
 * Integrated with real Firebase Authentication (email/password).
 * Ensures Firestore profile document exists before entering HomeActivity.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
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

        // Forgot Password
        binding.tvForgotPassword.setOnClickListener {
            val email = binding.etEmail.text?.toString()?.trim().orEmpty()
            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.etEmail.error = "Enter your registered email address"
                binding.etEmail.requestFocus()
                Toast.makeText(this, "Please enter your email to receive a password reset link", Toast.LENGTH_SHORT).show()
            } else {
                auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Password reset link sent to $email", Toast.LENGTH_LONG).show()
                    }
                    .addOnFailureListener { exception ->
                        val msg = exception.localizedMessage ?: "Failed to send password reset email"
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                    }
            }
        }

        // Sign In → Firebase Auth
        binding.btnSignIn.setOnClickListener {
            handleSignIn()
        }

        // "Create Account" → Register screen
        binding.tvCreateAccount.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
            overridePendingTransition(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
        }
    }

    private fun handleSignIn() {
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()

        if (email.isEmpty()) {
            binding.etEmail.error = "Please enter your email"
            binding.etEmail.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Please enter a valid email address"
            binding.etEmail.requestFocus()
            return
        }

        if (password.isEmpty()) {
            binding.etPassword.error = "Please enter your password"
            binding.etPassword.requestFocus()
            return
        }

        // Double-tap protection / loading state
        setLoadingState(true)

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val user = authResult.user
                if (user != null) {
                    // Ensure Firestore profile document exists safely without overwriting
                    UserRepository.ensureUserProfileExists(user) {
                        Toast.makeText(this, "Welcome back to EcoLocal!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, HomeActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        finish()
                    }
                } else {
                    setLoadingState(false)
                    Toast.makeText(this, "Sign in failed. Please try again.", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { exception ->
                setLoadingState(false)
                val message = when (exception) {
                    is FirebaseAuthInvalidUserException ->
                        "No account found with this email. Please register."
                    is FirebaseAuthInvalidCredentialsException ->
                        "Incorrect email or password. Please try again."
                    is FirebaseNetworkException ->
                        "Network error. Please check your internet connection."
                    else ->
                        exception.localizedMessage ?: "Sign in failed. Please check your credentials."
                }
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.btnSignIn.isClickable = !isLoading
        binding.btnSignIn.alpha = if (isLoading) 0.6f else 1.0f
    }
}
