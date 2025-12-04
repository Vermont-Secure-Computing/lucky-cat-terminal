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
import org.bitcoinj.core.Base58
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.crypto.HDKeyDerivation
import org.bitcoinj.params.AbstractBitcoinNetParams
import org.bitcoinj.params.Networks
import java.nio.ByteBuffer

class ZcashMainNetParams : AbstractBitcoinNetParams() {
    init {
        id = ID_ZCASH_MAINNET
        packetMagic = 0x24E92764L
        addressHeader = 0x1CB8    // Prefix for t1 (P2PKH)
        p2shHeader = 0x1CBD       // Prefix for t3 (P2SH)
        dumpedPrivateKeyHeader = 128
        bip32HeaderP2PKHpub = 0x0488B21E // xpub
        bip32HeaderP2PKHpriv = 0x0488ADE4 // xprv

        dnsSeeds = arrayOf(
            "dnsseed.z.cash",
            "dnsseed.zclassic.community",
            "dnsseed.str4d.xyz"
        )
    }

    override fun getPaymentProtocolId(): String {
        return "zcash main"
    }

    companion object {
        private var instance: ZcashMainNetParams? = null

        @JvmStatic
        fun get(): ZcashMainNetParams {
            if (instance == null) {
                instance = ZcashMainNetParams()
            }
            return instance!!
        }

        const val ID_ZCASH_MAINNET = "org.zcash.production"
    }
}

/**
 * ZcashManager
 * Generates deterministic transparent Zcash (t1) addresses from an xPub.
 * This class is designed for display and address derivation only.
 */
class ZcashManager(private val context: Context? = null, private val xPub: String) {

    companion object {
        const val PREFS_NAME = "ZcashManagerPrefs"
        const val LAST_INDEX_KEY = "lastIndex"

        /**
         * Checks if the provided xPub appears valid for Zcash address derivation.
         */
        fun isValidXpub(xPub: String): Boolean {
            return try {
                val params: NetworkParameters = ZcashMainNetParams.get()
                if (xPub.length < 111) return false
                val decoded = Base58.decodeChecked(xPub)
                val header = ByteBuffer.wrap(decoded, 0, 4).int
                Log.d(
                    "ZcashManager",
                    "Decoded xPub header: $header, expected pub header: ${params.bip32HeaderP2PKHpub}"
                )
                header == params.bip32HeaderP2PKHpub
            } catch (e: Exception) {
                Log.e("ZcashManager", "Invalid xPub: ${e.message}")
                false
            }
        }

        /**
         * Basic validation for a transparent Zcash address (t1/t3).
         */
        fun isValidAddress(address: String): Boolean {
            return try {
                // Transparent addresses start with t1 (P2PKH) or t3 (P2SH)
                if (!address.startsWith("t1") && !address.startsWith("t3")) {
                    return false
                }

                // Decode using Base58Check to ensure valid checksum
                val decoded = org.bitcoinj.core.Base58.decodeChecked(address)

                // Version prefix bytes for Zcash transparent addresses
                val version = ((decoded[0].toInt() and 0xFF) shl 8) or (decoded[1].toInt() and 0xFF)
                version == 0x1CB8 || version == 0x1CBD
            } catch (e: Exception) {
                false
            }
        }
    }

    private val params: NetworkParameters = ZcashMainNetParams.get()
    private val accountKey =
        if (isValidXpub(xPub)) DeterministicKey.deserializeB58(null, xPub, params) else null
    private val sharedPreferences = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        // Register Zcash parameters for address parsing
        Networks.register(params)
    }

    /**
     * Generates a new transparent address and returns it with its derivation index.
     */
    fun getAddress(): Pair<String, Int>? {
        if (accountKey == null) {
            Log.e("ZcashManager", "Cannot derive address: invalid xPub")
            return null
        }

        val lastIndex = getLastIndex()
        val newIndex = if (lastIndex == -1) 0 else lastIndex + 1
        return Pair(deriveAddress(newIndex), newIndex)
    }

    fun saveLastIndex(index: Int) {
        with(sharedPreferences?.edit()) {
            this?.putInt(LAST_INDEX_KEY, index)
            this?.apply()
        }
    }

    /**
     * Derives the transparent address at the given index.
     * Path: m/44'/133'/0'/0/index (BIP44 standard for Zcash)
     */
    private fun deriveAddress(index: Int): String {
        Log.d("ZEC", "Zcash address index $index")

        val receivingKey = deriveKey(accountKey!!, index)
        val pubKeyHash = org.bitcoinj.core.Utils.sha256hash160(receivingKey.pubKey)

        // Zcash transparent prefix for t1 (P2PKH)
        val prefix = byteArrayOf(0x1C.toByte(), 0xB8.toByte())

        // Combine prefix + pubkey hash
        val payload = ByteArray(prefix.size + pubKeyHash.size)
        System.arraycopy(prefix, 0, payload, 0, prefix.size)
        System.arraycopy(pubKeyHash, 0, payload, prefix.size, pubKeyHash.size)

        // ✅ Compute Base58Check checksum manually
        val sha256 = java.security.MessageDigest.getInstance("SHA-256")
        val firstHash = sha256.digest(payload)
        val secondHash = sha256.digest(firstHash)
        val checksum = secondHash.copyOfRange(0, 4)

        // Append checksum
        val addressBytes = ByteArray(payload.size + 4)
        System.arraycopy(payload, 0, addressBytes, 0, payload.size)
        System.arraycopy(checksum, 0, addressBytes, payload.size, 4)

        // Encode Base58
        val address = org.bitcoinj.core.Base58.encode(addressBytes)
        Log.d("ZCASH_ADDRESS", address)
        return address
    }

    private fun deriveKey(masterKey: DeterministicKey, index: Int): DeterministicKey {
        // Non-hardened derivation for external chain
        val changeKey = HDKeyDerivation.deriveChildKey(masterKey, ChildNumber(0, false))
        return HDKeyDerivation.deriveChildKey(changeKey, index)
    }

    private fun getLastIndex(): Int {
        return sharedPreferences?.getInt(LAST_INDEX_KEY, -1) ?: -1
    }

    fun getXpub(): String {
        return xPub
    }
}
