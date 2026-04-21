package com.uxonauts.resqadmin.controllers

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.uxonauts.resqadmin.models.Laporan
import com.uxonauts.resqadmin.models.SosAlert
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.*

class PetugasHomeController : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    val sosAlerts = mutableStateListOf<SosAlert>()
    val laporanList = mutableStateListOf<Laporan>()

    var isLoading by mutableStateOf(false)
    var selectedFilter by mutableStateOf("Semua")
    var errorMessage by mutableStateOf<String?>(null)
    var petugasRole by mutableStateOf(0)
    var newAlertNotification by mutableStateOf<SosAlert?>(null)
    var newLaporanNotification by mutableStateOf<Laporan?>(null)

    private var sosListener: ListenerRegistration? = null
    private var laporanListener: ListenerRegistration? = null
    private var previousAlertIds = setOf<String>()
    private var previousLaporanIds = setOf<String>()
    private var isFirstSosSnapshot = true
    private var isFirstLaporanSnapshot = true

    init {
        fetchPetugasRole()
    }

    private fun fetchPetugasRole() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val doc = db.collection("petugas").document(uid).get().await()
                petugasRole = (doc.getLong("role") ?: 0L).toInt()
                startListening()
                startListeningReports()
            } catch (e: Exception) {
                errorMessage = "Gagal memuat role: ${e.localizedMessage}"
            }
        }
    }
    fun startListening() {
        sosListener?.remove()

        sosListener = db.collection("sos_alerts")
            .whereArrayContains("targetRoles", petugasRole)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("PetugasHome", "SOS Listen error", error)
                    errorMessage = "Listener error: ${error.message}"
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val newList = snapshot.documents.map { doc ->

                        @Suppress("UNCHECKED_CAST")
                        val respondingMap = doc.get("respondingPetugas") as? Map<String, Map<String, Any>> ?: emptyMap()

                        SosAlert(
                            alertId = doc.id,
                            userId = doc.getString("userId") ?: "",
                            userName = doc.getString("userName") ?: "Pengguna",
                            userPhone = doc.getString("userPhone") ?: "",
                            category = doc.getString("category") ?: "",
                            latitude = doc.getDouble("latitude") ?: 0.0,
                            longitude = doc.getDouble("longitude") ?: 0.0,
                            address = doc.getString("address") ?: "",
                            status = doc.getString("status") ?: "active",
                            targetRoles = (doc.get("targetRoles") as? List<Long>)?.map { it.toInt() } ?: emptyList(),
                            acceptedBy = doc.getString("acceptedBy") ?: "",
                            acceptedByName = doc.getString("acceptedByName") ?: "",
                            acceptedByRole = (doc.getLong("acceptedByRole") ?: 0L).toInt(),
                            petugasLat = doc.getDouble("petugasLat") ?: 0.0,
                            petugasLng = doc.getDouble("petugasLng") ?: 0.0,
                            medicalInfo = (doc.get("medicalInfo") as? Map<String, String>) ?: emptyMap(),
                            timestamp = doc.getTimestamp("timestamp"),
                            respondingPetugas = respondingMap // Added mapping here
                        )
                    }

                    val currentIds = newList.map { it.alertId }.toSet()
                    if (isFirstSosSnapshot) {
                        previousAlertIds = currentIds
                        isFirstSosSnapshot = false
                    } else {
                        val freshlyAdded = currentIds - previousAlertIds
                        if (freshlyAdded.isNotEmpty()) {
                            newList.firstOrNull { it.alertId in freshlyAdded && it.status == "active" }?.let {
                                newAlertNotification = it
                            }
                        }
                        previousAlertIds = currentIds
                    }

                    sosAlerts.clear()
                    sosAlerts.addAll(newList)
                }
            }
    }
    fun startListeningReports() {
        laporanListener?.remove()

        laporanListener = db.collection("reports")
            .whereArrayContains("targetRoles", petugasRole)
            .orderBy("tanggalLapor", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("PetugasHome", "Laporan Listen error", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val newList = snapshot.documents.map { doc ->
                        Laporan(
                            reportId = doc.id,
                            userId = doc.getString("userId") ?: "",
                            namaPelapor = doc.getString("namaPelapor") ?: "Pengguna",
                            jenisLaporan = doc.getString("jenisLaporan") ?: "",
                            subJenis = doc.getString("subJenis") ?: "",
                            judul = doc.getString("judul") ?: "-",
                            kronologi = doc.getString("kronologi") ?: "",
                            lokasi = doc.getString("lokasi") ?: "",
                            latitude = doc.getDouble("latitude") ?: 0.0,
                            longitude = doc.getDouble("longitude") ?: 0.0,
                            status = doc.getString("status") ?: "Menunggu",
                            tanggalLapor = doc.getTimestamp("tanggalLapor")
                        )
                    }

                    val currentIds = newList.map { it.reportId }.toSet()

                    if (isFirstLaporanSnapshot) {
                        previousLaporanIds = currentIds
                        isFirstLaporanSnapshot = false
                    } else {
                        val freshlyAdded = currentIds - previousLaporanIds
                        if (freshlyAdded.isNotEmpty()) {
                            newList.firstOrNull { it.reportId in freshlyAdded && it.status == "Menunggu" }?.let {
                                newLaporanNotification = it
                            }
                        }
                        previousLaporanIds = currentIds
                    }

                    laporanList.clear()
                    laporanList.addAll(newList)
                }
            }
    }
    fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
    fun terimaLaporan(alert: SosAlert, petugasLat: Double, petugasLng: Double, petugasName: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val petugasDoc = db.collection("petugas").document(uid).get().await()
                val role = (petugasDoc.getLong("role") ?: 2L).toInt()

                db.collection("sos_alerts").document(alert.alertId)
                    .update(
                        mapOf(
                            "status" to "accepted",
                            "respondingPetugas.$uid" to mapOf(
                                "name" to petugasName,
                                "role" to role,
                                "lat" to petugasLat,
                                "lng" to petugasLng,
                                "status" to "on_the_way"  // status individual
                            )
                        )
                    ).await()
            } catch (e: Exception) {
                errorMessage = e.localizedMessage
            }
        }
    }

    fun updatePetugasLocation(alertId: String, lat: Double, lng: Double) {
        viewModelScope.launch {
            try {
                db.collection("sos_alerts").document(alertId).update(
                    mapOf("petugasLat" to lat, "petugasLng" to lng)
                ).await()
            } catch (e: Exception) {
                Log.e("PetugasHome", "Update location failed", e)
            }
        }
    }
    fun terimaLaporanNonDarurat(reportId: String, petugasName: String) {
        viewModelScope.launch {
            try {
                val updates = mapOf(
                    "status" to "Diterima",
                    "acceptedBy" to (auth.currentUser?.uid ?: ""),
                    "acceptedByName" to petugasName
                )
                db.collection("reports").document(reportId).update(updates).await()
            } catch (e: Exception) {
                errorMessage = "Gagal terima laporan: ${e.localizedMessage}"
            }
        }
    }

    fun updateLaporanStatus(reportId: String, newStatus: String) {
        viewModelScope.launch {
            try {
                db.collection("reports").document(reportId)
                    .update("status", newStatus).await()
            } catch (e: Exception) {
                errorMessage = "Gagal update status: ${e.localizedMessage}"
            }
        }
    }
    fun dismissNotification() {
        newAlertNotification = null
    }

    fun dismissLaporanNotification() {
        newLaporanNotification = null
    }
    fun filteredAlerts(): List<SosAlert> {
        return when (selectedFilter) {
            "Active" -> sosAlerts.filter { it.status == "active" }
            "Accepted" -> sosAlerts.filter { it.status == "accepted" || it.status == "on_the_way" }
            "Completed" -> sosAlerts.filter { it.status == "completed" }
            else -> sosAlerts
        }
    }

    fun filteredLaporan(): List<Laporan> {
        return when (selectedFilter) {
            "Menunggu" -> laporanList.filter { it.status == "Menunggu" }
            "Diterima" -> laporanList.filter { it.status == "Diterima" || it.status == "Diproses" }
            "Selesai" -> laporanList.filter { it.status == "Selesai" }
            else -> laporanList
        }
    }

    override fun onCleared() {
        super.onCleared()
        sosListener?.remove()
        laporanListener?.remove()
    }
}