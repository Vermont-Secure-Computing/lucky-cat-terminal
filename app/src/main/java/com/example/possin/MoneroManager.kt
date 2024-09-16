package com.example.possin

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
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

    fun getAddress(index: Int): String {
        val addressType = getAddressTypeFromConfig()
        val newAddress = deriveAddress(privateViewKey, privateSpendKey, index, addressType)
        saveLastIndex(index)
        return newAddress
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

    // Simple method to derive an address based on private keys
    private fun deriveAddress(viewKey: String, spendKey: String, index: Int, addressType: String): String {
        // Dummy logic to generate an address (you can replace this with actual derivation logic)
        Log.d("Monero", "Deriving address for index $index with $addressType type")
        return "4DummyMoneroAddressForIndex$index"
    }

    private fun getAddressTypeFromConfig(): String {
        val assetManager: AssetManager = context.assets
        val properties = Properties()

        try {
            assetManager.open("config.properties").use { inputStream ->
                properties.load(inputStream)
            }
        } catch (e: Exception) {
            Log.e("MoneroManager", "Error reading config.properties", e)
        }

        return properties.getProperty("Monero_address_type", "standard")
    }

    fun getPrivateViewKey(): String {
        return privateViewKey
    }

    fun getPrivateSpendKey(): String {
        return privateSpendKey
    }
}
