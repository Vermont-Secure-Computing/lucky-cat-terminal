package com.vermont.possin.gif

import android.widget.ImageView
import pl.droidsonroids.gif.GifDrawable

object GifHandler {

    fun loadGif(
        imageView: ImageView,
        resId: Int,
        onFinished: (() -> Unit)? = null
    ) {
        try {
            val gif = GifDrawable(imageView.context.resources, resId)
            imageView.setImageDrawable(gif)
            gif.start()

            gif.addAnimationListener {
                onFinished?.invoke()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            onFinished?.invoke()
        }
    }
}
