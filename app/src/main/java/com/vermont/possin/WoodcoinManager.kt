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

import android.content.Context
import android.util.Log
import org.bitcoinj.core.Address
import org.bitcoinj.core.Base58
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.Utils
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.crypto.HDKeyDerivation
import org.bitcoinj.params.AbstractBitcoinNetParams
import org.bitcoinj.script.Script
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security
import java.util.Arrays
import com.google.common.collect.ImmutableList
import org.bitcoinj.crypto.LazyECPoint

class WoodcoinManager(private val context: Context, private val xPub: String) {

    companion object {
        private const val PREFS_NAME = "WoodcoinManagerPrefs"
        private const val LAST_INDEX_KEY = "lastIndex"
        private const val XPUB_EXPECTED_LENGTH = 78
    }

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    private val params: AbstractBitcoinNetParams = WoodcoinMainNetParams()
    private val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getAddress(): String {
        val lastIndex = getLastIndex()
        val newIndex = if (lastIndex == -1) 0 else lastIndex + 1
        saveLastIndex(newIndex)
        return deriveAddress(newIndex)
    }

    private fun deriveAddress(index: Int): String {
        Log.d("WDC", "Woodcoin address index $index")
        val receivingKey = derivePublicKey(index)

        // Generate Woodcoin address from public key
        val ecKey = ECKey.fromPublicOnly(receivingKey.pubKey)
        val address = Address.fromKey(params, ecKey, Script.ScriptType.P2PKH)

        Log.d("ADDRESS", address.toString())
        return address.toString()
    }

    private fun derivePublicKey(index: Int): DeterministicKey {
        val parentKey = deserializeXPub(xPub)
        // Use the m/0/n derivation path
        return HDKeyDerivation.deriveChildKey(parentKey, ChildNumber(0, false))
    }

    private fun deserializeXPub(xPub: String): DeterministicKey {
        Log.d("XPUB", "Provided xPub: $xPub")
        val xpubBytes = Base58.decodeChecked(xPub)  // Use decodeChecked to verify the integrity of the key
        Log.d("XPUB", "Decoded xPub bytes: ${Arrays.toString(xpubBytes)}")
        Log.d("XPUB", "Decoded xPub bytes length: ${xpubBytes.size}")
        if (xpubBytes.size != XPUB_EXPECTED_LENGTH) {
            throw IllegalArgumentException("Invalid xPub key length: ${xpubBytes.size}, expected: $XPUB_EXPECTED_LENGTH")
        }

        val depth = xpubBytes[4].toInt() and 0xff
        val parentFingerprint = Utils.readUint32BE(xpubBytes, 5).toInt()
        val chainCode = Arrays.copyOfRange(xpubBytes, 13, 45)
        val publicKey = Arrays.copyOfRange(xpubBytes, 45, 78)

        val pubKeyPoint = LazyECPoint(ECKey.CURVE.curve, publicKey)

        return DeterministicKey(
            ImmutableList.of(ChildNumber(0, false)),
            chainCode,
            pubKeyPoint,
            null,
            depth,
            parentFingerprint
        )
    }

    private fun getLastIndex(): Int {
        if (!sharedPreferences.contains(LAST_INDEX_KEY)) {
            return -1
        }
        return sharedPreferences.getInt(LAST_INDEX_KEY, -1)
    }

    private fun saveLastIndex(index: Int) {
        with(sharedPreferences.edit()) {
            putInt(LAST_INDEX_KEY, index)
            apply()
        }
    }
}