package com.example.cloud

import android.content.Context
import com.example.data.DocumentEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.Date

class FirestoreSyncService(private val context: Context) {

    private fun getFirestoreInstance(): FirebaseFirestore? {
        return try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun backupToFirestore(
        userVaultId: String,
        documents: List<DocumentEntity>
    ): Result<Int> {
        val firestore = getFirestoreInstance()
            ?: return Result.failure(IllegalStateException("Firebase is not configured or initialized in this environment."))

        val cleanVaultId = userVaultId.trim().ifEmpty { "default_user_vault" }
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")

        return try {
            val collectionRef = firestore.collection("user_vaults")
                .document(cleanVaultId)
                .collection("documents")

            var count = 0
            for (doc in documents) {
                val docData = hashMapOf(
                    "id" to doc.id,
                    "title" to doc.title,
                    "author" to doc.author,
                    "content" to doc.content,
                    "category" to doc.category,
                    "paperSize" to doc.paperSize,
                    "primaryColorHex" to doc.primaryColorHex,
                    "updatedAt" to doc.updatedAt,
                    "isFavorite" to doc.isFavorite,
                    "syncedAt" to System.currentTimeMillis()
                )

                // Use title + id as document key
                val docKey = "doc_${doc.id}"
                collectionRef.document(docKey).set(docData, SetOptions.merge()).await()
                count++
            }
            Result.success(count)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun restoreFromFirestore(
        userVaultId: String
    ): Result<List<DocumentEntity>> {
        val firestore = getFirestoreInstance()
            ?: return Result.failure(IllegalStateException("Firebase is not configured or initialized in this environment."))

        val cleanVaultId = userVaultId.trim().ifEmpty { "default_user_vault" }
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")

        return try {
            val querySnapshot = firestore.collection("user_vaults")
                .document(cleanVaultId)
                .collection("documents")
                .get()
                .await()

            val docs = mutableListOf<DocumentEntity>()
            for (snapshot in querySnapshot.documents) {
                val title = snapshot.getString("title") ?: "Untitled Cloud Doc"
                val author = snapshot.getString("author") ?: "LoongPDF User"
                val content = snapshot.getString("content") ?: ""
                val category = snapshot.getString("category") ?: "General"
                val paperSize = snapshot.getString("paperSize") ?: "A4"
                val primaryColorHex = snapshot.getString("primaryColorHex") ?: "#0F172A"
                val updatedAt = snapshot.getLong("updatedAt") ?: System.currentTimeMillis()
                val isFavorite = snapshot.getBoolean("isFavorite") ?: false

                docs.add(
                    DocumentEntity(
                        id = 0, // Auto-generate local Room ID on insert
                        title = title,
                        author = author,
                        content = content,
                        category = category,
                        paperSize = paperSize,
                        primaryColorHex = primaryColorHex,
                        updatedAt = updatedAt,
                        isFavorite = isFavorite
                    )
                )
            }
            Result.success(docs)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
