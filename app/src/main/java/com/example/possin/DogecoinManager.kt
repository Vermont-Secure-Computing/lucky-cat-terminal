package com.example.possin

import android.content.Context
import org.bitcoinj.core.Address
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.crypto.HDKeyDerivation
import org.bitcoinj.script.Script
import org.libdohj.params.DogecoinMainNetParams
import org.bitcoinj.crypto.ChildNumber
import android.util.Log

class DogecoinManager(private val context: Context, private val xPub: String) {

    companion object {
        private const val PREFS_NAME = "DogecoinManagerPrefs"
        private const val LAST_INDEX_KEY = "lastIndex"
    }

    private val params = DogecoinMainNetParams.get()

    // Create a DeterministicKey from the xPub
    private val accountKey: DeterministicKey

    init {
        accountKey = DeterministicKey.deserializeB58(null, xPub, params)
    }

    private val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getAddress(): String {
        val lastIndex = getLastIndex()
        val newIndex = lastIndex + 1
        saveLastIndex(newIndex)
        return deriveAddress(newIndex)
    }

    private fun deriveAddress(index: Int): String {
        // Use derivation path m (directly using the master key without further derivation)
        val address = Address.fromKey(params, accountKey, Script.ScriptType.P2PKH)
        Log.d("ADDRESS", address.toString())
        return address.toString()
    }

    private fun getLastIndex(): Int {
        return sharedPreferences.getInt(LAST_INDEX_KEY, 0)
    }

    private fun saveLastIndex(index: Int) {
        sharedPreferences.edit().putInt(LAST_INDEX_KEY, index).apply()
    }
}