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
import com.ecolocal.app.databinding.ActivityRegisterBinding
import com.ecolocal.app.ui.main.HomeActivity
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest

/**
 * RegisterActivity — Create EcoLocal account screen.
 *
 * Integrated with Firebase Authentication (email/password) and Cloud Firestore
 * for creating the user profile document at users/{uid}.
 */
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
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

        // Profile photo upload (Cloudinary in later batch)
        binding.ivProfilePhoto.setOnClickListener {
            Toast.makeText(this, "Profile photo upload will be available with Cloudinary", Toast.LENGTH_SHORT).show()
        }

        // Create Account → Firebase Auth + Firestore
        binding.btnCreateAccount.setOnClickListener {
            handleRegister()
        }

        // "Sign In" → Login screen
        binding.tvSignIn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            overridePendingTransition(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
        }
    }

    private fun handleRegister() {
        val name = binding.etFullName.text?.toString()?.trim().orEmpty()
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()
        val confirmPassword = binding.etConfirmPassword.text?.toString().orEmpty()
        val location = binding.etLocation.text?.toString()?.trim().orEmpty()

        // Validation
        if (name.isEmpty()) {
            binding.etFullName.error = "Please enter your full name"
            binding.etFullName.requestFocus()
            return
        }

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
            binding.etPassword.error = "Please enter a password"
            binding.etPassword.requestFocus()
            return
        }

        if (password.length < 6) {
            binding.etPassword.error = "Password must be at least 6 characters"
            binding.etPassword.requestFocus()
            return
        }

        if (confirmPassword != password) {
            binding.etConfirmPassword.error = "Passwords do not match"
            binding.etConfirmPassword.requestFocus()
            return
        }

        // Double-tap protection / loading state
        setLoadingState(true)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val user = authResult.user
                if (user != null) {
                    // Update Firebase Auth display name
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()
                    user.updateProfile(profileUpdates)

                    // Create Firestore user document in users/{uid}
                    UserRepository.createUserProfile(
                        uid = user.uid,
                        name = name,
                        email = email,
                        locationText = location,
                        onSuccess = {
                            Toast.makeText(
                                this,
                                "Account created successfully! Welcome to EcoLocal.",
                                Toast.LENGTH_SHORT
                            ).show()

                            val intent = Intent(this, HomeActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(intent)
                            finish()
                        },
                        onFailure = { exception ->
                            setLoadingState(false)
                            val errorMsg = exception.localizedMessage ?: "Failed to save user profile"
                            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                        }
                    )
                } else {
                    setLoadingState(false)
                    Toast.makeText(this, "Registration failed. Please try again.", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { exception ->
                setLoadingState(false)
                val message = when (exception) {
                    is FirebaseAuthUserCollisionException ->
                        "This email address is already registered. Please sign in."
                    is FirebaseAuthWeakPasswordException ->
                        "Password is too weak. Please use at least 6 characters."
                    is FirebaseAuthInvalidCredentialsException ->
                        "The email address is invalid."
                    is FirebaseNetworkException ->
                        "Network error. Please check your internet connection."
                    else ->
                        exception.localizedMessage ?: "Registration failed. Please try again."
                }
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.btnCreateAccount.isEnabled = !isLoading
        binding.btnCreateAccount.alpha = if (isLoading) 0.6f else 1.0f
        binding.btnCreateAccount.text = if (isLoading) "Creating Account..." else getString(R.string.register_cta)
    }
}
