package com.example.possin

import android.util.Log

object Base58 {
    private const val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
    private val INDEXES = IntArray(128)

    init {
        for (i in INDEXES.indices) {
            INDEXES[i] = -1
        }
        for (i in ALPHABET.indices) {
            INDEXES[ALPHABET[i].code] = i
        }
    }

    fun encodeChecked(input: ByteArray): String {
        val checksum = doubleSha256(input).copyOfRange(0, 4)
        val inputWithChecksum = input + checksum
        return encode(inputWithChecksum)
    }

    fun doubleSha256(input: ByteArray): ByteArray {
        return org.web3j.crypto.Hash.sha256(org.web3j.crypto.Hash.sha256(input))
    }

    fun encode(input: ByteArray): String {
        if (input.isEmpty()) return ""

        var inputCopy = input.copyOf(input.size)
        var zeros = inputCopy.takeWhile { it.toInt() == 0 }.count()
        val encoded = CharArray(inputCopy.size * 2)

        var j = encoded.size
        var startAt = zeros
        while (startAt < inputCopy.size) {
            val mod = divmod58(inputCopy, startAt)
            if (inputCopy[startAt].toInt() == 0) {
                ++startAt
            }
            encoded[--j] = ALPHABET[mod]
        }

        while (j < encoded.size && encoded[j] == ALPHABET[0]) {
            ++j
        }

        while (--zeros >= 0) {
            encoded[--j] = ALPHABET[0]
        }

        return String(encoded, j, encoded.size - j)
    }

    fun decode(input: String): ByteArray {
        if (input.isEmpty()) return ByteArray(0)

        Log.e("Base58", "Decoding input: $input")
        var zeros = input.takeWhile { it == ALPHABET[0] }.count()
        val decoded = ByteArray(input.length)
        var j = decoded.size

        var startAt = zeros
        while (startAt < input.length) {
            val mod = try {
                divmod256(decoded, startAt, input)
            } catch (e: Exception) {
                Log.e("Base58", "Error in divmod256: ${e.message}")
                throw e
            }
            if (input[startAt] == ALPHABET[0]) {
                ++startAt
            }
            decoded[--j] = mod.toByte()
        }

        while (j < decoded.size && decoded[j].toInt() == 0) {
            ++j
        }

        Log.e("Base58", "Decoded byte array: ${decoded.joinToString(", ") { it.toString() }}")
        return decoded.copyOfRange(j - zeros, decoded.size)
    }

    fun decodeChecked(input: String): ByteArray {
        val decoded = decode(input)
        if (decoded.size < 4) {
            Log.e("Base58", "Input too short for checksum")
            throw IllegalArgumentException("Input too short for checksum")
        }

        val data = decoded.copyOfRange(0, decoded.size - 4)
        val checksum = decoded.copyOfRange(decoded.size - 4, decoded.size)
        val expectedChecksum = doubleSha256(data).copyOfRange(0, 4)

        Log.e("Base58", "Checksum: ${checksum.joinToString(", ") { it.toString() }}, Expected checksum: ${expectedChecksum.joinToString(", ") { it.toString() }}")
        if (!checksum.contentEquals(expectedChecksum)) {
            Log.e("Base58", "Invalid checksum")
            throw IllegalArgumentException("Invalid checksum")
        }

        Log.e("Base58", "Checksum valid")
        return data
    }

    private fun divmod58(number: ByteArray, startAt: Int): Int {
        var remainder = 0
        for (i in startAt until number.size) {
            val digit256 = number[i].toInt() and 0xFF
            val temp = remainder * 256 + digit256
            number[i] = (temp / 58).toByte()
            remainder = temp % 58
        }
        return remainder
    }

    private fun divmod256(number: ByteArray, startAt: Int, input: String): Int {
        var remainder = 0
        for (i in startAt until input.length) {
            val digit58 = INDEXES[input[i].code]
            if (digit58 == -1) {
                Log.e("Base58", "Invalid Base58 character: ${input[i]}")
                throw IllegalArgumentException("Invalid Base58 character: ${input[i]}")
            }
            val temp = remainder * 58 + digit58
            number[i - startAt] = (temp / 256).toByte()
            remainder = temp % 256
        }
        return remainder
    }
}
