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
import java.nio.ByteBuffer

class DashMainNetParams : AbstractBitcoinNetParams() {
    init {
        id = ID_DASH_MAINNET
        packetMagic = 0xbf0c6bbdL
        addressHeader = 76
        p2shHeader = 16
        dumpedPrivateKeyHeader = 204
        bip32HeaderP2PKHpub = 0x02FE52F8  // Dash-specific headers.
        bip32HeaderP2PKHpriv = 0x02FE52CC

        dnsSeeds = arrayOf(
            "dnsseed.dash.org",
            "dnsseed.dashdot.io",
            "dnsseed.masternode.io",
            "dnsseed.dashpay.io"
        )
    }

    override fun getPaymentProtocolId(): String {
        return "dash main"
    }

    companion object {
        private var instance: DashMainNetParams? = null

        @JvmStatic
        fun get(): DashMainNetParams {
            if (instance == null) {
                instance = DashMainNetParams()
            }
            return instance!!
        }

        const val ID_DASH_MAINNET = "org.dash.production"
    }
}

class DashManager(private val context: Context? = null, private val xPub: String) {

    companion object {
        const val PREFS_NAME = "DashManagerPrefs"
        const val LAST_INDEX_KEY = "lastIndex"

        fun isValidXpub(xPub: String): Boolean {
            return try {
                val params: NetworkParameters = DashMainNetParams.get()
                if (xPub.length < 111) return false
                val decoded = Base58.decodeChecked(xPub)
                val header = ByteBuffer.wrap(decoded, 0, 4).int
                Log.d("DashManager", "Decoded xPub header: $header, expected pub header: ${params.bip32HeaderP2PKHpub}, expected priv header: ${params.bip32HeaderP2PKHpriv}")
                // Checking if the xPub has the correct header for Dash
                header == params.bip32HeaderP2PKHpub
            } catch (e: Exception) {
                Log.e("DashManager", "Invalid xPub: ${e.message}")
                false
            }
        }

        fun isValidAddress(address: String): Boolean {
            return try {
                val params: NetworkParameters = DashMainNetParams.get()
                if (address.length !in 26..35) return false
                Address.fromString(params, address)
                true
            } catch (e: Exception) {
                Log.e("DashManager", "Invalid address: ${e.message}")
                false
            }
        }
    }

    private val params: NetworkParameters = DashMainNetParams.get()
    private val accountKey = if (DashManager.isValidXpub(xPub)) DeterministicKey.deserializeB58(null, xPub, params) else null
    private val sharedPreferences = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        // Register Dash parameters
        Networks.register(params)
    }

    fun getAddress(): Pair<String, Int>? {
        if (accountKey == null) {
            Log.e("DashManager", "Cannot derive address: invalid xPub")
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

    private fun deriveAddress(index: Int): String {
        Log.d("DASH", "Dash address index $index")
        val receivingKey = deriveKey(accountKey!!, index)
        val address = Address.fromKey(params, receivingKey, Script.ScriptType.P2PKH)
        Log.d("ADDRESS", address.toString())
        return address.toString()
    }

    private fun deriveKey(masterKey: DeterministicKey, index: Int): DeterministicKey {
        // BIP44 path: m/44'/5'/0'/0/index (5 is the coin type for Dash)
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
