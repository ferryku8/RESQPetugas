package com.uxonauts.resqadmin.controllers

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.uxonauts.resqadmin.models.Artikel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class PetugasArtikelController : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage by lazy { FirebaseStorage.getInstance() }

    val artikelList = mutableStateListOf<Artikel>()
    var isLoading by mutableStateOf(false)
    var editingArticle by mutableStateOf<Artikel?>(null)

    var editJudul by mutableStateOf("")
    var editKonten by mutableStateOf("")
    var editType by mutableStateOf("article") // "article" | "banner"
    var editImageUri by mutableStateOf<Uri?>(null)
    var errorMessage by mutableStateOf<String?>(null)
    var successMessage by mutableStateOf<String?>(null)

    init { fetchArtikel() }

    fun fetchArtikel() {
        viewModelScope.launch {
            isLoading = true
            try {
                val snap = db.collection("articles")
                    .orderBy("tglPublish", Query.Direction.DESCENDING)
                    .get().await()
                artikelList.clear()
                for (doc in snap.documents) {
                    artikelList.add(
                        Artikel(
                            articleId = doc.id,
                            adminId = doc.getString("adminId") ?: "",
                            judul = doc.getString("judul") ?: "",
                            konten = doc.getString("konten") ?: "",
                            gambarUrl = doc.getString("gambarUrl") ?: "",
                            type = doc.getString("type") ?: "article",
                            tglPublish = doc.getTimestamp("tglPublish")
                        )
                    )
                }
            } catch (e: Exception) {
                errorMessage = e.localizedMessage
            } finally {
                isLoading = false
            }
        }
    }

    fun startNewArticle() {
        editingArticle = null
        editJudul = ""
        editKonten = ""
        editType = "article"
        editImageUri = null
    }

    fun startEditArticle(a: Artikel) {
        editingArticle = a
        editJudul = a.judul
        editKonten = a.konten
        editType = a.type.ifEmpty { "article" }
        editImageUri = null
    }

    fun saveArticle(onDone: () -> Unit = {}) {
        val adminId = auth.currentUser?.uid ?: return
        if (editJudul.isBlank() || editKonten.isBlank()) {
            errorMessage = "Judul dan konten wajib diisi"
            return
        }
        viewModelScope.launch {
            isLoading = true
            try {
                var imageUrl = editingArticle?.gambarUrl ?: ""
                if (editImageUri != null) {
                    val id = editingArticle?.articleId ?: UUID.randomUUID().toString()
                    val ref = storage.reference.child("articles/$id.jpg")
                    ref.putFile(editImageUri!!).await()
                    imageUrl = ref.downloadUrl.await().toString()
                }

                if (editingArticle == null) {
                    val newId = UUID.randomUUID().toString()
                    val data = mapOf(
                        "articleId" to newId,
                        "adminId" to adminId,
                        "judul" to editJudul,
                        "konten" to editKonten,
                        "gambarUrl" to imageUrl,
                        "type" to editType,
                        "tglPublish" to Timestamp.now()
                    )
                    db.collection("articles").document(newId).set(data).await()
                    successMessage = if (editType == "banner") "Banner ditambahkan"
                    else "Artikel ditambahkan"
                } else {
                    val updates = mapOf(
                        "judul" to editJudul,
                        "konten" to editKonten,
                        "gambarUrl" to imageUrl,
                        "type" to editType
                    )
                    db.collection("articles").document(editingArticle!!.articleId)
                        .update(updates).await()
                    successMessage = "Berhasil diperbarui"
                }
                fetchArtikel()
                onDone()
            } catch (e: Exception) {
                errorMessage = e.localizedMessage
            } finally {
                isLoading = false
            }
        }
    }

    fun deleteArticle(id: String) {
        viewModelScope.launch {
            try {
                db.collection("articles").document(id).delete().await()
                successMessage = "Berhasil dihapus"
                fetchArtikel()
            } catch (e: Exception) {
                errorMessage = e.localizedMessage
            }
        }
    }

    fun clearMessages() {
        errorMessage = null
        successMessage = null
    }
}