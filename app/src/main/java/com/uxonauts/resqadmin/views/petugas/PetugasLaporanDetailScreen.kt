package com.uxonauts.resqadmin.views.petugas

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale


private val PROGRESS_STAGES = listOf(
    "Diterima",
    "Sedang Diproses",
    "Diverifikasi",
    "Ditindaklanjuti",
    "Selesai"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetugasLaporanDetailScreen(
    navController: NavController,
    reportId: String
) {
    val context = LocalContext.current
    val firestore = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }
    val dateFormat = remember { SimpleDateFormat("d MMMM yyyy - HH.mm 'WIB'", Locale("id", "ID")) }
    val timelineFormat = remember { SimpleDateFormat("[dd/MM/yyyy HH:mm]", Locale("id", "ID")) }

    var loading by remember { mutableStateOf(true) }
    var namaPelapor by remember { mutableStateOf("") }
    var jenisLaporan by remember { mutableStateOf("") }
    var subJenis by remember { mutableStateOf("") }
    var judul by remember { mutableStateOf("") }
    var kronologi by remember { mutableStateOf("") }
    var lokasi by remember { mutableStateOf("") }
    var tanggalKejadian by remember { mutableStateOf("") }
    var waktuKejadian by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Menunggu") }
    var photos by remember { mutableStateOf<List<String>>(emptyList()) }
    var details by remember { mutableStateOf<Map<String, Any>>(emptyMap()) }
    var acceptedByName by remember { mutableStateOf("") }
    var petugasKontak by remember { mutableStateOf("") }
    var progressNotes by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var tanggalLapor by remember { mutableStateOf<Timestamp?>(null) }

    var selectedStatus by remember { mutableStateOf("") }
    var catatan by remember { mutableStateOf("") }
    var statusDropdownExpanded by remember { mutableStateOf(false) }

    
    LaunchedEffect(reportId) {
        firestore.collection("reports").document(reportId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    namaPelapor = snapshot.getString("namaPelapor") ?: "-"
                    jenisLaporan = snapshot.getString("jenisLaporan") ?: "-"
                    subJenis = snapshot.getString("subJenis") ?: "-"
                    judul = snapshot.getString("judul") ?: "-"
                    kronologi = snapshot.getString("kronologi") ?: "-"
                    lokasi = snapshot.getString("lokasi") ?: "-"
                    tanggalKejadian = snapshot.getString("tanggalKejadian") ?: "-"
                    waktuKejadian = snapshot.getString("waktuKejadian") ?: "-"
                    status = snapshot.getString("status") ?: "Menunggu"
                    tanggalLapor = snapshot.getTimestamp("tanggalLapor")
                    @Suppress("UNCHECKED_CAST")
                    photos = (snapshot.get("photos") as? List<String>) ?: emptyList()
                    @Suppress("UNCHECKED_CAST")
                    details = (snapshot.get("details") as? Map<String, Any>) ?: emptyMap()
                    acceptedByName = snapshot.getString("acceptedByName") ?: ""
                    @Suppress("UNCHECKED_CAST")
                    progressNotes = (snapshot.get("progressNotes") as? List<Map<String, Any>>)
                        ?: emptyList()

                    
                    if (selectedStatus.isEmpty()) {
                        selectedStatus = if (status in PROGRESS_STAGES) status
                        else "Sedang Diproses"
                    }

                    loading = false
                }
            }
    }

    
    LaunchedEffect(Unit) {
        val petugasUid = auth.currentUser?.uid ?: return@LaunchedEffect
        firestore.collection("petugas").document(petugasUid).get()
            .addOnSuccessListener { doc ->
                petugasKontak = doc.getString("noTelepon") ?: ""
            }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Progress Laporan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Kembali")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFFBFBFB)
                )
            )
        },
        containerColor = Color(0xFFFBFBFB)
    ) { padding ->
        if (loading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding), contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF0084FF))
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            
            SectionHeader("Informasi Identitas Laporan:")
            KeyValueRow("Nomor Laporan", "RESQ-${reportId.take(8).uppercase()}")
            KeyValueRow("Jenis Laporan", subJenis)
            KeyValueRow("Judul Laporan", judul)
            KeyValueRow("Tanggal Dilaporkan", formatTimestamp(tanggalLapor, dateFormat))

            Spacer(Modifier.height(20.dp))

            
            SectionHeader("Status Terkini:")
            KeyValueRow("Status", status)
            val lastUpdate = progressNotes.lastOrNull()?.get("timestamp") as? Timestamp
            KeyValueRow(
                "Update Terakhir",
                if (lastUpdate != null) formatTimestamp(lastUpdate, dateFormat)
                else formatTimestamp(tanggalLapor, dateFormat)
            )

            Spacer(Modifier.height(20.dp))

            
            SectionHeader("Ringkasan Laporan Awal:")
            KeyValueRow("Tanggal & Waktu Kejadian", "$tanggalKejadian $waktuKejadian")
            KeyValueRow("Lokasi Kejadian", lokasi)

            
            details.forEach { (key, value) ->
                val vStr = value.toString()
                if (vStr.isNotBlank() && vStr != "false" && vStr != "0") {
                    KeyValueRow(formatLabel(key), vStr)
                }
            }

            Spacer(Modifier.height(8.dp))
            KeyValueRowMultiline("Kronologi Singkat", kronologi)

            
            if (photos.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                ) {
                    Text(
                        "Bukti Singkat",
                        modifier = Modifier.width(140.dp),
                        fontSize = 13.sp,
                        color = Color.Black
                    )
                    Text(":", fontSize = 13.sp, color = Color.Black)
                    Spacer(Modifier.width(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(photos) { url ->
                            Image(
                                painter = rememberAsyncImagePainter(url),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            
            SectionHeader("Riwayat Tindak Lanjut / Timeline:")
            val tglLaporStr = formatTimestampShort(tanggalLapor, timelineFormat)
            TimelineItem(tglLaporStr, "Laporan berhasil dikirim dan diterima sistem")
            progressNotes.forEach { note ->
                val ts = note["timestamp"] as? Timestamp
                val noteText = note["note"] as? String ?: ""
                val tsStr = formatTimestampShort(ts, timelineFormat)
                TimelineItem(tsStr, noteText)
            }

            Spacer(Modifier.height(24.dp))

            
            if (acceptedByName.isNotEmpty()) {
                SectionHeader("Kontak Petugas/Unit")
                KeyValueRow("Ditangani Oleh", acceptedByName)
                if (petugasKontak.isNotEmpty()) {
                    KeyValueRow("Kontak", petugasKontak)
                }
                Spacer(Modifier.height(24.dp))
            }

            
            if (status != "Selesai") {
                SectionHeader("Update Status Laporan")

                
                Text("Ubah Status", fontSize = 13.sp, color = Color.Black)
                Spacer(Modifier.height(6.dp))
                Box {
                    OutlinedTextField(
                        value = selectedStatus,
                        onValueChange = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { statusDropdownExpanded = true },
                        readOnly = true,
                        enabled = false,
                        trailingIcon = {
                            Icon(Icons.Default.KeyboardArrowDown, null,
                                tint = Color(0xFF0084FF))
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = Color.Black,
                            disabledContainerColor = Color.White,
                            disabledBorderColor = Color(0xFFE0E0E0),
                            disabledTrailingIconColor = Color(0xFF0084FF)
                        )
                    )
                    DropdownMenu(
                        expanded = statusDropdownExpanded,
                        onDismissRequest = { statusDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        PROGRESS_STAGES.forEach { stage ->
                            DropdownMenuItem(
                                text = { Text(stage) },
                                onClick = {
                                    selectedStatus = stage
                                    statusDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text("Catatan", fontSize = 13.sp, color = Color.Black)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = catatan,
                    onValueChange = { catatan = it },
                    placeholder = { Text("Tambah Catatan atau update laporan",
                        color = Color.Gray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color(0xFF0084FF)
                    )
                )

                Spacer(Modifier.height(24.dp))

                
                val isFinishing = selectedStatus == "Selesai"
                OutlinedButton(
                    onClick = {
                        val petugasUid = auth.currentUser?.uid ?: return@OutlinedButton
                        if (catatan.isBlank() && !isFinishing) {
                            Toast.makeText(context,
                                "Harap tambahkan catatan update",
                                Toast.LENGTH_SHORT).show()
                            return@OutlinedButton
                        }

                        firestore.collection("petugas").document(petugasUid).get()
                            .addOnSuccessListener { pdoc ->
                                val pname = pdoc.getString("namaLengkap") ?: "Petugas"

                                val defaultNote = when (selectedStatus) {
                                    "Diterima" -> "Laporan diterima"
                                    "Sedang Diproses" -> "Laporan sedang diproses oleh petugas"
                                    "Diverifikasi" -> "Data laporan telah diverifikasi"
                                    "Ditindaklanjuti" -> "Laporan sedang ditindaklanjuti"
                                    "Selesai" -> "Laporan telah diselesaikan"
                                    else -> "Status diupdate ke $selectedStatus"
                                }

                                val finalNote = catatan.ifBlank { defaultNote }

                                val newEntry = hashMapOf(
                                    "note" to finalNote,
                                    "by" to pname,
                                    "stage" to selectedStatus,
                                    "timestamp" to Timestamp.now()
                                )
                                val updated = progressNotes + newEntry

                                val updates = mutableMapOf<String, Any>(
                                    "progressNotes" to updated,
                                    "status" to selectedStatus
                                )
                                
                                if (acceptedByName.isEmpty()) {
                                    updates["acceptedBy"] = petugasUid
                                    updates["acceptedByName"] = pname
                                }

                                firestore.collection("reports").document(reportId)
                                    .update(updates)
                                    .addOnSuccessListener {
                                        Toast.makeText(
                                            context,
                                            if (isFinishing) "Laporan ditandai selesai"
                                            else "Status diupdate ke $selectedStatus",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        catatan = ""
                                        if (isFinishing) navController.popBackStack()
                                    }
                            }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        2.dp, Color(0xFF0084FF)
                    )
                ) {
                    Text(
                        if (isFinishing) "Selesai" else "Update",
                        color = Color(0xFF0084FF),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            } else {
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFE8F5E9), RoundedCornerShape(14.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "✓ Laporan Selesai Ditangani",
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Black,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}

@Composable
private fun KeyValueRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            label,
            modifier = Modifier.width(140.dp),
            fontSize = 13.sp,
            color = Color.Black
        )
        Text(":", fontSize = 13.sp, color = Color.Black)
        Spacer(Modifier.width(8.dp))
        Text(
            value,
            fontSize = 13.sp,
            color = Color.Black,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun KeyValueRowMultiline(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            label,
            modifier = Modifier.width(140.dp),
            fontSize = 13.sp,
            color = Color.Black
        )
        Text(":", fontSize = 13.sp, color = Color.Black)
        Spacer(Modifier.width(8.dp))
        Text(
            value,
            fontSize = 13.sp,
            color = Color.Black,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TimelineItem(timestamp: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            timestamp,
            modifier = Modifier.width(140.dp),
            fontSize = 12.sp,
            color = Color.Black,
            fontFamily = FontFamily.Monospace
        )
        Text(":", fontSize = 12.sp)
        Spacer(Modifier.width(8.dp))
        Text(
            text,
            fontSize = 13.sp,
            color = Color.Black,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun formatTimestamp(ts: Timestamp?, fmt: SimpleDateFormat): String {
    return ts?.toDate()?.let { fmt.format(it) } ?: "-"
}

private fun formatTimestampShort(ts: Timestamp?, fmt: SimpleDateFormat): String {
    return ts?.toDate()?.let { fmt.format(it) } ?: "[-]"
}

private fun formatLabel(key: String): String {
    return key.replace(Regex("([a-z])([A-Z])"), "$1 $2")
        .replaceFirstChar { it.uppercase() }
}