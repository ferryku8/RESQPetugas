package com.uxonauts.resqadmin.models

import com.google.firebase.Timestamp

data class Petugas(
    val petugasId: String = "",
    val namaLengkap: String = "",
    val email: String = "",
    val noTelepon: String = "",
    val jenisKelamin: String = "",
    val alamat: String = "",
    val profileImageUrl: String = "",
    val role: Int = 2, // 1=Admin, 2=Polisi, 3=Ambulans/Medis, 4=Damkar
    val instansi: String = "",
    val nip: String = "",
    val laporanHariIni: Int = 0,
    val totalLaporan: Int = 0,
    val responseRate: Int = 0
) {
    fun roleName(): String = when (role) {
        1 -> "Admin"
        2 -> "Polisi"
        3 -> "Medis/Ambulans"
        4 -> "Damkar"
        else -> "Petugas"
    }
}

data class Laporan(
    val reportId: String = "",
    val userId: String = "",
    val namaPelapor: String = "",
    val jenisLaporan: String = "", // SOS / Non-Darurat
    val subJenis: String = "",
    val judul: String = "",
    val kronologi: String = "",
    val lokasi: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val status: String = "Menunggu", // Menunggu / Diterima / Diproses / Selesai / Ditolak
    val tanggalLapor: Timestamp? = null
)

data class Artikel(
    val articleId: String = "",
    val adminId: String = "",
    val judul: String = "",
    val konten: String = "",
    val gambarUrl: String = "",
    val type: String = "article", // "article" atau "banner"
    val tglPublish: Timestamp? = null
)

data class SosAlert(
    val alertId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhone: String = "",
    val category: String = "",
    val address: String = "",
    val location: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val status: String = "active",
    val timestamp: com.google.firebase.Timestamp? = null,
    val targetRoles: List<Int> = emptyList(),
    val medicalInfo: Map<String, String> = emptyMap(),
    val acceptedBy: String = "",
    val acceptedByName: String = "",
    val acceptedByRole: Int = 0,
    val petugasLat: Double = 0.0,
    val petugasLng: Double = 0.0,
    val respondingPetugas: Map<String, Map<String, Any>> = emptyMap()  // BARU
)