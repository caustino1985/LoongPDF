package com.example.pdf

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object WebViewPdfGenerator {

    suspend fun generatePdfFromHtml(
        context: Context,
        htmlContent: String,
        config: PdfExportConfig,
        outputFile: File
    ): Boolean = suspendCoroutine { continuation ->
        try {
            val handler = Handler(Looper.getMainLooper())
            val webView = WebView(context)
            webView.settings.javaScriptEnabled = true
            webView.settings.domStorageEnabled = true

            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    handler.postDelayed({
                        try {
                            val (width, height) = config.getDimensions()

                            val pdfDocument = PdfDocument()
                            val pageInfo = PdfDocument.PageInfo.Builder(width, height, 1).create()
                            val page = pdfDocument.startPage(pageInfo)

                            webView.draw(page.canvas)
                            pdfDocument.finishPage(page)

                            FileOutputStream(outputFile).use { out ->
                                pdfDocument.writeTo(out)
                            }
                            pdfDocument.close()
                            continuation.resume(true)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            continuation.resume(false)
                        }
                    }, 800)
                }
            }

            webView.loadDataWithBaseURL("https://cdn.jsdelivr.net", htmlContent, "text/html", "UTF-8", null)
        } catch (e: Exception) {
            e.printStackTrace()
            continuation.resume(false)
        }
    }
}

