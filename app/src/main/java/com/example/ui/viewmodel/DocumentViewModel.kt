package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.DefaultTemplates
import com.example.data.DocumentEntity
import com.example.data.DocumentRepository
import com.example.parser.MarkdownElement
import com.example.parser.MarkdownParser
import android.net.Uri
import com.example.pdf.HtmlDocumentConverter
import com.example.pdf.PdfExportConfig
import com.example.pdf.PdfPageRenderer
import com.example.pdf.WebViewPdfGenerator
import com.example.cloud.CloudImportManager
import com.example.cloud.FirestoreSyncService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class PdfEngine {
    NATIVE_VECTOR,
    WEBVIEW_HTML
}

class DocumentViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DocumentRepository(
        AppDatabase.getDatabase(application).documentDao()
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allDocuments: StateFlow<List<DocumentEntity>> = combine(
        _searchQuery.flatMapLatest { query ->
            if (query.isBlank()) {
                repository.allDocuments
            } else {
                repository.searchDocuments(query.trim())
            }
        },
        _selectedCategory
    ) { docs, category ->
        docs.filter { doc ->
            category == "All" || doc.category.equals(category, ignoreCase = true)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Current Active Document for Editing & Preview
    private val _activeDocument = MutableStateFlow<DocumentEntity?>(null)
    val activeDocument: StateFlow<DocumentEntity?> = _activeDocument

    // Live Parsed Markdown AST
    private val _parsedElements = MutableStateFlow<List<MarkdownElement>>(emptyList())
    val parsedElements: StateFlow<List<MarkdownElement>> = _parsedElements

    // PDF Export Config
    private val _pdfConfig = MutableStateFlow(PdfExportConfig())
    val pdfConfig: StateFlow<PdfExportConfig> = _pdfConfig

    // PDF Engine Choice
    private val _pdfEngine = MutableStateFlow(PdfEngine.NATIVE_VECTOR)
    val pdfEngine: StateFlow<PdfEngine> = _pdfEngine

    // Custom Export Directory URI
    private val _customExportFolderUri = MutableStateFlow<Uri?>(null)
    val customExportFolderUri: StateFlow<Uri?> = _customExportFolderUri

    // Cloud Sync & Import State
    private val _userVaultId = MutableStateFlow("my_personal_vault")
    val userVaultId: StateFlow<String> = _userVaultId

    private val _isCloudSyncing = MutableStateFlow(false)
    val isCloudSyncing: StateFlow<Boolean> = _isCloudSyncing

    private val _cloudSyncStatus = MutableStateFlow<String?>("Cloud sync ready")
    val cloudSyncStatus: StateFlow<String?> = _cloudSyncStatus

    fun setUserVaultId(id: String) {
        _userVaultId.value = id
    }

    fun backupToFirestore(context: Context, onResult: (Boolean, String) -> Unit) {
        _isCloudSyncing.value = true
        _cloudSyncStatus.value = "Backing up documents to Firestore..."

        viewModelScope.launch(Dispatchers.IO) {
            val service = FirestoreSyncService(context)
            val result = service.backupToFirestore(_userVaultId.value, allDocuments.value)

            withContext(Dispatchers.Main) {
                _isCloudSyncing.value = false
                result.fold(
                    onSuccess = { count ->
                        val msg = "Successfully backed up $count document(s) to Firestore Cloud."
                        _cloudSyncStatus.value = msg
                        onResult(true, msg)
                    },
                    onFailure = { error ->
                        val msg = "Backup failed: ${error.localizedMessage ?: "Unknown error"}"
                        _cloudSyncStatus.value = msg
                        onResult(false, msg)
                    }
                )
            }
        }
    }

    fun restoreFromFirestore(context: Context, onResult: (Boolean, String) -> Unit) {
        _isCloudSyncing.value = true
        _cloudSyncStatus.value = "Fetching backup from Firestore..."

        viewModelScope.launch(Dispatchers.IO) {
            val service = FirestoreSyncService(context)
            val result = service.restoreFromFirestore(_userVaultId.value)

            withContext(Dispatchers.Main) {
                _isCloudSyncing.value = false
                result.fold(
                    onSuccess = { docs ->
                        viewModelScope.launch(Dispatchers.IO) {
                            var restoredCount = 0
                            for (doc in docs) {
                                repository.saveDocument(doc)
                                restoredCount++
                            }
                            withContext(Dispatchers.Main) {
                                val msg = "Restored $restoredCount document(s) from Firestore Cloud!"
                                _cloudSyncStatus.value = msg
                                onResult(true, msg)
                            }
                        }
                    },
                    onFailure = { error ->
                        val msg = "Restore failed: ${error.localizedMessage ?: "Unknown error"}"
                        _cloudSyncStatus.value = msg
                        onResult(false, msg)
                    }
                )
            }
        }
    }

    fun importFromUri(context: Context, uri: Uri, onResult: (Boolean, String) -> Unit) {
        _isCloudSyncing.value = true
        _cloudSyncStatus.value = "Importing file from storage provider..."

        viewModelScope.launch(Dispatchers.IO) {
            val result = CloudImportManager.importFromUri(context, uri)

            withContext(Dispatchers.Main) {
                _isCloudSyncing.value = false
                result.fold(
                    onSuccess = { doc ->
                        viewModelScope.launch(Dispatchers.IO) {
                            val id = repository.saveDocument(doc)
                            withContext(Dispatchers.Main) {
                                val msg = "Successfully imported '${doc.title}' into library!"
                                _cloudSyncStatus.value = msg
                                selectDocument(doc.copy(id = id))
                                onResult(true, msg)
                            }
                        }
                    },
                    onFailure = { error ->
                        val msg = "Import failed: ${error.localizedMessage ?: "Invalid file"}"
                        _cloudSyncStatus.value = msg
                        onResult(false, msg)
                    }
                )
            }
        }
    }

    fun importFromCloudUrl(url: String, onResult: (Boolean, String) -> Unit) {
        _isCloudSyncing.value = true
        _cloudSyncStatus.value = "Downloading document from Cloud link..."

        viewModelScope.launch(Dispatchers.IO) {
            val result = CloudImportManager.importFromCloudUrl(url)

            withContext(Dispatchers.Main) {
                _isCloudSyncing.value = false
                result.fold(
                    onSuccess = { doc ->
                        viewModelScope.launch(Dispatchers.IO) {
                            val id = repository.saveDocument(doc)
                            withContext(Dispatchers.Main) {
                                val msg = "Successfully imported '${doc.title}' from Cloud link!"
                                _cloudSyncStatus.value = msg
                                selectDocument(doc.copy(id = id))
                                onResult(true, msg)
                            }
                        }
                    },
                    onFailure = { error ->
                        val msg = "Cloud link import failed: ${error.localizedMessage ?: "Unable to fetch content"}"
                        _cloudSyncStatus.value = msg
                        onResult(false, msg)
                    }
                )
            }
        }
    }

    // PDF Export State
    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting

    private val _lastExportedFile = MutableStateFlow<File?>(null)
    val lastExportedFile: StateFlow<File?> = _lastExportedFile

    // Multi-select state
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode

    private val _selectedDocumentIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedDocumentIds: StateFlow<Set<Long>> = _selectedDocumentIds

    fun toggleSelectionMode(enable: Boolean? = null) {
        val next = enable ?: !_isSelectionMode.value
        _isSelectionMode.value = next
        if (!next) {
            _selectedDocumentIds.value = emptySet()
        }
    }

    fun toggleDocumentSelection(docId: Long) {
        val current = _selectedDocumentIds.value.toMutableSet()
        if (current.contains(docId)) {
            current.remove(docId)
        } else {
            current.add(docId)
        }
        _selectedDocumentIds.value = current
        if (!_isSelectionMode.value && current.isNotEmpty()) {
            _isSelectionMode.value = true
        }
    }

    fun selectAllDocuments(documents: List<DocumentEntity>) {
        _selectedDocumentIds.value = documents.map { it.id }.toSet()
        _isSelectionMode.value = true
    }

    fun clearSelection() {
        _selectedDocumentIds.value = emptySet()
    }

    fun deleteSelectedDocuments() {
        val ids = _selectedDocumentIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteDocumentsByIds(ids)
            withContext(Dispatchers.Main) {
                _selectedDocumentIds.value = emptySet()
                _isSelectionMode.value = false
            }
        }
    }

    fun exportSelectedMergedPdf(
        context: Context,
        selectedDocs: List<DocumentEntity>,
        onComplete: (File?) -> Unit
    ) {
        if (selectedDocs.isEmpty()) return
        _isExporting.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val items = selectedDocs.map { doc ->
                    val elements = MarkdownParser.parse(doc.content)
                    Pair(doc.title, elements)
                }

                val bundleTitle = "Merged_PDF_Bundle_${selectedDocs.size}_Docs"
                val config = _pdfConfig.value

                val file = if (_pdfEngine.value == PdfEngine.WEBVIEW_HTML) {
                    val outputDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "documents")
                    if (!outputDir.exists()) outputDir.mkdirs()
                    val tempFile = File(outputDir, "${bundleTitle.lowercase()}_${System.currentTimeMillis()}.pdf")

                    val html = HtmlDocumentConverter.convertToMergedHtml(bundleTitle, items, config)
                    val ok = WebViewPdfGenerator.generatePdfFromHtml(context, html, config, tempFile)
                    if (ok) tempFile else PdfPageRenderer.generateMergedPdf(context, bundleTitle, items, config)
                } else {
                    PdfPageRenderer.generateMergedPdf(context, bundleTitle, items, config)
                }

                withContext(Dispatchers.Main) {
                    _isExporting.value = false
                    _lastExportedFile.value = file
                    onComplete(file)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _isExporting.value = false
                    onComplete(null)
                }
            }
        }
    }

    fun exportSelectedMergedPdfToUri(
        context: Context,
        selectedDocs: List<DocumentEntity>,
        destinationUri: Uri,
        onComplete: (Boolean) -> Unit
    ) {
        if (selectedDocs.isEmpty()) return
        _isExporting.value = true

        viewModelScope.launch(Dispatchers.IO) {
            var success = false
            try {
                val items = selectedDocs.map { doc ->
                    val elements = MarkdownParser.parse(doc.content)
                    Pair(doc.title, elements)
                }

                val bundleTitle = "Merged_PDF_Bundle"
                val config = _pdfConfig.value

                val tempFile = if (_pdfEngine.value == PdfEngine.WEBVIEW_HTML) {
                    val html = HtmlDocumentConverter.convertToMergedHtml(bundleTitle, items, config)
                    val cacheFile = File(context.cacheDir, "merged_export_temp.pdf")
                    val ok = WebViewPdfGenerator.generatePdfFromHtml(context, html, config, cacheFile)
                    if (ok) cacheFile else PdfPageRenderer.generateMergedPdf(context, bundleTitle, items, config)
                } else {
                    PdfPageRenderer.generateMergedPdf(context, bundleTitle, items, config)
                }

                context.contentResolver.openOutputStream(destinationUri)?.use { outStream ->
                    tempFile.inputStream().use { inStream ->
                        inStream.copyTo(outStream)
                    }
                    success = true
                }

                _lastExportedFile.value = tempFile
            } catch (e: Exception) {
                e.printStackTrace()
                success = false
            }

            withContext(Dispatchers.Main) {
                _isExporting.value = false
                onComplete(success)
            }
        }
    }

    fun setPdfEngine(engine: PdfEngine) {
        _pdfEngine.value = engine
    }

    fun setCustomExportFolderUri(uri: Uri?) {
        _customExportFolderUri.value = uri
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun selectDocument(doc: DocumentEntity) {
        _activeDocument.value = doc
        _pdfConfig.value = PdfExportConfig(
            paperSize = doc.paperSize,
            primaryColorHex = doc.primaryColorHex,
            author = doc.author
        )
        reparseContent(doc.content)
    }

    fun createNewDocument(
        title: String = "Untitled Document",
        category: String = "General",
        templateContent: String = DefaultTemplates.SYSTEM_ARCHITECTURE_TEMPLATE
    ) {
        val newDoc = DocumentEntity(
            title = title,
            category = category,
            content = templateContent
        )
        viewModelScope.launch {
            val id = repository.saveDocument(newDoc)
            selectDocument(newDoc.copy(id = id))
        }
    }

    fun updateActiveContent(newContent: String) {
        val current = _activeDocument.value ?: return
        val updated = current.copy(content = newContent)
        _activeDocument.value = updated
        reparseContent(newContent)

        // Debounced or direct auto-save
        viewModelScope.launch {
            repository.saveDocument(updated)
        }
    }

    fun updateActiveTitle(newTitle: String) {
        val current = _activeDocument.value ?: return
        val updated = current.copy(title = newTitle)
        _activeDocument.value = updated

        viewModelScope.launch {
            repository.saveDocument(updated)
        }
    }

    fun updatePdfConfig(newConfig: PdfExportConfig) {
        _pdfConfig.value = newConfig
        val current = _activeDocument.value ?: return
        val updated = current.copy(
            paperSize = newConfig.paperSize,
            primaryColorHex = newConfig.primaryColorHex,
            author = newConfig.author
        )
        _activeDocument.value = updated
        viewModelScope.launch {
            repository.saveDocument(updated)
        }
    }

    fun deleteDocument(doc: DocumentEntity) {
        viewModelScope.launch {
            repository.deleteDocument(doc)
            if (_activeDocument.value?.id == doc.id) {
                _activeDocument.value = null
            }
        }
    }

    private fun reparseContent(markdown: String) {
        viewModelScope.launch(Dispatchers.Default) {
            val ast = MarkdownParser.parse(markdown)
            _parsedElements.value = ast
        }
    }

    fun exportToPdf(context: Context, onComplete: (File) -> Unit) {
        val doc = _activeDocument.value ?: return
        _isExporting.value = true

        viewModelScope.launch(Dispatchers.IO) {
            val file = if (_pdfEngine.value == PdfEngine.WEBVIEW_HTML) {
                val outputDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "documents")
                if (!outputDir.exists()) outputDir.mkdirs()
                val sanitizedTitle = doc.title.replace(Regex("[^a-zA-Z0-9_]"), "_").lowercase()
                val tempFile = File(outputDir, "${sanitizedTitle}_${System.currentTimeMillis()}.pdf")

                val html = HtmlDocumentConverter.convertToHtml(doc.title, _parsedElements.value, _pdfConfig.value)
                val success = WebViewPdfGenerator.generatePdfFromHtml(context, html, _pdfConfig.value, tempFile)
                if (success) tempFile else PdfPageRenderer.generatePdf(context, doc.title, _parsedElements.value, _pdfConfig.value)
            } else {
                PdfPageRenderer.generatePdf(
                    context = context,
                    title = doc.title,
                    elements = _parsedElements.value,
                    config = _pdfConfig.value
                )
            }

            // Save PDF path back to Room
            val updatedDoc = doc.copy(pdfExportPath = file.absolutePath)
            repository.saveDocument(updatedDoc)
            _activeDocument.value = updatedDoc

            withContext(Dispatchers.Main) {
                _isExporting.value = false
                _lastExportedFile.value = file
                onComplete(file)
            }
        }
    }

    fun exportToCustomUri(context: Context, destinationUri: Uri, onComplete: (Boolean) -> Unit) {
        val doc = _activeDocument.value ?: return
        _isExporting.value = true

        viewModelScope.launch(Dispatchers.IO) {
            var success = false
            try {
                val tempFile = if (_pdfEngine.value == PdfEngine.WEBVIEW_HTML) {
                    val html = HtmlDocumentConverter.convertToHtml(doc.title, _parsedElements.value, _pdfConfig.value)
                    val cacheFile = File(context.cacheDir, "export_temp.pdf")
                    val ok = WebViewPdfGenerator.generatePdfFromHtml(context, html, _pdfConfig.value, cacheFile)
                    if (ok) cacheFile else PdfPageRenderer.generatePdf(context, doc.title, _parsedElements.value, _pdfConfig.value)
                } else {
                    PdfPageRenderer.generatePdf(
                        context = context,
                        title = doc.title,
                        elements = _parsedElements.value,
                        config = _pdfConfig.value
                    )
                }

                context.contentResolver.openOutputStream(destinationUri)?.use { outStream ->
                    tempFile.inputStream().use { inStream ->
                        inStream.copyTo(outStream)
                    }
                    success = true
                }

                // Update room entity path
                val updatedDoc = doc.copy(pdfExportPath = destinationUri.toString())
                repository.saveDocument(updatedDoc)
                _activeDocument.value = updatedDoc
                _lastExportedFile.value = tempFile
            } catch (e: Exception) {
                e.printStackTrace()
                success = false
            }

            withContext(Dispatchers.Main) {
                _isExporting.value = false
                onComplete(success)
            }
        }
    }
}
