package com.vermont.possin.utils

import android.content.Context
import java.io.File
import java.util.Properties

object ConfigUtils {
    fun isXpubCurrency(context: Context, currency: String): Boolean {
        // Only these currencies can actually have xpub
        val xpubCapable = setOf("Bitcoin", "Litecoin", "Dogecoin", "Dash", "Bitcoincash")

        if (!xpubCapable.contains(currency)) return false

        val props = Properties()
        val file = File(context.filesDir, "config.properties")
        if (file.exists()) props.load(file.inputStream())

        val type = props.getProperty("${currency}_type")
        return type.equals("xpub", ignoreCase = true)
    }
}
