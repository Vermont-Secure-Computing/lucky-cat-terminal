package com.vermont.possin.utils

import java.util.Arrays
import org.bitcoinj.core.Base58
import org.bitcoinj.core.Sha256Hash

class TronAddressValidator {

    fun validate(address: String): Boolean {
        return try {
            val decoded = Base58.decode(address)
            if (decoded.size != 25) return false

            val checksum = Arrays.copyOfRange(decoded, 21, 25)
            val hash = Sha256Hash.hashTwice(decoded, 0, 21)
            val calculatedChecksum = Arrays.copyOfRange(hash, 0, 4)

            Arrays.equals(checksum, calculatedChecksum)
        } catch (e: Exception) {
            false
        }
    }
}