package com.example.possin

import org.bitcoinj.core.Base58
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.crypto.LazyECPoint
import org.bitcoinj.params.MainNetParams
import java.nio.ByteBuffer

object XPubUtils {

//    fun deserializeDashXPUB(xPub: String, params: MainNetParams): DeterministicKey {
//        val xpubBytes = Base58.decodeChecked(xPub)
//        val bb = ByteBuffer.wrap(xpubBytes)
//        bb.int // Magic number
//        val depth = bb.get().toInt() and 0xFF
//        val parentFingerprint = bb.int
//        val childNumber = bb.int
//        val chainCode = ByteArray(32)
//        bb.get(chainCode)
//        val publicKey = ByteArray(33)
//        bb.get(publicKey)
//
//        // Create LazyECPoint from the public key bytes
//        val pubKeyPoint = LazyECPoint(params.curve, publicKey)
//
//        return DeterministicKey(
//            listOf(ChildNumber.ZERO_HARDENED), // Adjust path if necessary
//            chainCode,
//            pubKeyPoint,
//            null, // No private key available
//            parentFingerprint,
//            childNumber
//        ).setDepth(depth.toByte())
//    }
}