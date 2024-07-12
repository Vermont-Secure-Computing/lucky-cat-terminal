package com.example.possin

import android.content.Context
import android.util.Log
import org.bitcoinj.core.Address
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.params.AbstractBitcoinNetParams
import org.bitcoinj.crypto.HDKeyDerivation
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.script.Script
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.core.Bech32
import org.bitcoinj.core.SegwitAddress
import org.bitcoinj.params.Networks

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

        // DNS Seeds
        dnsSeeds = arrayOf(
            "seed-a.litecoin.loshan.co.uk",
            "dnsseed.thrasher.io",
            "dnsseed.litecointools.com",
            "dnsseed.litecoinpool.org"
        )
    }


//    override fun getId(): String {
//        return id
//    }
//
//    override fun getPacketMagic(): Long {
//        return packetMagic
//    }
//
//    override fun getAddressHeader(): Int {
//        return addressHeader
//    }
//
//    override fun getP2SHHeader(): Int {
//        return p2shHeader
//    }
//
//    override fun getDumpedPrivateKeyHeader(): Int {
//        return dumpedPrivateKeyHeader
//    }
//
//    override fun getSegwitAddressHrp(): String {
//        return segwitAddressHrp
//    }

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
    }

    private val params: NetworkParameters = LitecoinMainNetParams()
    private val accountKey = DeterministicKey.deserializeB58(null, xPub, params)
    private val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        // Register Dogecoin parameters
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
        Log.d("LTC", "Litecoin address index $index")
        val receivingKey = deriveKey(accountKey, index)

        // Create a P2WPKH (Pay to Witness Public Key Hash) address with correct HRP for Litecoin
        val address = Address.fromKey(params, receivingKey, Script.ScriptType.P2PKH)
//        Log.d("ADDRESS", segwitAddress.toString())
        Log.d("ADDRESS", address.toString())
//        Log.d("ADDRESS", segwitAddress.toString())
        return address.toString()
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

}