package com.example.possin

import android.content.Context
import android.util.Log
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.crypto.HDKeyDerivation
import org.bitcoinj.params.MainNetParams
import org.web3j.crypto.Hash
import org.web3j.crypto.Keys
import org.web3j.utils.Numeric

class EthereumManager(private val context: Context, private val xPub: String) {

    companion object {
        private const val PREFS_NAME = "EthereumManagerPrefs"
        private const val LAST_INDEX_KEY = "lastIndex"
    }

    private val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getAddress(): String {
        val lastIndex = getLastIndex()
        val newIndex = if (lastIndex == -1) 0 else lastIndex + 1
        saveLastIndex(newIndex)
        return deriveAddress(newIndex)
    }

    private fun deriveAddress(index: Int): String {
        Log.d("ETH", "Ethereum address index $index")

        // Deserialize the xPub key using MainNetParams
        val xpubKey = DeterministicKey.deserializeB58(xPub, MainNetParams.get())

        // Derive the key using the non-hardened path m/0/index
        val path = listOf(
            ChildNumber(0, false),
            ChildNumber(index, false)
        )

        val derivedKey = path.fold(xpubKey) { key, childNumber ->
            HDKeyDerivation.deriveChildKey(key, childNumber)
        }

        // Get the public key bytes (uncompressed, starting with 0x04)
        val publicKeyBytes = derivedKey.pubKeyPoint.getEncoded(false)

        // Perform Keccak-256 hashing on the public key bytes without the prefix
        val hash = Hash.sha3(publicKeyBytes.copyOfRange(1, publicKeyBytes.size))

        // Extract the last 20 bytes of the hash to form the Ethereum address
        val addressBytes = hash.copyOfRange(hash.size - 20, hash.size)
        val address = Numeric.toHexString(addressBytes)
        Log.d("ADDRESS", address)
        return address
    }

    private fun getLastIndex(): Int {
        if (!sharedPreferences.contains(LAST_INDEX_KEY)) {
            return -1
        }
        return sharedPreferences.getInt(LAST_INDEX_KEY, -1)
    }

    private fun saveLastIndex(index: Int) {
        with(sharedPreferences.edit()) {
            putInt(LAST_INDEX_KEY, index)
            apply()
        }
    }
}