package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart

class DocumentRepository(private val dao: DocumentDao) {

    val allDocuments: Flow<List<DocumentEntity>> = dao.getAllDocuments()
        .onStart {
            ensureDefaultData()
        }

    fun getDocumentById(id: Long): Flow<DocumentEntity?> = dao.getDocumentById(id)

    fun searchDocuments(query: String): Flow<List<DocumentEntity>> = dao.searchDocuments(query)

    suspend fun saveDocument(document: DocumentEntity): Long {
        return if (document.id == 0L) {
            dao.insertDocument(document.copy(updatedAt = System.currentTimeMillis()))
        } else {
            dao.updateDocument(document.copy(updatedAt = System.currentTimeMillis()))
            document.id
        }
    }

    suspend fun deleteDocument(document: DocumentEntity) {
        dao.deleteDocument(document)
    }

    private suspend fun ensureDefaultData() {
        if (dao.getDocumentCount() == 0) {
            DefaultTemplates.getInitialDocuments().forEach { doc ->
                dao.insertDocument(doc)
            }
        }
    }
}
