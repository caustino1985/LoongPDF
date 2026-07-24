package com.example.ocr

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object OcrScannerService {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun recognizeTextFromBitmap(bitmap: Bitmap): Result<String> = suspendCancellableCoroutine { continuation ->
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val formattedMarkdown = formatVisionTextToMarkdown(visionText)
                    val rawText = visionText.text
                    val resultText = if (formattedMarkdown.isNotBlank()) formattedMarkdown else rawText
                    if (continuation.isActive) {
                        continuation.resume(Result.success(resultText))
                    }
                }
                .addOnFailureListener { e ->
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(e))
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
            if (continuation.isActive) {
                continuation.resume(Result.failure(e))
            }
        }
    }

    private fun formatVisionTextToMarkdown(visionText: com.google.mlkit.vision.text.Text): String {
        val sb = StringBuilder()
        for (block in visionText.textBlocks) {
            val blockText = block.text.trim()
            if (blockText.isBlank()) continue

            val lines = blockText.lines()
            if (lines.size == 1 && lines[0].length < 35 && !lines[0].endsWith(".")) {
                sb.append("### ").append(lines[0]).append("\n\n")
            } else {
                var isBulletList = false
                for (line in lines) {
                    val trimmed = line.trim()
                    if (trimmed.startsWith("•") || trimmed.startsWith("-") || trimmed.startsWith("*")) {
                        isBulletList = true
                        val clean = trimmed.removePrefix("•").removePrefix("-").removePrefix("*").trim()
                        sb.append("- ").append(clean).append("\n")
                    } else if (trimmed.matches(Regex("^\\d+[\\.\\)]\\s*.*"))) {
                        isBulletList = true
                        sb.append(trimmed).append("\n")
                    } else {
                        if (isBulletList) {
                            sb.append("  ").append(trimmed).append("\n")
                        } else {
                            sb.append(trimmed).append(" ")
                        }
                    }
                }
                sb.append("\n\n")
            }
        }
        return sb.toString().trim()
    }
}
