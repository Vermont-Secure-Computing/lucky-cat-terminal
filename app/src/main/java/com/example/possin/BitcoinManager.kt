package com.example.possin

import android.content.Context
import android.util.Log
import org.bitcoinj.core.Address
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.crypto.HDKeyDerivation
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.script.Script

class BitcoinManager(private val context: Context, private val xPub: String) {

    companion object {
        private const val PREFS_NAME = "BitcoinManagerPrefs"
        private const val LAST_INDEX_KEY = "lastIndex"
    }

    // Initialize network parameters for MainNet
    private val params: NetworkParameters = MainNetParams.get()
    // Create a DeterministicKey from the xPub
    private val accountKey = DeterministicKey.deserializeB58(null, xPub, params)

    private val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getAddress(): String {
        val lastIndex = getLastIndex()
        val newIndex = lastIndex + 1
        saveLastIndex(newIndex)
        return deriveAddress(newIndex)
    }

    private fun deriveAddress(index: Int): String {
        Log.d("BTC", "Bitcoin address index $index")
        val receivingKey = HDKeyDerivation.deriveChildKey(accountKey, index)
        val address = Address.fromKey(params, receivingKey, Script.ScriptType.P2PKH)
        return address.toString()
    }

    private fun getLastIndex(): Int {
        return sharedPreferences.getInt(LAST_INDEX_KEY, 0)
    }

    private fun saveLastIndex(index: Int) {
        sharedPreferences.edit().putInt(LAST_INDEX_KEY, index).apply()
    }
}