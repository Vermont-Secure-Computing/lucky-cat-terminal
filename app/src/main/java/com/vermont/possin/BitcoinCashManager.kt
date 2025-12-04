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
import org.bitcoinj.core.Address
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.crypto.HDKeyDerivation
import org.bitcoinj.params.AbstractBitcoinNetParams
import org.bitcoinj.params.Networks
import org.bitcoinj.script.Script
import java.util.logging.Logger

class BitcoinCashMainNetParams : AbstractBitcoinNetParams() {
    init {
        id = ID_BCH_MAINNET
        packetMagic = 0xe8f3e1e3L
        addressHeader = 0
        p2shHeader = 5
        dumpedPrivateKeyHeader = 128

        dnsSeeds = arrayOf(
            "seed.bitcoinabc.org",
            "seed-abc.bitcoinforks.org",
            "btccash-seeder.bitcoinunlimited.info",
            "seed.bitprim.org",
            "seed.deadalnix.me",
            "seeder.criptolayer.net"
        )
        bip32HeaderP2PKHpub = 0x0488b21e  // The headers are Bitcoin Cash-specific.
        bip32HeaderP2PKHpriv = 0x0488ade4
    }

    override fun getPaymentProtocolId(): String {
        return "main"
    }



    fun getP2shHeader(): Int {
        return p2shHeader
    }

    companion object {
        private var instance: BitcoinCashMainNetParams? = null

        @JvmStatic
        fun get(): BitcoinCashMainNetParams {
            if (instance == null) {
                instance = BitcoinCashMainNetParams()
            }
            return instance!!
        }

        const val ID_BCH_MAINNET = "org.bitcoincash.production"
    }
}

class BitcoinCashManager(private val context: Context, private val xPub: String) {

    companion object {
        private val logger = Logger.getLogger(BitcoinCashManager::class.java.name)
        const val PREFS_NAME = "BitcoinCashManagerPrefs"
        const val LAST_INDEX_KEY = "lastIndex"

        fun isValidXpub(xPub: String): Boolean {
            return try {
                val params: NetworkParameters = BitcoinCashMainNetParams.get()
                if (xPub.length < 111) return false
                DeterministicKey.deserializeB58(null, xPub, params)
                true
            } catch (e: Exception) {
                false
            }
        }

        fun isValidAddress(address: String): Boolean {
            val params: NetworkParameters = BitcoinCashMainNetParams.get()
            var formattedAddress = address

            // Check if the address starts with "bitcoincash:"
            if (!formattedAddress.startsWith("bitcoincash:", true)) {
                // Add "bitcoincash:" prefix if it's not present
                formattedAddress = "bitcoincash:$formattedAddress"
            }

            return try {
                // Validate as a Bitcoin Cash address
                Address.fromString(params, formattedAddress)
                true
            } catch (e: Exception) {
                // If it fails, try to validate as CashAddr without the "bitcoincash:" prefix
                CashAddress.isValidAddress(formattedAddress, params)
            }
        }

    }

    private val params: NetworkParameters = BitcoinCashMainNetParams.get()
    private val accountKey =
        if (isValidXpub(xPub)) DeterministicKey.deserializeB58(null, xPub, params) else null
    private val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        // Register Bitcoin Cash parameters
        Networks.register(params)
    }

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
        logger.info("Bitcoin Cash address index $index")
        val receivingKey = deriveKey(accountKey!!, index)

        // Create a P2PKH address and convert it to CashAddr format
        val legacyAddress = Address.fromKey(params, receivingKey, Script.ScriptType.P2PKH)
        logger.info("LEGACY: $legacyAddress")
        val cashAddr = CashAddress.toCashAddress(legacyAddress.toString(), params)
        logger.info("ADDRESS: $cashAddr")
        return cashAddr
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
