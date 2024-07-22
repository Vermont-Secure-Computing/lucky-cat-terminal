package com.example.possin

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

class DashManager(private val context: Context, private val xPub: String) {

    companion object {
        const val PREFS_NAME = "DashManagerPrefs"
        const val LAST_INDEX_KEY = "lastIndex"
    }

    private val params: NetworkParameters = DashMainNetParams.get()
    private val accountKey: DeterministicKey = try {
        val decoded = Base58.decodeChecked(xPub)
        val header = (decoded[0].toInt() shl 24) or ((decoded[1].toInt() and 0xFF) shl 16) or ((decoded[2].toInt() and 0xFF) shl 8) or (decoded[3].toInt() and 0xFF)
        Log.d("DashManager", "xPub header bytes: 0x${header.toString(16).toUpperCase()}")
        if (header != DashMainNetParams.get().bip32HeaderP2PKHpub) {
            throw IllegalArgumentException("Unknown header bytes: 0x${header.toString(16).toUpperCase()} - Ensure xPub is for Dash")
        }
        DeterministicKey.deserializeB58(null, xPub, params)
    } catch (e: IllegalArgumentException) {
        Log.e("DashManager", "Failed to deserialize xPub: ${e.message}")
        throw e
    }

    private val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        // Register Dash parameters
        Networks.register(params)
    }

    fun getAddress(): Pair<String, Int> {
        val lastIndex = getLastIndex()
        val newIndex = if (lastIndex == -1) 0 else lastIndex + 1
        saveLastIndex(newIndex)
        return Pair(deriveAddress(newIndex), newIndex)
    }

    fun saveLastIndex(index: Int) {
        with(sharedPreferences.edit()) {
            putInt(LAST_INDEX_KEY, index)
            apply()
        }
    }

    private fun deriveAddress(index: Int): String {
        Log.d("DASH", "Dash address index $index")
        val receivingKey = deriveKey(accountKey, index)
        val address = Address.fromKey(params, receivingKey, Script.ScriptType.P2PKH)
        Log.d("ADDRESS", address.toString())
        return address.toString()
    }

    private fun deriveKey(masterKey: DeterministicKey, index: Int): DeterministicKey {
        // BIP44 path: m/44'/5'/0'/0/index (5 is the coin type for Dash)
        val purposeKey = HDKeyDerivation.deriveChildKey(masterKey, ChildNumber(44, true))
        val coinTypeKey = HDKeyDerivation.deriveChildKey(purposeKey, ChildNumber(5, true))
        val accountKey = HDKeyDerivation.deriveChildKey(coinTypeKey, ChildNumber(0, true))
        val changeKey = HDKeyDerivation.deriveChildKey(accountKey, ChildNumber(0, false))
        return HDKeyDerivation.deriveChildKey(changeKey, index)
    }

    private fun getLastIndex(): Int {
        if (!sharedPreferences.contains(LAST_INDEX_KEY)) {
            return -1
        }
        return sharedPreferences.getInt(LAST_INDEX_KEY, -1)
    }
}
