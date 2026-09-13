package com.ecolocal.app.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.ecolocal.app.R
import com.ecolocal.app.data.UserRepository
import com.ecolocal.app.databinding.ActivitySettingsBinding
import com.ecolocal.app.ui.auth.WelcomeActivity
import com.ecolocal.app.util.AppPreferences
import com.google.firebase.auth.FirebaseAuth

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            AppPreferences.setLocationSuggestionsEnabled(true)
            binding.switchLocationSuggestions.isChecked = true
            Toast.makeText(this, "Location suggestions enabled", Toast.LENGTH_SHORT).show()
        } else {
            // Permission denied: fallback gracefully
            AppPreferences.setLocationSuggestionsEnabled(false)
            binding.switchLocationSuggestions.isChecked = false
            Toast.makeText(this, "Location permission denied; suggestions disabled", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupThemeSelection()
        setupSwitches()
        setupActions()
    }

    private fun setupThemeSelection() {
        when (AppPreferences.getThemeMode()) {
            "LIGHT" -> binding.rbThemeLight.isChecked = true
            "DARK" -> binding.rbThemeDark.isChecked = true
            else -> binding.rbThemeSystem.isChecked = true
        }

        binding.rgTheme.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.rb_theme_light -> "LIGHT"
                R.id.rb_theme_dark -> "DARK"
                else -> "SYSTEM"
            }
            if (mode != AppPreferences.getThemeMode()) {
                AppPreferences.setThemeMode(mode)
                recreate()
            }
        }
    }

    private fun setupSwitches() {
        // Notifications switch
        binding.switchNotifications.isChecked = AppPreferences.isNotificationsEnabled()
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            AppPreferences.setNotificationsEnabled(isChecked)
            val msg = if (isChecked) "Push notifications enabled" else "Push notifications disabled"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        // Location suggestions switch
        binding.switchLocationSuggestions.isChecked = AppPreferences.isLocationSuggestionsEnabled()
        binding.switchLocationSuggestions.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                if (!fine && !coarse) {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                } else {
                    AppPreferences.setLocationSuggestionsEnabled(true)
                    Toast.makeText(this, "Location suggestions enabled", Toast.LENGTH_SHORT).show()
                }
            } else {
                AppPreferences.setLocationSuggestionsEnabled(false)
                Toast.makeText(this, "Location suggestions disabled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupActions() {
        binding.btnSettingsBack.setOnClickListener {
            finish()
        }

        binding.rowAbout.setOnClickListener {
            showAboutDialog()
        }

        binding.rowClearSearches.setOnClickListener {
            AppPreferences.clearRecentSearches()
            Toast.makeText(this, "Recent searches cleared", Toast.LENGTH_SHORT).show()
        }

        binding.btnSettingsLogout.setOnClickListener {
            performLogout()
        }
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("About EcoLocal")
            .setMessage(
                "EcoLocal v1.0\n\n" +
                "Sustainable Neighborhood Marketplace & Community Services platform.\n\n" +
                "Features smart AI search, nearby distance tracking, EcoMatch intelligent suggestions, verified member ratings, and real EcoPoints community incentives.\n\n" +
                "Developed for sustainable local circular communities."
            )
            .setPositiveButton("Close", null)
            .show()
    }

    private fun performLogout() {
        AlertDialog.Builder(this)
            .setTitle("Log Out")
            .setMessage("Are you sure you want to log out of EcoLocal?")
            .setPositiveButton("Log Out") { _, _ ->
                FirebaseAuth.getInstance().signOut()
                UserRepository.clearCache()

                val intent = Intent(this, WelcomeActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
