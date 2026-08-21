package com.spidey.js.angad.util

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

object PacketParser {

    data class DnsQuestion(val domain: String, val type: Int)

    fun parseDnsDomain(data: ByteArray, offset: Int, length: Int): DnsQuestion? {
        if (length < 12) return null
        
        val buffer = ByteBuffer.wrap(data, offset, length)
        val qdCount = buffer.getShort(4).toInt()
        if (qdCount <= 0) return null

        val domain = StringBuilder()
        var pos = 12
        
        try {
            while (pos < length) {
                val labelLen = data[offset + pos].toInt() and 0xFF
                if (labelLen == 0) {
                    pos++
                    break
                }
                if (domain.isNotEmpty()) domain.append(".")
                if (pos + 1 + labelLen > length) return null
                domain.append(String(data, offset + pos + 1, labelLen, StandardCharsets.US_ASCII))
                pos += 1 + labelLen
            }
            if (pos + 2 > length) return null
            val type = ((data[offset + pos].toInt() and 0xFF) shl 8) or (data[offset + pos + 1].toInt() and 0xFF)
            return DnsQuestion(domain.toString(), type)
        } catch (e: Exception) {
            return null
        }
    }

    fun parseSni(data: ByteArray, offset: Int, length: Int): String? {
        try {
            val buffer = ByteBuffer.wrap(data, offset, length)
            if (buffer.get().toInt() != 22) return null
            buffer.position(buffer.position() + 4)
            if (buffer.get().toInt() != 1) return null
            buffer.position(buffer.position() + 3)
            buffer.position(buffer.position() + 34)
            val sessionIdLen = buffer.get().toInt() and 0xFF
            buffer.position(buffer.position() + sessionIdLen)
            val cipherSuitesLen = buffer.getShort().toInt() and 0xFFFF
            buffer.position(buffer.position() + cipherSuitesLen)
            val compressionLen = buffer.get().toInt() and 0xFF
            buffer.position(buffer.position() + compressionLen)
            
            if (buffer.position() >= length) return null
            val extensionsLen = buffer.getShort().toInt() and 0xFFFF
            val extensionsEnd = buffer.position() + extensionsLen
            
            while (buffer.position() < extensionsEnd && buffer.position() < length - 4) {
                val extType = buffer.getShort().toInt() and 0xFFFF
                val extLen = buffer.getShort().toInt() and 0xFFFF
                if (extType == 0) {
                    buffer.position(buffer.position() + 2)
                    if (buffer.get().toInt() == 0) {
                        val nameLen = buffer.getShort().toInt() and 0xFFFF
                        val nameBytes = ByteArray(nameLen)
                        buffer.get(nameBytes)
                        return String(nameBytes, StandardCharsets.US_ASCII)
                    }
                } else {
                    buffer.position(buffer.position() + extLen)
                }
            }
        } catch (e: Exception) {}
        return null
    }

    fun createNxDomainResponse(queryData: ByteArray, offset: Int, length: Int): ByteArray {
        val response = queryData.copyOfRange(offset, offset + length)
        if (response.size < 12) return response
        response[2] = 0x81.toByte() // QR=1, RD=1
        response[3] = 0x83.toByte() // RA=1, RCODE=3 (NXDOMAIN)
        response[6] = 0x00.toByte() // Answer count = 0
        response[7] = 0x00.toByte()
        response[8] = 0x00.toByte() // Authority count = 0
        response[9] = 0x00.toByte()
        response[10] = 0x00.toByte() // Additional count = 0
        response[11] = 0x00.toByte()
        return response
    }

    fun createSinkholeResponse(queryData: ByteArray, offset: Int, length: Int): ByteArray {
        val base = queryData.copyOfRange(offset, offset + length)
        if (base.size < 12) return base
        val response = ByteArray(base.size + 16)
        System.arraycopy(base, 0, response, 0, base.size)
        response[2] = 0x81.toByte()
        response[3] = 0x80.toByte() // NOERROR
        response[6] = 0x00.toByte()
        response[7] = 0x01.toByte() // 1 Answer (0.0.0.0)
        
        var idx = base.size
        response[idx++] = 0xC0.toByte()
        response[idx++] = 0x0C.toByte() // Name Pointer to 0x0C
        response[idx++] = 0x00.toByte()
        response[idx++] = 0x01.toByte() // TYPE A
        response[idx++] = 0x00.toByte()
        response[idx++] = 0x01.toByte() // CLASS IN
        response[idx++] = 0x00.toByte()
        response[idx++] = 0x00.toByte()
        response[idx++] = 0x00.toByte()
        response[idx++] = 0x00.toByte() // TTL 0s (prevents browser DNS caching)
        response[idx++] = 0x00.toByte()
        response[idx++] = 0x04.toByte() // RDLENGTH 4
        response[idx++] = 0x00.toByte() // 0.0.0.0 (Sinkhole)
        response[idx++] = 0x00.toByte()
        response[idx++] = 0x00.toByte()
        response[idx++] = 0x00.toByte()
        return response
    }

    /**
     * Parses the first resolved IPv4 (Type A) address from a DNS response packet.
     */
    fun parseDnsResponseIp(data: ByteArray, offset: Int, length: Int): String? {
        if (length < 12) return null
        try {
            val buffer = ByteBuffer.wrap(data, offset, length)
            val flags = buffer.getShort(2).toInt() and 0xFFFF
            // Must be a DNS response (QR bit = 1)
            if ((flags and 0x8000) == 0) return null
            val qdCount = buffer.getShort(4).toInt() and 0xFFFF
            val anCount = buffer.getShort(6).toInt() and 0xFFFF
            if (anCount <= 0) return null

            // Skip Question Section
            var pos = 12
            for (q in 0 until qdCount) {
                while (pos < length) {
                    val len = data[offset + pos].toInt() and 0xFF
                    if (len == 0) { pos++; break }
                    if ((len and 0xC0) == 0xC0) { pos += 2; break }
                    pos += 1 + len
                }
                pos += 4 // Type (2) + Class (2)
            }

            // Parse Answer Section
            for (a in 0 until anCount) {
                if (pos >= length) break
                val firstByte = data[offset + pos].toInt() and 0xFF
                if ((firstByte and 0xC0) == 0xC0) {
                    pos += 2
                } else {
                    while (pos < length) {
                        val len = data[offset + pos].toInt() and 0xFF
                        if (len == 0) { pos++; break }
                        if ((len and 0xC0) == 0xC0) { pos += 2; break }
                        pos += 1 + len
                    }
                }
                if (pos + 10 > length) break
                val type = ((data[offset + pos].toInt() and 0xFF) shl 8) or (data[offset + pos + 1].toInt() and 0xFF)
                val rdLength = ((data[offset + pos + 8].toInt() and 0xFF) shl 8) or (data[offset + pos + 9].toInt() and 0xFF)
                pos += 10

                if (type == 1 && rdLength == 4 && pos + 4 <= length) { // Type A (IPv4)
                    val ip0 = data[offset + pos].toInt() and 0xFF
                    val ip1 = data[offset + pos + 1].toInt() and 0xFF
                    val ip2 = data[offset + pos + 2].toInt() and 0xFF
                    val ip3 = data[offset + pos + 3].toInt() and 0xFF
                    return "$ip0.$ip1.$ip2.$ip3"
                }
                pos += rdLength
            }
        } catch (e: Exception) {}
        return null
    }
}
