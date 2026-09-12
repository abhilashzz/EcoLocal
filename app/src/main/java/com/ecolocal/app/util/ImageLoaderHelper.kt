package com.ecolocal.app.util

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.ImageView
import androidx.annotation.DrawableRes

object ImageLoaderHelper {

    fun load(imageView: ImageView, imageUri: String?, @DrawableRes imageRes: Int) {
        if (!imageUri.isNullOrEmpty()) {
            try {
                val uri = Uri.parse(imageUri)
                val context = imageView.context
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream.close()
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap)
                        return
                    }
                }
            } catch (_: Exception) {
                try {
                    imageView.setImageURI(Uri.parse(imageUri))
                    return
                } catch (_: Exception) {
                    // Fall through to imageRes
                }
            }
        }
        if (imageRes != 0) {
            imageView.setImageResource(imageRes)
        }
    }
}
