package com.vermont.possin.gif

import android.widget.ImageView

object GifHandler {
    fun loadGif(
        imageView: ImageView,
        resId: Int,
        onFinished: (() -> Unit)? = null
    ) {

        // FDroid → no GIF support, show static resource instead
        imageView.setImageResource(resId)

        // If no callback → do nothing (like neku.gif)
        if (onFinished == null) return

        // If callback exists (payment dialog), add a delay
        imageView.postDelayed({
            onFinished.invoke()
        }, 850) // perfect smooth timing
    }
}
