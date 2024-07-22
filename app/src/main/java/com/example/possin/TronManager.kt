package com.example.possin

import android.content.Context
import android.util.Log
import com.example.possin.utils.TronAddressValidator
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.crypto.HDKeyDerivation
import org.bitcoinj.params.MainNetParams
import org.web3j.crypto.Hash

class TronManager(private val context: Context, private val xPub: String) {

    companion object {
        const val PREFS_NAME = "TronManagerPrefs"
        const val LAST_INDEX_KEY = "lastIndex"

        private val tronAddressValidator = TronAddressValidator()

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
            Log.e("TRON", "Validating address: $address")

            if (address.length != 34 || !address.startsWith("T")) {
                Log.e("TRON", "Invalid length or prefix")
                return false
            }

            return tronAddressValidator.validate(address)
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
        Log.d("TRX", "Tron address index $index")

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

        // Get the public key bytes (uncompressed, starting with 0x04)
        val publicKeyBytes = derivedKey.pubKeyPoint.getEncoded(false)

        // Perform Keccak-256 hashing on the public key bytes without the prefix
        val hash = Hash.sha3(publicKeyBytes.copyOfRange(1, publicKeyBytes.size))

        // Extract the last 20 bytes of the hash to form the Tron address
        val addressBytes = hash.copyOfRange(hash.size - 20, hash.size)
        val addressWithPrefix = byteArrayOf(0x41.toByte()) + addressBytes

        val base58Address = Base58.encodeChecked(addressWithPrefix)
        Log.d("ADDRESS", base58Address)
        return base58Address
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
