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