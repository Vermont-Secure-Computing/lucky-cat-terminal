package com.vermont.possin

import org.bitcoinj.core.NetworkParameters
import java.util.logging.Logger

object CashAddress {

    private val logger = Logger.getLogger(CashAddress::class.java.name)
    private const val CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l"
    private val GENERATOR = longArrayOf(
        0x98f2bc8e61L,
        0x79b76d99e2L,
        0xf33e5fb3c4L,
        0xae2eabe2a8L,
        0x1e4f43e470L
    )

    private fun polymod(values: ByteArray): Long {
        var chk = 1L
        for (value in values) {
            val top = (chk shr 35).toInt()
            chk = ((chk and 0x07ffffffffL) shl 5) xor (value.toLong() and 0xff)
            for (i in GENERATOR.indices) {
                if ((top shr i) and 1 != 0) {
                    chk = chk xor GENERATOR[i]
                }
            }
        }
        logger.info("Polymod checksum: $chk")
        return chk xor 1L
    }

    private fun prefixExpand(prefix: String): ByteArray {
        val buf = ByteArray(prefix.length + 1)
        for (i in prefix.indices) {
            buf[i] = (prefix[i].code and 0x1f).toByte()
        }
        buf[prefix.length] = 0
        logger.info("Expanded prefix: ${buf.joinToString(",")}")
        return buf
    }

    private fun createChecksum(prefix: String, payload: ByteArray): ByteArray {
        val expandedPrefix = prefixExpand(prefix)
        val values = ByteArray(expandedPrefix.size + payload.size + 8)
        System.arraycopy(expandedPrefix, 0, values, 0, expandedPrefix.size)
        System.arraycopy(payload, 0, values, expandedPrefix.size, payload.size)
        logger.info("Polymod values: ${values.joinToString(",")}")
        val mod = polymod(values)
        val ret = ByteArray(8)
        for (i in 0..7) {
            ret[i] = ((mod shr (5 * (7 - i))) and 31).toByte()
        }
        logger.info("Checksum: ${ret.joinToString(",")}")
        return ret
    }

    private fun convertBits(data: ByteArray, fromBits: Int, toBits: Int, pad: Boolean): ByteArray {
        var acc = 0
        var bits = 0
        val maxv = (1 shl toBits) - 1
        val result = mutableListOf<Byte>()
        for (value in data) {
            acc = (acc shl fromBits) or (value.toInt() and 0xff)
            bits += fromBits
            while (bits >= toBits) {
                bits -= toBits
                result.add(((acc shr bits) and maxv).toByte())
            }
        }
        if (pad && bits > 0) {
            result.add(((acc shl (toBits - bits)) and maxv).toByte())
        } else if (!pad && (bits >= fromBits || ((acc shl (toBits - bits)) and maxv) != 0)) {
            logger.severe("Could not convert bits, invalid data: acc = $acc, bits = $bits, maxv = $maxv")
            throw IllegalArgumentException("Could not convert bits, invalid data")
        }
        logger.info("Converted bits: ${result.joinToString(",")}")
        return result.toByteArray()
    }

    private fun decodeBase32(encoded: String): ByteArray {
        val data = ByteArray(encoded.length)
        for (i in encoded.indices) {
            val index = CHARSET.indexOf(encoded[i])
            if (index == -1) {
                throw IllegalArgumentException("Invalid Base32 character: ${encoded[i]}")
            }
            data[i] = index.toByte()
        }
        logger.info("Decoded Base32: ${data.joinToString(",")}")
        return data
    }

    fun encode(prefix: String, payload: ByteArray): String {
        val checksum = createChecksum(prefix, payload)
        val combined = ByteArray(payload.size + checksum.size)
        System.arraycopy(payload, 0, combined, 0, payload.size)
        System.arraycopy(checksum, 0, combined, payload.size, checksum.size)

        val sb = StringBuilder(prefix.length + 1 + combined.size)
        sb.append(prefix)
        sb.append(':')

        for (b in combined) {
            sb.append(CHARSET[b.toInt() and 0xff])
        }

        val encodedAddress = sb.toString()
        logger.info("Encoded address: $encodedAddress")
        return encodedAddress
    }

    fun toCashAddress(legacyAddress: String, params: NetworkParameters): String {
        val decoded = Base58BCH.decodeChecked(legacyAddress)
        val version = decoded[0].toInt() and 0xff

        val addressHeader = params.addressHeader
        val p2shHeader = params.p2SHHeader

        val payload = ByteArray(21)
        payload[0] = when (version) {
            addressHeader -> 0.toByte()
            p2shHeader -> 1.toByte()
            else -> throw IllegalArgumentException("Invalid address version")
        }
        System.arraycopy(decoded, 1, payload, 1, 20)
        val payload5bit = convertBits(payload, 8, 5, true)
        return encode("bitcoincash", payload5bit)
    }

    fun isValidAddress(address: String, params: NetworkParameters): Boolean {
        return try {
            val prefix = "bitcoincash"
            val (prefixPart, dataPart) = address.split(":")
            if (prefixPart != prefix) {
                logger.severe("Prefix does not match: $prefixPart != $prefix")
                return false
            }

            val decoded = decodeBase32(dataPart)
            logger.info("Decoded Base32: ${decoded.joinToString(",")}")

            // Separate the payload from the checksum
            val payload = decoded.copyOf(decoded.size - 8)
            val checksum = decoded.copyOfRange(decoded.size - 8, decoded.size)
            logger.info("Payload: ${payload.joinToString(",")}")
            logger.info("Checksum: ${checksum.joinToString(",")}")

            // Validate the checksum
            val calculatedChecksum = createChecksum(prefix, payload)
            logger.info("Calculated Checksum: ${calculatedChecksum.joinToString(",")}")
            if (!calculatedChecksum.contentEquals(checksum)) {
                logger.severe("Checksum does not match")
                return false
            }

            // Convert 5-bit payload to 8-bit
            val payload8bit = convertBits(payload, 5, 8, false)
            logger.info("Payload 8-bit: ${payload8bit.joinToString(",")}")

            // Reconstruct the address
            val reconstructed = encode(prefix, payload)
            logger.info("Original: $address, Reconstructed: $reconstructed")

            val isValid = reconstructed == address && payload8bit.size == 21
            if (!isValid) {
                logger.severe("Address validation failed: $reconstructed != $address or payload size != 21")
            }
            isValid
        } catch (e: Exception) {
            logger.severe("Error validating CashAddr: ${e.message}")
            false
        }
    }
}

//fun main() {
//    val legacyAddress = "1mK1JkwZhYFdn9z842kqkB2hY6oq6yw6G"
//    val params = BitcoinCashMainNetParams.get()
//    val cashAddress = CashAddress.toCashAddress(legacyAddress, params)
//    println("Cash Address: $cashAddress")
//
//    // Test isValidAddress
//    val isValidLegacy = BitcoinCashManager.isValidAddress("1mK1JkwZhYFdn9z842kqkB2hY6oq6yw6G")
//    val isValidCashAddr = CashAddress.isValidAddress("bitcoincash:qqyxz4gkr8umhd7tqkqxvvgpgezhe9xveyuvyp09cl", params)
//    println("Is valid legacy address: $isValidLegacy")
//    println("Is valid CashAddr: $isValidCashAddr")
//}
