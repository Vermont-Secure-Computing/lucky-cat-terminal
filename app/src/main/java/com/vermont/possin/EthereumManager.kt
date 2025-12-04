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
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.crypto.HDKeyDerivation
import org.bitcoinj.params.MainNetParams
import org.web3j.crypto.Hash
import org.web3j.crypto.Keys
import org.web3j.utils.Numeric

class EthereumManager(private val context: Context, private val xPub: String) {

    companion object {
        const val PREFS_NAME = "EthereumManagerPrefs"
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

        /**
         * Validate and normalize Ethereum address.
         * @return Triple<isValid, normalizedAddressOrNull, warningMessageOrNull>
         */
        fun validateAddress(address: String): Triple<Boolean, String?, String?> {
            return try {
                if (address.length != 42 || !address.startsWith("0x")) {
                    return Triple(false, null, "Invalid length or missing 0x prefix")
                }

                val checksumAddress = Keys.toChecksumAddress(address)

                return if (address == checksumAddress) {
                    // fully valid, already checksummed
                    Triple(true, checksumAddress, null)
                } else if (address.lowercase() == checksumAddress.lowercase()) {
                    // valid but unchecksummed — normalize it
                    Triple(true, checksumAddress, "This address has no checksum. Would you like to continue?")
                } else {
                    Triple(false, null, "Invalid Ethereum address")
                }
            } catch (e: Exception) {
                Triple(false, null, "Invalid Ethereum address")
            }
        }
    }

    private val accountKey: DeterministicKey? =
        if (isValidXpub(xPub)) DeterministicKey.deserializeB58(xPub, MainNetParams.get()) else null
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
        Log.d("ETH", "Ethereum address index $index")

        val xpubKey = accountKey ?: throw IllegalStateException("Invalid xPub key")

        val path = listOf(ChildNumber(0, false), ChildNumber(index, false))
        val derivedKey = path.fold(xpubKey) { key, childNumber ->
            HDKeyDerivation.deriveChildKey(key, childNumber)
        }

        val publicKeyBytes = derivedKey.pubKeyPoint.getEncoded(false)
        val hash = Hash.sha3(publicKeyBytes.copyOfRange(1, publicKeyBytes.size))
        val addressBytes = hash.copyOfRange(hash.size - 20, hash.size)

        val address = Numeric.toHexString(addressBytes)
        Log.d("ADDRESS", address)
        return address
    }

    private fun getLastIndex(): Int {
        if (!sharedPreferences.contains(LAST_INDEX_KEY)) return -1
        return sharedPreferences.getInt(LAST_INDEX_KEY, -1)
    }

    fun getXpub(): String = xPub
}
