package com.vermont.possin

import java.util.Arrays

object Base58BCH {
    private const val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
    private val INDEXES = IntArray(128)
    init {
        Arrays.fill(INDEXES, -1)
        for (i in ALPHABET.indices) {
            INDEXES[ALPHABET[i].toInt()] = i
        }
    }

    fun decode(input: String): ByteArray {
        if (input.isEmpty()) {
            return ByteArray(0)
        }

        // Convert the base58-encoded ASCII chars to a base58 byte sequence (base58 digits).
        val input58 = ByteArray(input.length)
        for (i in input.indices) {
            val c = input[i]
            val digit = if (c.toInt() < 128) INDEXES[c.toInt()] else -1
            if (digit < 0) {
                throw IllegalArgumentException("Invalid character $c in Base58 string")
            }
            input58[i] = digit.toByte()
        }

        // Count leading zeros.
        var zeros = 0
        while (zeros < input58.size && input58[zeros].toInt() == 0) {
            ++zeros
        }

        // Convert base-58 digits to base-256 digits.
        val decoded = ByteArray(input.length)
        var outputStart = decoded.size
        for (inputStart in zeros until input58.size) {
            var carry = input58[inputStart].toInt()
            var i = decoded.size - 1
            while (carry != 0 || i >= outputStart) {
                carry += 58 * (decoded[i].toInt() and 0xFF)
                decoded[i] = (carry % 256).toByte()
                carry /= 256
                --i
            }
            outputStart = i + 1
        }

        // Skip leading zeros in decoded.
        var outputIndex = decoded.size
        while (outputIndex > outputStart && decoded[outputIndex - 1].toInt() == 0) {
            --outputIndex
        }

        // Return decoded bytes.
        return decoded.copyOfRange(outputStart - zeros, outputIndex)
    }

    fun decodeChecked(input: String): ByteArray {
        val decoded = decode(input)
        if (decoded.size < 4) {
            throw IllegalArgumentException("Input too short for checksum")
        }
        val data = decoded.copyOfRange(0, decoded.size - 4)
        val checksum = decoded.copyOfRange(decoded.size - 4, decoded.size)
        val actualChecksum = Arrays.copyOfRange(doubleSha256(data), 0, 4)
        if (!Arrays.equals(checksum, actualChecksum)) {
            throw IllegalArgumentException("Invalid checksum")
        }
        return data
    }

    private fun doubleSha256(data: ByteArray): ByteArray {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        return digest.digest(digest.digest(data))
    }
}
