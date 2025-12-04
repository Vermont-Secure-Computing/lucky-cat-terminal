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
import android.content.res.AssetManager
import android.util.Log
import org.bitcoinj.core.Address
import org.bitcoinj.core.Base58
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.core.SegwitAddress
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.crypto.HDKeyDerivation
import org.bitcoinj.params.AbstractBitcoinNetParams
import org.bitcoinj.params.Networks
import org.bitcoinj.script.Script
import java.security.MessageDigest
import java.util.Properties

class LitecoinMainNetParams : AbstractBitcoinNetParams() {
    init {
        id = ID_LITECOIN_MAINNET
        packetMagic = 0xfbc0b6dbL
        addressHeader = 48
        p2shHeader = 50
        dumpedPrivateKeyHeader = 176
        segwitAddressHrp = "ltc"

        bip32HeaderP2PKHpub = 0x019da462 // Ltub
        bip32HeaderP2PKHpriv = 0x019d9cfe // Ltpv
        bip32HeaderP2WPKHpub = 0x04b24746 // Mtub
        bip32HeaderP2WPKHpriv = 0x04b2430c // Mtpv

        // DNS Seeds
        dnsSeeds = arrayOf(
            "seed-a.litecoin.loshan.co.uk",
            "dnsseed.thrasher.io",
            "dnsseed.litecointools.com",
            "dnsseed.litecoinpool.org"
        )
    }

    override fun getPaymentProtocolId(): String {
        return "main"
    }

    companion object {
        private var instance: LitecoinMainNetParams? = null

        @JvmStatic
        fun get(): LitecoinMainNetParams {
            if (instance == null) {
                instance = LitecoinMainNetParams()
            }
            return instance!!
        }

        const val ID_LITECOIN_MAINNET = "org.litecoin.production"
    }
}

class LitecoinManager(private val context: Context, private val xPub: String) {

    companion object {
        const val PREFS_NAME = "LitecoinManagerPrefs"
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

        fun convertBitcoinXpubToLitecoin(xPub: String): String {
            Log.d("LitecoinManager", "Attempting to decode xPub: $xPub")

            val prefixMap = mapOf(
                "xpub" to byteArrayOf(0x01, 0x9d.toByte(), 0xa4.toByte(), 0x62.toByte()), // Ltub
                "ypub" to byteArrayOf(0x04, 0xb2.toByte(), 0x47.toByte(), 0x46.toByte()), // Mtub
                "zpub" to byteArrayOf(0x04, 0xb2.toByte(), 0x43.toByte(), 0x0c.toByte())  // Mtpv
            )

            val bitcoinPrefix = prefixMap.keys.find { xPub.startsWith(it) }
                ?: throw IllegalArgumentException("Invalid Bitcoin xPub prefix: $xPub")

            val litecoinPrefix = prefixMap[bitcoinPrefix]!!

            val decoded = try {
                Base58.decodeChecked(xPub)
            } catch (e: Exception) {
                throw IllegalArgumentException("Base58 decoding failed: ${e.message}")
            }

            if (decoded.size != 78) {
                throw IllegalArgumentException("Decoded xPub must be 78 bytes but got: ${decoded.size}")
            }

            Log.d("LitecoinManager", "Decoded xPub bytes: ${decoded.joinToString(", ")}")

            System.arraycopy(litecoinPrefix, 0, decoded, 0, 4)
            return addChecksumAndEncode(decoded)
        }

        fun isValidXpub(xPub: String, context: Context): Boolean {
            return try {
                val params: NetworkParameters = LitecoinMainNetParams.get()

                // Convert Bitcoin xPub to Litecoin xPub if necessary
                val convertedXpub = if (xPub.startsWith("xpub")) {
                    Log.d("LitecoinManager", "Converting Bitcoin xPub to Litecoin xPub...")
                    convertBitcoinXpubToLitecoin(xPub)
                } else {
                    xPub
                }

                // Validate the xPub
                DeterministicKey.deserializeB58(null, convertedXpub, params)
                Log.d("LitecoinManager", "xPub is valid: $convertedXpub")
                true
            } catch (e: Exception) {
                Log.e("LitecoinManager", "Invalid xPub: ${e.message}")
                false
            }
        }

        fun isValidAddress(address: String): Boolean {
            return try {
                val params: NetworkParameters = LitecoinMainNetParams.get()
                if (address.startsWith("ltc1")) {
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

    private val params: NetworkParameters = LitecoinMainNetParams.get()
    private val accountKey = try {
        DeterministicKey.deserializeB58(null, xPub, params)
    } catch (e: Exception) {
        Log.e("LitecoinManager", "Failed to initialize account key: ${e.message}")
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
            Log.e("LitecoinManager", "Error deriving address: ${e.message}")
            throw IllegalStateException("Failed to derive address.")
        }
    }

    private fun deriveAddress(index: Int): String {
        if (accountKey == null) {
            throw IllegalStateException("Account key is not initialized. Invalid xPub: $xPub")
        }

        val receivingKey = deriveKey(accountKey, index)
        val addressType = getAddressTypeFromConfig()
        return if (addressType == "legacy") {
            Address.fromKey(params, receivingKey, Script.ScriptType.P2PKH).toString()
        } else {
            SegwitAddress.fromKey(params, receivingKey).toString()
        }
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

    private fun getAddressTypeFromConfig(): String {
        val assetManager: AssetManager = context.assets
        val properties = Properties()

        try {
            assetManager.open("config.properties").use { inputStream ->
                properties.load(inputStream)
            }
        } catch (e: Exception) {
            Log.e("LitecoinManager", "Error reading config.properties", e)
        }

        return properties.getProperty("Litecoin_segwit_legacy", "segwit")
    }
}
