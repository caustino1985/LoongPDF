package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val author: String = "LoongPDF User",
    val content: String,
    val category: String = "General",
    val paperSize: String = "A4",
    val primaryColorHex: String = "#0F172A",
    val updatedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val pdfExportPath: String? = null
)
