package com.example.possin

import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.params.AbstractBitcoinNetParams

class WoodcoinMainNetParams : AbstractBitcoinNetParams() {
    init {
        id = ID_MAINNET
        packetMagic = 0xf1c8d2fdL
        addressHeader = 73 // 'W' prefix for Woodcoin addresses
        p2shHeader = 75
        dumpedPrivateKeyHeader = 201
        segwitAddressHrp = "wd" // Modify if different
    }

    override fun getPaymentProtocolId(): String {
        return "woodcoin-main"
    }

    override fun getProtocolVersionNum(version: NetworkParameters.ProtocolVersion): Int {
        return 70015 // Same as Bitcoin's protocol version
    }

    companion object {
        const val ID_MAINNET = "org.woodcoin.production"
    }
}