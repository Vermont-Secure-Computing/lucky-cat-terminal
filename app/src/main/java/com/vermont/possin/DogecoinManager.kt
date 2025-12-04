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
import org.bitcoinj.core.Address
import org.bitcoinj.core.Base58
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.crypto.HDKeyDerivation
import org.bitcoinj.params.AbstractBitcoinNetParams
import org.bitcoinj.params.Networks
import org.bitcoinj.script.Script
import java.security.MessageDigest

class DogecoinMainNetParams : AbstractBitcoinNetParams() {
    init {
        id = ID_DOGE_MAINNET
        packetMagic = 0xc0c0c0c0L
        addressHeader = 30
        p2shHeader = 22
        dumpedPrivateKeyHeader = 158
        segwitAddressHrp = "doge"

        dnsSeeds = arrayOf(
            "seed.dogecoin.com",
            "seed.mophides.com",
            "seed.dglibrary.org",
            "seed.dogechain.info"
        )
        bip32HeaderP2PKHpub = 0x02facafd  // dgub (Dogecoin Legacy xPub header)
        bip32HeaderP2PKHpriv = 0x02fac398 // dgub (Dogecoin Legacy xPriv header)
    }

    override fun getPaymentProtocolId(): String {
        return "main"
    }

    companion object {
        private var instance: DogecoinMainNetParams? = null

        @JvmStatic
        fun get(): DogecoinMainNetParams {
            if (instance == null) {
                instance = DogecoinMainNetParams()
            }
            return instance!!
        }

        const val ID_DOGE_MAINNET = "org.dogecoin.production"
    }
}

class DogecoinManager(private val context: Context, private val xPub: String) {

    companion object {
        const val PREFS_NAME = "DogecoinManagerPrefs"
        const val LAST_INDEX_KEY = "lastIndex"

        private fun sha256(input: ByteArray): ByteArray {
            val digest = MessageDigest.getInstance("SHA-256")
            return digest.digest(input)
        }

        private fun addChecksumAndEncode(data: ByteArray): String {
            val hash1 = sha256(data)
            val hash2 = sha256(hash1)
            val checksum = hash2.copyOfRange(0, 4)

            val dataWithChecksum = ByteArray(data.size + 4)
            System.arraycopy(data, 0, dataWithChecksum, 0, data.size)
            System.arraycopy(checksum, 0, dataWithChecksum, data.size, 4)

            return Base58.encode(dataWithChecksum)
        }

        fun convertBitcoinXpubToDogecoin(xPub: String): String {
            Log.d("DogecoinManager", "Attempting to decode xPub: $xPub")

            val prefixMap = mapOf(
                "xpub" to byteArrayOf(0x02, 0xfa.toByte(), 0xca.toByte(), 0xfd.toByte()) // dgub
            )

            val bitcoinPrefix = prefixMap.keys.find { xPub.startsWith(it) }
                ?: throw IllegalArgumentException("Invalid Bitcoin xPub prefix: $xPub")

            val dogecoinPrefix = prefixMap[bitcoinPrefix]!!

            val decoded = try {
                Base58.decodeChecked(xPub)
            } catch (e: Exception) {
                throw IllegalArgumentException("Base58 decoding failed: ${e.message}")
            }

            if (decoded.size != 78) {
                throw IllegalArgumentException("Decoded xPub must be 78 bytes but got: ${decoded.size}")
            }

            Log.d("DogecoinManager", "Decoded xPub bytes: ${decoded.joinToString(", ")}")

            System.arraycopy(dogecoinPrefix, 0, decoded, 0, 4)
            return addChecksumAndEncode(decoded)
        }

        fun isValidXpub(xPub: String, context: Context): Boolean {
            return try {
                val params: NetworkParameters = DogecoinMainNetParams.get()

                // Convert Bitcoin xPub to Dogecoin xPub if necessary
                val convertedXpub = if (xPub.startsWith("xpub")) {
                    Log.d("DogecoinManager", "Converting Bitcoin xPub to Dogecoin xPub...")
                    convertBitcoinXpubToDogecoin(xPub)
                } else {
                    xPub
                }

                // Validate the xPub
                DeterministicKey.deserializeB58(null, convertedXpub, params)
                Log.d("DogecoinManager", "xPub is valid: $convertedXpub")
                true
            } catch (e: Exception) {
                Log.e("DogecoinManager", "Invalid xPub: ${e.message}")
                false
            }
        }


        fun isValidAddress(address: String): Boolean {
            return try {
                val params: NetworkParameters = DogecoinMainNetParams.get()
                if (address.length !in 26..35) return false
                Address.fromString(params, address)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    private val params: NetworkParameters = DogecoinMainNetParams.get()
    private val accountKey = try {
        DeterministicKey.deserializeB58(null, xPub, params)
    } catch (e: Exception) {
        Log.e("DogecoinManager", "Failed to initialize account key: ${e.message}")
        null
    }
    private val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        Networks.register(params)
    }

    fun getAddress(): Pair<String, Int> {
        if (accountKey == null) {
            throw IllegalStateException("Account key is not initialized.")
        }

        val lastIndex = getLastIndex()
        val newIndex = if (lastIndex == -1) 0 else lastIndex + 1
        return try {
            Pair(deriveAddress(newIndex), newIndex)
        } catch (e: Exception) {
            Log.e("DogecoinManager", "Error deriving address: ${e.message}")
            throw IllegalStateException("Failed to derive address.")
        }
    }

    private fun deriveAddress(index: Int): String {
        if (accountKey == null) {
            throw IllegalStateException("Account key is not initialized. Invalid xPub: $xPub")
        }

        val receivingKey = deriveKey(accountKey, index)
        return Address.fromKey(params, receivingKey, Script.ScriptType.P2PKH).toString()
    }

    private fun deriveKey(masterKey: DeterministicKey, index: Int): DeterministicKey {
        val changeKey = HDKeyDerivation.deriveChildKey(masterKey, ChildNumber(0, false))
        return HDKeyDerivation.deriveChildKey(changeKey, index)
    }

    private fun getLastIndex(): Int {
        return sharedPreferences.getInt(LAST_INDEX_KEY, -1)
    }

    fun getXpub(): String {
        return xPub
    }
}
