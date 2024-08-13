package com.example.possin

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import android.widget.Toast
import org.bitcoinj.core.Address
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.core.SegwitAddress
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.crypto.HDKeyDerivation
import org.bitcoinj.params.AbstractBitcoinNetParams
import org.bitcoinj.params.Networks
import org.bitcoinj.script.Script
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

        fun isValidXpub(xPub: String, context: Context): Boolean {
            return try {
                val params: NetworkParameters = LitecoinMainNetParams.get()
                val validPrefixes = listOf("Ltub", "Mtub", "zpub")
                if (xPub.startsWith("zpub")) {
                    Toast.makeText(context, "Make sure your xpub is correct", Toast.LENGTH_LONG).show()
                    return true
                }
                if (validPrefixes.none { xPub.startsWith(it) }) return false
                DeterministicKey.deserializeB58(null, xPub, params)
                true
            } catch (e: Exception) {
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
    private val accountKey = if (isValidXpub(xPub, context)) DeterministicKey.deserializeB58(null, xPub, params) else null
    private val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        // Register Litecoin parameters
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
        val receivingKey = deriveKey(accountKey!!, index)

        // Create a P2WPKH (Pay to Witness Public Key Hash) address with correct HRP for Litecoin
        val addressType = getAddressTypeFromConfig()
        val address = if (addressType == "legacy") {
            Address.fromKey(params, receivingKey, Script.ScriptType.P2PKH)
        } else {
            SegwitAddress.fromKey(params, receivingKey)
        }
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
