package com.field.survey.ui.util

import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.ImageView
import com.field.survey.domain.model.DistributionPoint
import com.squareup.picasso.Picasso

fun ImageView.loadDpImage(dp: DistributionPoint) {
    when {
        !dp.photoUrl.isNullOrBlank() -> {
            Picasso.get()
                .load(dp.photoUrl)
                .into(this)
        }
        !dp.imageBase64.isNullOrBlank() -> {
            try {
                val bytes = Base64.decode(dp.imageBase64, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                setImageBitmap(bmp)
            } catch (_: Exception) {
                setImageResource(android.R.drawable.ic_menu_gallery)
            }
        }
        else -> setImageResource(android.R.drawable.ic_menu_gallery)
    }
}

fun ImageView.loadProfileImage(photoUrl: String) {
    if (photoUrl.isNotBlank()) {
        Picasso.get()
            .load(photoUrl)
            .into(this)
    }
}
