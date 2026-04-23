package com.vermont.possin.gif

import android.os.Build
import android.widget.ImageView
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.load

object GifHandler {

    fun loadGif(
        imageView: ImageView,
        resId: Int,
        onFinished: (() -> Unit)? = null
    ) {
        val loader = ImageLoader.Builder(imageView.context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()

        imageView.load(resId, loader) {
            crossfade(false)
        }

        if (onFinished != null) {
            imageView.postDelayed({
                onFinished.invoke()
            }, 900)
        }
    }
}