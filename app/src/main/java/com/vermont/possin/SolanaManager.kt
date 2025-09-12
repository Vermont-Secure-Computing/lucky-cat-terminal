package com.vermont.possin

import android.content.Context
import android.util.Log
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.crypto.HDKeyDerivation
import org.bitcoinj.params.MainNetParams

class SolanaManager(private val context: Context, private val xPub: String) {

    companion object {
        const val PREFS_NAME = "SolanaManagerPrefs"
        const val LAST_INDEX_KEY = "lastIndex"

        fun isValidXpub(xPub: String): Boolean {
            return try {
                if (xPub.length < 111) return false
                DeterministicKey.deserializeB58(xPub, MainNetParams.get())
                true
            } catch (e: Exception) {
                false
            }
        }

        fun isValidAddress(address: String): Boolean {
            return try {

                if (address.length < 32 || address.length > 44) {
                    Log.e("VALIDATION", "Invalid length: ${address.length}.")
                    return false
                }


                val decoded = decodeBase58(address)

                
                if (decoded.size != 32) {
                    Log.w("VALIDATION", "Non-standard decoded size: ${decoded.size}. Address still accepted.")
                }

                // Passed all checks
                true
            } catch (e: Exception) {
                // Catch any decoding errors
                Log.e("VALIDATION", "Decoding failed: ${e.message}")
                false
            }
        }

        private fun decodeBase58(input: String): ByteArray {
            return org.bitcoinj.core.Base58.decode(input)
        }
    }

    private val accountKey: DeterministicKey? = if (isValidXpub(xPub)) DeterministicKey.deserializeB58(xPub, MainNetParams.get()) else null
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
        Log.d("SOL", "Solana address index $index")

        // Deserialize the xPub key using MainNetParams
        val xpubKey = accountKey ?: throw IllegalStateException("Invalid xPub key")

        // Derive the key using the non-hardened path m/0/index
        val path = listOf(
            ChildNumber(0, false),
            ChildNumber(index, false)
        )

        val derivedKey = path.fold(xpubKey) { key, childNumber ->
            HDKeyDerivation.deriveChildKey(key, childNumber)
        }

        // Get the public key bytes
        val publicKeyBytes = derivedKey.pubKeyPoint.getEncoded(false)

        // Convert to Solana base58 address format (assume proper conversion library or function is available)
        val address = encodeBase58(publicKeyBytes)
        Log.d("ADDRESS", address)
        return address
    }

    private fun encodeBase58(input: ByteArray): String {
        // Implement Base58 encoding for Solana address
        // Use a library or write custom logic for encoding
        return "Base58EncodedAddress" // Placeholder, replace with actual implementation
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
