package com.vermont.possin.gif

import android.widget.ImageView

object GifHandler {

    fun loadGif(
        imageView: ImageView,
        resId: Int,
        onFinished: (() -> Unit)? = null
    ) {
        try {
            imageView.setImageResource(resId)

            if (onFinished != null) {
                imageView.postDelayed({
                    onFinished.invoke()
                }, 850)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            onFinished?.invoke()
        }
    }
}