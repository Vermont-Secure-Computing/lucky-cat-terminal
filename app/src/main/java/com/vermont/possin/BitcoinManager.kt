package com.vermont.possin

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import org.bitcoinj.core.Address
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.core.SegwitAddress
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.crypto.HDKeyDerivation
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.script.Script
import java.util.Properties


class BitcoinManager(private val context: Context, private val xPub: String) {

    companion object {
        const val PREFS_NAME = "BitcoinManagerPrefs"
        const val LAST_INDEX_KEY = "lastIndex"

        // Static validation methods
        fun isValidXpub(xPub: String): Boolean {
            return try {
                val params: NetworkParameters = MainNetParams.get()
                if (xPub.length < 111) return false
                DeterministicKey.deserializeB58(null, xPub, params)
                true
            } catch (e: Exception) {
                false
            }
        }

        fun isValidAddress(address: String): Boolean {
            return try {
                val params: NetworkParameters = MainNetParams.get()
                if (address.startsWith("bc1")) {
                    SegwitAddress.fromBech32(params, address)
                } else {
                    Address.fromString(params, address)
                }
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    // Initialize network parameters for MainNet
    private val params: NetworkParameters = MainNetParams.get()
    // Create a DeterministicKey from the xPub
    private val accountKey = if (isValidXpub(xPub)) DeterministicKey.deserializeB58(null, xPub, params) else null

    private val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getAddress(): Pair<String, Int> {
        val lastIndex = getLastIndex()
        val newIndex = if (lastIndex == -1) 0 else lastIndex + 1
        return Pair(deriveAddress(newIndex), newIndex)
    }

    fun saveLastIndex(index: Int) {
        with(sharedPreferences.edit()) {
            putInt(LAST_INDEX_KEY, index)
            apply()
        }
    }


    private fun deriveAddress(index: Int): String {
        Log.d("BTC", "Bitcoin address index $index")
        val receivingKey = deriveKey(accountKey!!, index)

        // Create a P2WPKH (Pay to Witness Public Key Hash) address (Bech32)
        val addressType = getAddressTypeFromConfig()
        val address = if (addressType == "legacy") {
            Address.fromKey(params, receivingKey, Script.ScriptType.P2PKH)
        } else {
            SegwitAddress.fromKey(params, receivingKey)
        }
        Log.d("ADDRESS", address.toString())
        return address.toString()
    }

    private fun deriveKey(masterKey: DeterministicKey, index: Int): DeterministicKey {
        // Use non-hardened derivation path m/0/0/index for native SegWit (non-hardened)
        val changeKey = HDKeyDerivation.deriveChildKey(masterKey, ChildNumber(0, false))
//        val changeKey = HDKeyDerivation.deriveChildKey(accountKey, ChildNumber(0, false))
        return HDKeyDerivation.deriveChildKey(changeKey, index)
    }

    private fun getLastIndex(): Int {
        if (!sharedPreferences.contains(LAST_INDEX_KEY)) {
            return -1
        }
        return sharedPreferences.getInt(LAST_INDEX_KEY, -1)
    }

    fun getXpub(): String {
        return xPub
    }

    private fun getAddressTypeFromConfig(): String {
        val assetManager: AssetManager = context.assets
        val properties = Properties()

        try {
            assetManager.open("config.properties").use { inputStream ->
                properties.load(inputStream)
            }
        } catch (e: Exception) {
            Log.e("BitcoinManager", "Error reading config.properties", e)
        }

        return properties.getProperty("Bitcoin_segwit_legacy", "segwit")
    }
}
