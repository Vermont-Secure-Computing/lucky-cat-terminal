package com.example.possin

import android.content.Context
import android.util.Log
import org.bitcoinj.core.Address
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.crypto.HDKeyDerivation
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptBuilder
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.core.SegwitAddress

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
                if (address.length !in 26..35) return false
                Address.fromString(params, address)
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

        // Create a P2WPKH (Pay to Witness Public Key Hash) address
        val address = Address.fromKey(params, receivingKey, Script.ScriptType.P2PKH)
        Log.d("ADDRESS", address.toString())
        return address.toString()
    }

    private fun deriveKey(masterKey: DeterministicKey, index: Int): DeterministicKey {
        // Use non-hardened derivation path m/0/index
        val changeKey = HDKeyDerivation.deriveChildKey(masterKey, ChildNumber(0, false))
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
}
