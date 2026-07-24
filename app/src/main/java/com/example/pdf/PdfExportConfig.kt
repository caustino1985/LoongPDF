package com.example.pdf

import androidx.compose.ui.graphics.Color

data class PdfExportConfig(
    val paperSize: String = "A4",
    val marginPt: Float = 40f,
    val baseFontSize: Float = 11f,
    val primaryColorHex: String = "#0F172A",
    val showPageNumbers: Boolean = true,
    val headerText: String = "",
    val footerText: String = "LoongPDF Processor v2.0",
    val author: String = "LoongPDF Author"
) {
    fun getDimensions(): Pair<Int, Int> {
        return when (paperSize) {
            "A4" -> Pair(595, 842)
            "Letter" -> Pair(612, 792)
            "A3" -> Pair(842, 1191)
            "A5" -> Pair(420, 595)
            else -> Pair(595, 842)
        }
    }

    fun parsePrimaryColor(): Int {
        return try {
            android.graphics.Color.parseColor(primaryColorHex)
        } catch (e: Exception) {
            android.graphics.Color.parseColor("#0F172A")
        }
    }
}
