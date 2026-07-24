package com.example.cloud

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.DocumentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

object CloudImportManager {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun importFromUri(context: Context, uri: Uri): Result<DocumentEntity> = withContext(Dispatchers.IO) {
        try {
            var fileName = "Imported_Document.md"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    fileName = cursor.getString(nameIndex) ?: fileName
                }
            }

            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(IllegalArgumentException("Unable to open input stream for selected file."))

            val contentBuilder = StringBuilder()
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    contentBuilder.append(line).append("\n")
                    line = reader.readLine()
                }
            }

            val rawContent = contentBuilder.toString()
            val title = fileName.replace(Regex("\\.(md|txt|markdown|doc|docx)$", RegexOption.IGNORE_CASE), "")
            val category = when {
                title.contains("report", ignoreCase = true) -> "Report"
                title.contains("resume", ignoreCase = true) || title.contains("cv", ignoreCase = true) -> "Resume"
                title.contains("tech", ignoreCase = true) || title.contains("spec", ignoreCase = true) -> "Technical"
                else -> "General"
            }

            val doc = DocumentEntity(
                title = title,
                author = "Cloud Import",
                content = rawContent.ifBlank { "# $title\n\nEmpty imported document." },
                category = category,
                updatedAt = System.currentTimeMillis()
            )

            Result.success(doc)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun importFromCloudUrl(url: String): Result<DocumentEntity> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = url.trim()
            if (cleanUrl.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("URL cannot be empty."))
            }

            val (downloadUrl, sourceProvider) = resolveDownloadUrl(cleanUrl)

            val request = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile) LoongPDF/2.0")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(IllegalStateException("HTTP ${response.code}: Failed to download document from $sourceProvider."))
            }

            val bodyText = response.body?.string() ?: ""
            if (bodyText.isBlank()) {
                return@withContext Result.failure(IllegalStateException("Downloaded document content is empty."))
            }

            val extractedTitle = extractTitleFromContent(bodyText, sourceProvider)

            val doc = DocumentEntity(
                title = extractedTitle,
                author = "$sourceProvider Cloud",
                content = bodyText,
                category = "General",
                updatedAt = System.currentTimeMillis()
            )

            Result.success(doc)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun resolveDownloadUrl(url: String): Pair<String, String> {
        return when {
            // Google Docs URL
            url.contains("docs.google.com/document/d/") -> {
                val docId = url.substringAfter("/d/").substringBefore("/")
                Pair("https://docs.google.com/document/d/$docId/export?format=txt", "Google Docs")
            }
            // Google Drive File URL
            url.contains("drive.google.com/file/d/") -> {
                val fileId = url.substringAfter("/d/").substringBefore("/")
                Pair("https://drive.google.com/uc?export=download&id=$fileId", "Google Drive")
            }
            url.contains("drive.google.com") && url.contains("id=") -> {
                val fileId = url.substringAfter("id=").substringBefore("&")
                Pair("https://drive.google.com/uc?export=download&id=$fileId", "Google Drive")
            }
            // OneDrive URL
            url.contains("onedrive.live.com") || url.contains("1drv.ms") || url.contains("sharepoint.com") -> {
                val directUrl = if (url.contains("?")) "$url&download=1" else "$url?download=1"
                Pair(directUrl, "OneDrive")
            }
            else -> {
                Pair(url, "Cloud Link")
            }
        }
    }

    private fun extractTitleFromContent(content: String, provider: String): String {
        val firstHeader = content.lines().firstOrNull { it.startsWith("# ") }
        if (firstHeader != null) {
            return firstHeader.removePrefix("# ").trim().take(40)
        }
        val firstLine = content.lines().firstOrNull { it.isNotBlank() }
        if (firstLine != null && firstLine.length in 3..40) {
            return firstLine.trim()
        }
        return "Imported from $provider"
    }
}
