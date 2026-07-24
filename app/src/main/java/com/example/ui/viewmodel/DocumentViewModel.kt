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
import com.example.pdf.PdfExportConfig
import com.example.pdf.PdfPageRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class DocumentViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DocumentRepository(
        AppDatabase.getDatabase(application).documentDao()
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory

    val allDocuments: StateFlow<List<DocumentEntity>> = combine(
        repository.allDocuments,
        _searchQuery,
        _selectedCategory
    ) { docs, query, category ->
        docs.filter { doc ->
            val matchesQuery = query.isEmpty() ||
                    doc.title.contains(query, ignoreCase = true) ||
                    doc.content.contains(query, ignoreCase = true)
            val matchesCategory = category == "All" || doc.category.equals(category, ignoreCase = true)
            matchesQuery && matchesCategory
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

    // PDF Export State
    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting

    private val _lastExportedFile = MutableStateFlow<File?>(null)
    val lastExportedFile: StateFlow<File?> = _lastExportedFile

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
            val file = PdfPageRenderer.generatePdf(
                context = context,
                title = doc.title,
                elements = _parsedElements.value,
                config = _pdfConfig.value
            )

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
}
