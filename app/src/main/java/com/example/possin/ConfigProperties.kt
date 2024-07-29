package com.example.possin

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

object ConfigProperties {

    private const val CONFIG_FILE_NAME = "config.properties"

    fun loadProperties(context: Context): Properties {
        val properties = Properties()
        val propertiesFile = File(context.filesDir, CONFIG_FILE_NAME)

        if (!propertiesFile.exists()) {
            propertiesFile.createNewFile()
            createDefaultProperties(context)
        }

        FileInputStream(propertiesFile).use { inputStream ->
            properties.load(inputStream)
        }

        return properties
    }

    private fun createDefaultProperties(context: Context) {
        val properties = Properties()
        properties.setProperty("default_key", "default_value")
        saveProperties(context, properties)
    }

    fun saveProperties(context: Context, properties: Properties) {
        val propertiesFile = File(context.filesDir, CONFIG_FILE_NAME)

        FileOutputStream(propertiesFile).use { outputStream ->
            properties.store(outputStream, null)
        }
    }
}
