package com.ecolocal.app.util

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.ecolocal.app.R
import com.ecolocal.app.ui.main.CommunityServicesActivity
import com.ecolocal.app.ui.main.HomeActivity
import com.ecolocal.app.ui.main.MarketplaceActivity

/**
 * Navigation item enum identifying the active bottom navigation tab.
 */
enum class NavItem {
    HOME,
    SERVICES,
    MARKET,
    ACTIVITY,
    PROFILE
}

/**
 * Shared BottomNavHelper to bind and style bottom navigation tabs consistently.
 * Ensures the selected tab uses a rounded stadium pill background enclosing both icon and label,
 * while unselected tabs remain transparent.
 */
object BottomNavHelper {

    fun setup(
        activity: Activity,
        navRoot: View,
        selected: NavItem,
        onReselect: (() -> Unit)? = null
    ) {
        val pillHome = navRoot.findViewById<LinearLayout>(R.id.nav_pill_home)
        val iconHome = navRoot.findViewById<ImageView>(R.id.nav_icon_home)
        val labelHome = navRoot.findViewById<TextView>(R.id.nav_label_home)
        val itemHome = navRoot.findViewById<LinearLayout>(R.id.nav_item_home)

        val pillServices = navRoot.findViewById<LinearLayout>(R.id.nav_pill_services)
        val iconServices = navRoot.findViewById<ImageView>(R.id.nav_icon_services)
        val labelServices = navRoot.findViewById<TextView>(R.id.nav_label_services)
        val itemServices = navRoot.findViewById<LinearLayout>(R.id.nav_item_services)

        val pillMarket = navRoot.findViewById<LinearLayout>(R.id.nav_pill_market)
        val iconMarket = navRoot.findViewById<ImageView>(R.id.nav_icon_market)
        val labelMarket = navRoot.findViewById<TextView>(R.id.nav_label_market)
        val itemMarket = navRoot.findViewById<LinearLayout>(R.id.nav_item_market)

        val pillActivity = navRoot.findViewById<LinearLayout>(R.id.nav_pill_activity)
        val iconActivity = navRoot.findViewById<ImageView>(R.id.nav_icon_activity)
        val labelActivity = navRoot.findViewById<TextView>(R.id.nav_label_activity)
        val itemActivity = navRoot.findViewById<LinearLayout>(R.id.nav_item_activity)

        val pillProfile = navRoot.findViewById<LinearLayout>(R.id.nav_pill_profile)
        val iconProfile = navRoot.findViewById<ImageView>(R.id.nav_icon_profile)
        val labelProfile = navRoot.findViewById<TextView>(R.id.nav_label_profile)
        val itemProfile = navRoot.findViewById<LinearLayout>(R.id.nav_item_profile)

        val selectedColor = ContextCompat.getColor(activity, R.color.eco_nav_selected_text)
        val unselectedColor = ContextCompat.getColor(activity, R.color.eco_nav_unselected)

        fun applyStyle(pill: LinearLayout, icon: ImageView, label: TextView, isSelected: Boolean) {
            if (isSelected) {
                pill.setBackgroundResource(R.drawable.bg_nav_selected_pill)
                icon.setColorFilter(selectedColor)
                label.setTextColor(selectedColor)
                label.setTypeface(null, Typeface.BOLD)
            } else {
                pill.background = null
                icon.setColorFilter(unselectedColor)
                label.setTextColor(unselectedColor)
                label.setTypeface(null, Typeface.NORMAL)
            }
        }

        applyStyle(pillHome, iconHome, labelHome, selected == NavItem.HOME)
        applyStyle(pillServices, iconServices, labelServices, selected == NavItem.SERVICES)
        applyStyle(pillMarket, iconMarket, labelMarket, selected == NavItem.MARKET)
        applyStyle(pillActivity, iconActivity, labelActivity, selected == NavItem.ACTIVITY)
        applyStyle(pillProfile, iconProfile, labelProfile, selected == NavItem.PROFILE)

        fun navigateTo(targetClass: Class<*>) {
            val intent = Intent(activity, targetClass).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            activity.startActivity(intent)
            activity.overridePendingTransition(0, 0)
        }

        itemHome.setOnClickListener {
            if (selected != NavItem.HOME) {
                navigateTo(HomeActivity::class.java)
            } else {
                onReselect?.invoke()
            }
        }

        itemServices.setOnClickListener {
            if (selected != NavItem.SERVICES) {
                navigateTo(CommunityServicesActivity::class.java)
            } else {
                onReselect?.invoke()
            }
        }

        itemMarket.setOnClickListener {
            if (selected != NavItem.MARKET) {
                navigateTo(MarketplaceActivity::class.java)
            } else {
                onReselect?.invoke()
            }
        }

        itemActivity.setOnClickListener {
            if (selected != NavItem.ACTIVITY) {
                navigateTo(com.ecolocal.app.ui.activity.MyActivityActivity::class.java)
            } else {
                onReselect?.invoke()
            }
        }

        itemProfile.setOnClickListener {
            if (selected != NavItem.PROFILE) {
                navigateTo(com.ecolocal.app.ui.profile.ProfileActivity::class.java)
            } else {
                onReselect?.invoke()
            }
        }
    }
}
