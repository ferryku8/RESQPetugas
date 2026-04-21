package com.uxonauts.resqadmin.controllers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PetugasAuthController : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    var loginEmail by mutableStateOf("")
    var loginPassword by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    fun doLogin(navController: NavController) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                auth.signInWithEmailAndPassword(loginEmail, loginPassword).await()
                val uid = auth.currentUser?.uid ?: throw Exception("UID null")
                val doc = db.collection("petugas").document(uid).get().await()
                if (!doc.exists()) {
                    auth.signOut()
                    errorMessage = "Akun ini bukan akun petugas"
                    isLoading = false
                    return@launch
                }

                navController.navigate("petugas_home") { popUpTo(0) }
            } catch (e: Exception) {
                errorMessage = "Login gagal: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }
}