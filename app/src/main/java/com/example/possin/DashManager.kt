package com.example.possin

import android.content.Context
import android.util.Log
import org.bitcoinj.core.Address
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.crypto.HDKeyDerivation
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.script.Script

class DashMainNetParams : MainNetParams() {
    init {
        id = "org.dash.production"
        packetMagic = 0xbf0c6bbdL
        addressHeader = 76
        p2shHeader = 16
        port = 9999
        dumpedPrivateKeyHeader = 204
        bip32HeaderP2PKHpub = 0x02FE52F8
        bip32HeaderP2PKHpriv = 0x02FE52CC
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
    }
}

class DashManager(private val context: Context, private val xPub: String) {

    companion object {
        private const val PREFS_NAME = "DashManagerPrefs"
        private const val LAST_INDEX_KEY = "lastIndex"
    }

//    private val params: NetworkParameters = DashMainNetParams.get()
//    private val accountKey: DeterministicKey? = try {
//        XPubUtils.deserializeDashXPUB(xPub, params as MainNetParams)
//    } catch (e: Exception) {
//        Log.e("DashManager", "Failed to deserialize xPub: ${e.message}")
//        null
//    }
//
//    private val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
//
//    fun getAddress(): String? {
//        if (accountKey == null) {
//            Log.e("DashManager", "accountKey is null, cannot derive address")
//            return null
//        }
//
//        val lastIndex = getLastIndex()
//        val newIndex = lastIndex + 1
//        saveLastIndex(newIndex)
//        return deriveAddress(newIndex)
//    }
//
//    private fun deriveAddress(index: Int): String? {
//        if (accountKey == null) {
//            Log.e("DashManager", "accountKey is null, cannot derive address")
//            return null
//        }
//
//        return try {
//            val receivingKey = HDKeyDerivation.deriveChildKey(accountKey, index)
//            val address = Address.fromKey(params, receivingKey, Script.ScriptType.P2PKH)
//            address.toString()
//        } catch (e: Exception) {
//            Log.e("DashManager", "Failed to derive address: ${e.message}")
//            null
//        }
//    }
//
//    private fun getLastIndex(): Int {
//        return sharedPreferences.getInt(LAST_INDEX_KEY, 0)
//    }
//
//    private fun saveLastIndex(index: Int) {
//        sharedPreferences.edit().putInt(LAST_INDEX_KEY, index).apply()
//    }
}