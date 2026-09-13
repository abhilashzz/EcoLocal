package com.ecolocal.app.util

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

object AppPreferences {

    private const val PREFS_NAME = "ecolocal_preferences"
    private const val KEY_THEME = "key_theme_mode" // "LIGHT", "DARK", "SYSTEM"
    private const val KEY_NOTIFICATIONS = "key_notifications_enabled"
    private const val KEY_LOCATION_SUGGESTIONS = "key_location_suggestions"
    private const val KEY_RECENT_SEARCHES = "key_recent_searches"
    private const val KEY_RECENT_CATEGORIES = "key_recent_categories"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getThemeMode(): String {
        return prefs.getString(KEY_THEME, "SYSTEM") ?: "SYSTEM"
    }

    fun setThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME, mode).apply()
        applyTheme(mode)
    }

    fun applyTheme(mode: String) {
        val nightMode = when (mode) {
            "LIGHT" -> AppCompatDelegate.MODE_NIGHT_NO
            "DARK" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    fun isNotificationsEnabled(): Boolean {
        return prefs.getBoolean(KEY_NOTIFICATIONS, true)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply()
    }

    fun isLocationSuggestionsEnabled(context: Context? = null): Boolean {
        // Requirement 10: "Default: OFF"
        return prefs.getBoolean(KEY_LOCATION_SUGGESTIONS, false)
    }

    fun setLocationSuggestionsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOCATION_SUGGESTIONS, enabled).apply()
    }

    fun getRecentSearches(): List<String> {
        val raw = prefs.getString(KEY_RECENT_SEARCHES, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split("|||").filter { it.isNotBlank() }
    }

    fun addRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return
        val current = getRecentSearches().toMutableList()
        current.removeAll { it.equals(trimmed, ignoreCase = true) }
        current.add(0, trimmed)
        val limited = current.take(10)
        prefs.edit().putString(KEY_RECENT_SEARCHES, limited.joinToString("|||")).apply()
    }

    fun clearRecentSearches() {
        prefs.edit().remove(KEY_RECENT_SEARCHES).apply()
    }

    fun getRecentCategories(): List<String> {
        val raw = prefs.getString(KEY_RECENT_CATEGORIES, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split("|||").filter { it.isNotBlank() }
    }

    fun recordCategoryInteraction(category: String) {
        if (category.isBlank() || category.equals("All", ignoreCase = true)) return
        val current = getRecentCategories().toMutableList()
        current.removeAll { it.equals(category, ignoreCase = true) }
        current.add(0, category)
        val limited = current.take(8)
        prefs.edit().putString(KEY_RECENT_CATEGORIES, limited.joinToString("|||")).apply()
    }
}
