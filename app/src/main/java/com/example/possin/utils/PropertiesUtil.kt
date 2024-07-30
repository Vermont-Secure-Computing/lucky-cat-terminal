package com.example.possin.utils

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.Properties

object PropertiesUtil {

    fun getProperty(context: Context, propertyName: String): String? {
        val properties = Properties()
        try {
            val file = File(context.filesDir, "api.properties")
            val inputStream = FileInputStream(file)
            properties.load(inputStream)
            inputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return properties.getProperty(propertyName)
    }
}
