package com.example.possin

import org.bitcoinj.core.Address
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.crypto.HDKeyDerivation
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.script.Script

class BitcoinManager(private val xPub: String) {

    // Initialize network parameters for MainNet
    private val params: NetworkParameters = MainNetParams.get()
    // Create a DeterministicKey from the xPub
    private val accountKey = DeterministicKey.deserializeB58(null, xPub, params)

    fun getAddress(index: Int): String {
        // Derive the key at the specified index
        val receivingKey = HDKeyDerivation.deriveChildKey(accountKey, index)
        // Convert the key to a Bitcoin address
        val address = Address.fromKey(params, receivingKey, Script.ScriptType.P2PKH)
        return address.toString()
    }
}