package com.example.possin

import android.content.Context
import org.bitcoinj.core.Address
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.crypto.HDKeyDerivation
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.script.Script

class DogecoinMainNetParams : MainNetParams() {
    init {
        id = "org.dogecoin.production"
        packetMagic = 0xc0c0c0c0
        addressHeader = 30
        p2shHeader = 22
//        acceptableAddressCodes = intArrayOf(addressHeader, p2shHeader)
        port = 22556
        dumpedPrivateKeyHeader = 158
    }

    override fun getId(): String {
        return id
    }

    override fun getPacketMagic(): Long {
        return packetMagic
    }

    override fun getAddressHeader(): Int {
        return addressHeader
    }

    override fun getP2SHHeader(): Int {
        return p2shHeader
    }

    override fun getDumpedPrivateKeyHeader(): Int {
        return dumpedPrivateKeyHeader
    }

    override fun getPaymentProtocolId(): String {
        return PAYMENT_PROTOCOL_ID_MAINNET
    }
}

class DogecoinManager(private val context: Context, private val xPub: String) {

    companion object {
        private const val PREFS_NAME = "DogecoinManagerPrefs"
        private const val LAST_INDEX_KEY = "lastIndex"
    }

    private val params: NetworkParameters = DogecoinMainNetParams()
    // Create a DeterministicKey from the xPub
    private val accountKey = DeterministicKey.deserializeB58(null, xPub, params)

    private val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getAddress(): String {
        val lastIndex = getLastIndex()
        val newIndex = lastIndex + 1
        saveLastIndex(newIndex)
        return deriveAddress(newIndex)
    }

    private fun deriveAddress(index: Int): String {
        val receivingKey = HDKeyDerivation.deriveChildKey(accountKey, index)
        val address = Address.fromKey(params, receivingKey, Script.ScriptType.P2PKH)
        return address.toString()
    }

    private fun getLastIndex(): Int {
        return sharedPreferences.getInt(LAST_INDEX_KEY, 0)
    }

    private fun saveLastIndex(index: Int) {
        sharedPreferences.edit().putInt(LAST_INDEX_KEY, index).apply()
    }
}