package com.ecolocal.app.util

import android.widget.ImageView
import androidx.annotation.DrawableRes
import coil.load
import com.ecolocal.app.R

object ImageLoaderHelper {

    fun load(imageView: ImageView, imageUri: String?, @DrawableRes imageRes: Int = 0) {
        val fallback = if (imageRes != 0) imageRes else R.drawable.img_mkt_desk

        if (!imageUri.isNullOrEmpty()) {
            try {
                imageView.load(imageUri) {
                    crossfade(true)
                    placeholder(fallback)
                    error(fallback)
                }
            } catch (_: Exception) {
                imageView.setImageResource(fallback)
            }
        } else {
            imageView.setImageResource(fallback)
        }
    }

    fun loadAvatar(imageView: ImageView, avatarUrl: String?, @DrawableRes avatarRes: Int = 0) {
        val fallback = if (avatarRes != 0) avatarRes else R.drawable.img_avatar_nimal
        if (!avatarUrl.isNullOrEmpty()) {
            try {
                imageView.load(avatarUrl) {
                    crossfade(true)
                    placeholder(fallback)
                    error(fallback)
                }
            } catch (_: Exception) {
                imageView.setImageResource(fallback)
            }
        } else {
            imageView.setImageResource(fallback)
        }
    }
}
