package com.example.possin

import org.bitcoinj.core.Address
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.crypto.HDKeyDerivation
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.script.Script

class LitecoinMainNetParams : MainNetParams() {
    init {
        id = "org.litecoin.production"
        packetMagic = 0xfbc0b6dbL
        addressHeader = 48
        p2shHeader = 50
        dumpedPrivateKeyHeader = 176
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
}

class LitecoinManager(private val xPub: String) {
    private val params: NetworkParameters = LitecoinMainNetParams()
    private val accountKey = DeterministicKey.deserializeB58(null, xPub, params)

    fun getAddress(index: Int): String {
        val childKey = HDKeyDerivation.deriveChildKey(accountKey, index)
        val address = Address.fromKey(params, childKey, Script.ScriptType.P2PKH)
        return address.toString()
    }
}