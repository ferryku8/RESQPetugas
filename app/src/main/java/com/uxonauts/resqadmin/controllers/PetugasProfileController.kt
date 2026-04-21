package com.uxonauts.resqadmin.controllers

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import com.uxonauts.resqadmin.models.Petugas
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class PetugasProfileController : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage by lazy { FirebaseStorage.getInstance() }

    var petugas by mutableStateOf(Petugas())
    var isLoading by mutableStateOf(false)
    var isUploadingPhoto by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var successMessage by mutableStateOf<String?>(null)

    var biodataExpanded by mutableStateOf(false)
    var kesehatanExpanded by mutableStateOf(false)
    var nomorExpanded by mutableStateOf(false)
    var editNama by mutableStateOf("")
    var editTelepon by mutableStateOf("")
    var editAlamat by mutableStateOf("")
    var editInstansi by mutableStateOf("")
    var editNip by mutableStateOf("")
    var laporanHariIniCount by mutableStateOf(0)
    var totalLaporanCount by mutableStateOf(0)
    var responseRatePercent by mutableStateOf(0)
    private var sosTotalCount = 0
    private var sosTodayCount = 0
    private var sosCompletedCount = 0
    private var laporanTotalCount = 0
    private var laporanTodayCount = 0
    private var laporanCompletedCount = 0

    private var sosListener: ListenerRegistration? = null
    private var laporanListener: ListenerRegistration? = null

    init {
        fetchProfile()
        startStatsListeners()
    }

    fun fetchProfile() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            isLoading = true
            try {
                val doc = db.collection("petugas").document(uid).get().await()
                doc.toObject(Petugas::class.java)?.let {
                    petugas = it.copy(petugasId = uid)
                    editNama = it.namaLengkap
                    editTelepon = it.noTelepon
                    editAlamat = it.alamat
                    editInstansi = it.instansi
                    editNip = it.nip
                }
            } catch (e: Exception) {
                errorMessage = e.localizedMessage
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * Mulai realtime listener untuk menghitung stats.
     * Stats = SOS (yang di-accept petugas ini) + Laporan non-darurat (yang di-accept petugas ini).
     *
     * Setiap kali ada perubahan di database (laporan baru, status berubah jadi Selesai, dll),
     * angka di UI akan otomatis ter-update tanpa perlu refresh.
     */
    private fun startStatsListeners() {
        val uid = auth.currentUser?.uid ?: return
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        val startOfTodayTs = Timestamp(startOfToday)
        sosListener = db.collection("sos_alerts")
            .whereEqualTo("acceptedBy", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                sosTotalCount = snapshot.size()
                sosTodayCount = snapshot.documents.count { doc ->
                    val ts = doc.getTimestamp("timestamp")
                    ts != null && ts >= startOfTodayTs
                }
                sosCompletedCount = snapshot.documents.count { doc ->
                    doc.getString("status") == "completed"
                }
                recalculateStats()
            }
        laporanListener = db.collection("reports")
            .whereEqualTo("acceptedBy", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                laporanTotalCount = snapshot.size()
                laporanTodayCount = snapshot.documents.count { doc ->
                    val ts = doc.getTimestamp("tanggalLapor")
                    ts != null && ts >= startOfTodayTs
                }
                laporanCompletedCount = snapshot.documents.count { doc ->
                    doc.getString("status") == "Selesai"
                }
                recalculateStats()
            }
    }

    /**
     * Gabungkan angka SOS + laporan, lalu update state yang dipakai UI.
     */
    private fun recalculateStats() {
        laporanHariIniCount = sosTodayCount + laporanTodayCount
        totalLaporanCount = sosTotalCount + laporanTotalCount

        val totalHandled = sosTotalCount + laporanTotalCount
        val totalCompleted = sosCompletedCount + laporanCompletedCount
        responseRatePercent = if (totalHandled == 0) 0
        else ((totalCompleted.toDouble() / totalHandled.toDouble()) * 100).toInt()
    }

    fun uploadProfilePhoto(uri: Uri) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            isUploadingPhoto = true
            try {
                val ref = storage.reference.child("petugas_profile/$uid.jpg")
                ref.putFile(uri).await()
                val url = ref.downloadUrl.await().toString()
                db.collection("petugas").document(uid).update("profileImageUrl", url).await()
                petugas = petugas.copy(profileImageUrl = url)
                successMessage = "Foto berhasil diperbarui"
            } catch (e: Exception) {
                errorMessage = e.localizedMessage
            } finally {
                isUploadingPhoto = false
            }
        }
    }

    fun saveProfile() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val updates = mapOf(
                    "namaLengkap" to editNama,
                    "noTelepon" to editTelepon,
                    "alamat" to editAlamat,
                    "instansi" to editInstansi,
                    "nip" to editNip
                )
                db.collection("petugas").document(uid).update(updates).await()
                petugas = petugas.copy(
                    namaLengkap = editNama,
                    noTelepon = editTelepon,
                    alamat = editAlamat,
                    instansi = editInstansi,
                    nip = editNip
                )
                successMessage = "Profil disimpan"
            } catch (e: Exception) {
                errorMessage = e.localizedMessage
            }
        }
    }

    fun logout(navController: NavController) {
        auth.signOut()
        sosListener?.remove()
        laporanListener?.remove()
        navController.navigate("petugas_login") { popUpTo(0) }
    }

    fun clearMessages() {
        errorMessage = null
        successMessage = null
    }

    override fun onCleared() {
        super.onCleared()
        sosListener?.remove()
        laporanListener?.remove()
    }
}