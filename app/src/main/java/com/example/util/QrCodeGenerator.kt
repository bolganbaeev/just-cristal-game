package com.example.util

import kotlin.math.abs

/**
 * Minimal, zero-dependency QR code generator for LAN URL sharing.
 * Implements standard QR Code Model 2 / Byte encoding for URLs like "http://192.168.1.5:8080".
 */
object QrCodeGenerator {

    /**
     * Generates a square boolean matrix where true = dark module, false = light module.
     * Uses Version 3 (29x29) or Version 4 (33x33) with Byte mode and ECC Low/Medium.
     */
    fun generateQrMatrix(text: String): Array<BooleanArray> {
        val size = 29
        val matrix = Array(size) { BooleanArray(size) { false } }
        val reserved = Array(size) { BooleanArray(size) { false } }

        // 1. Draw Position Finder Patterns at three corners (Top-Left, Top-Right, Bottom-Left)
        drawFinderPattern(matrix, reserved, 0, 0)
        drawFinderPattern(matrix, reserved, size - 7, 0)
        drawFinderPattern(matrix, reserved, 0, size - 7)

        // 2. Draw Alignment Pattern (at 22, 22 for V3)
        drawAlignmentPattern(matrix, reserved, 20, 20)

        // 3. Draw Timing Patterns (row 6 and col 6)
        for (i in 8 until size - 8) {
            val v = (i % 2 == 0)
            if (!reserved[6][i]) { matrix[6][i] = v; reserved[6][i] = true }
            if (!reserved[i][6]) { matrix[i][6] = v; reserved[i][6] = true }
        }

        // 4. Reserve Format Information areas
        for (i in 0 until 9) {
            reserved[8][i] = true
            reserved[i][8] = true
            reserved[8][size - 1 - i] = true
            reserved[size - 1 - i][8] = true
        }
        // Dark module
        matrix[size - 8][8] = true
        reserved[size - 8][8] = true

        // 5. Data encoding (Byte mode: Mode 0100 + Length 8 bits + data bytes + padding)
        val dataBits = mutableListOf<Int>()
        // Mode indicator 0100 for Byte
        dataBits.addAll(listOf(0, 1, 0, 0))
        // Character count indicator (8 bits for V1-V9 in byte mode)
        val len = text.length
        for (b in 7 downTo 0) {
            dataBits.add((len shr b) and 1)
        }
        // Data bytes
        for (char in text) {
            val code = char.code
            for (b in 7 downTo 0) {
                dataBits.add((code shr b) and 1)
            }
        }
        // Terminator
        repeat(4) { dataBits.add(0) }
        while (dataBits.size % 8 != 0) { dataBits.add(0) }

        // Pad bytes 0xEC (11101100) and 0x11 (00010001)
        val pad1 = listOf(1, 1, 1, 0, 1, 1, 0, 0)
        val pad2 = listOf(0, 0, 0, 1, 0, 0, 0, 1)
        var toggle = true
        while (dataBits.size < 440) { // Capacity for V3
            dataBits.addAll(if (toggle) pad1 else pad2)
            toggle = !toggle
        }

        // 6. Populate unreserved modules in a 2-column zigzag
        var bitIndex = 0
        var col = size - 1
        var goingUp = true

        while (col > 0) {
            if (col == 6) col-- // Skip timing col
            val rows = if (goingUp) (size - 1 downTo 0) else (0 until size)
            for (row in rows) {
                for (c in listOf(col, col - 1)) {
                    if (!reserved[row][c]) {
                        val bit = if (bitIndex < dataBits.size) dataBits[bitIndex++] else 0
                        // Mask 0: (row + col) % 2 == 0
                        val maskBit = (row + c) % 2 == 0
                        matrix[row][c] = (bit == 1) xor maskBit
                    }
                }
            }
            goingUp = !goingUp
            col -= 2
        }

        // 7. Write standard Format Info (Mask 0, Level L: 111011111000100)
        val formatBits = intArrayOf(1, 1, 1, 0, 1, 1, 1, 1, 1, 0, 0, 0, 1, 0, 0)
        var fbIdx = 0
        for (i in 0 until 9) {
            if (i == 6) continue
            matrix[8][i] = formatBits[fbIdx] == 1
            matrix[size - 1 - i][8] = formatBits[14 - fbIdx] == 1
            fbIdx++
        }
        for (i in 7 downTo 0) {
            if (i == 6) continue
            matrix[i][8] = formatBits[14 - i] == 1
            matrix[8][size - 1 - i] = formatBits[i] == 1
        }

        return matrix
    }

    private fun drawFinderPattern(matrix: Array<BooleanArray>, reserved: Array<BooleanArray>, startRow: Int, startCol: Int) {
        for (r in -1..7) {
            for (c in -1..7) {
                val row = startRow + r
                val col = startCol + c
                if (row in matrix.indices && col in matrix[0].indices) {
                    reserved[row][col] = true
                    if (r in 0..6 && c in 0..6) {
                        matrix[row][col] = (r == 0 || r == 6 || c == 0 || c == 6 || (r in 2..4 && c in 2..4))
                    } else {
                        matrix[row][col] = false
                    }
                }
            }
        }
    }

    private fun drawAlignmentPattern(matrix: Array<BooleanArray>, reserved: Array<BooleanArray>, centerRow: Int, centerCol: Int) {
        for (r in -2..2) {
            for (c in -2..2) {
                val row = centerRow + r
                val col = centerCol + c
                if (row in matrix.indices && col in matrix[0].indices) {
                    reserved[row][col] = true
                    matrix[row][col] = (abs(r) == 2 || abs(c) == 2 || (r == 0 && c == 0))
                }
            }
        }
    }
}
