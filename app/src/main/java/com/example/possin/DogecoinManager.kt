package com.example.possin

import android.content.Context
import org.bitcoinj.core.Address
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.params.AbstractBitcoinNetParams
import org.bitcoinj.crypto.HDKeyDerivation
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.params.Networks
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.script.Script
import android.util.Log

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
        bip32HeaderP2PKHpub = 0x02facafd  // The headers are Dogecoin-specific.
        bip32HeaderP2PKHpriv = 0x02fac398
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
    }

    private val params: NetworkParameters = DogecoinMainNetParams.get()
    private val accountKey: DeterministicKey = DeterministicKey.deserializeB58(null, xPub, params)
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
        Log.d("DOGE", "Dogecoin address index $index")

        // Decode the xpub key
//        val parentKey = DeterministicKey.deserializeB58(null, xPub, params)
//
//        // Derive the child key at m/index electrum
//        val childKey = HDKeyDerivation.deriveChildKey(parentKey, ChildNumber(index, false))
//
//        // Get the address from the child key
//        val address = Address.fromKey(params, childKey, Script.ScriptType.P2PKH)
        val receivingKey = deriveKey(accountKey, index)

        // Create a P2WPKH (Pay to Witness Public Key Hash) address
//        val segwitAddress = SegwitAddress.fromKey(params, receivingKey)
        val address = Address.fromKey(params, receivingKey, Script.ScriptType.P2PKH)
        Log.d("ADDRESS", address.toString())
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