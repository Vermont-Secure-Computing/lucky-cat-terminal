/*
 * Copyright 2024–2025 Vermont Secure Computing and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
http://www.apache.org/licenses/LICENSE-2.0

 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vermont.possin

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
