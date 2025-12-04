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
import android.util.Log
import java.io.File
import java.util.Properties

class MoneroManager(private val context: Context, private val privateViewKey: String, private val privateSpendKey: String) {

    companion object {
        const val PREFS_NAME = "MoneroManagerPrefs"
        const val LAST_INDEX_KEY = "lastIndex"

        // Static validation methods for view key
        fun isValidPrivateViewKey(viewKey: String): Boolean {
            return viewKey.length == 64 && viewKey.matches(Regex("[0-9a-fA-F]+"))
        }

        fun isValidAddress(address: String): Boolean {
            // Monero primary addresses start with '4' and are 95 characters long
            // Monero subaddresses start with '8' and are 95 characters long
            // Integrated addresses start with '4' and are 106 characters long
            return when {
                address.startsWith("4") && address.length == 95 -> true  // Primary address
                address.startsWith("4") && address.length == 106 -> true // Integrated address
                address.startsWith("8") && address.length == 95 -> true  // Subaddress
                else -> false  // Invalid address
            }
        }
    }

    private val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Get the Monero address from config.properties
    fun getAddress(): String? {
        val properties = Properties()
        try {
            val propertiesFile = File(context.filesDir, "config.properties")
            if (propertiesFile.exists()) {
                properties.load(propertiesFile.inputStream())
                return properties.getProperty("Monero_value") // Retrieve Monero address from config.properties
            }
        } catch (e: Exception) {
            Log.e("MoneroManager", "Error reading config.properties", e)
        }
        return null // Return null if the address is not found
    }

    fun saveLastIndex(index: Int) {
        with(sharedPreferences.edit()) {
            putInt(LAST_INDEX_KEY, index)
            apply()
        }
    }

    private fun getLastIndex(): Int {
        return sharedPreferences.getInt(LAST_INDEX_KEY, -1)
    }

    fun getPrivateViewKey(): String {
        return privateViewKey
    }

    fun getPrivateSpendKey(): String {
        return privateSpendKey
    }
}
