package com.uxonauts.resqadmin.views.petugas

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.uxonauts.resqadmin.controllers.PetugasHomeController
import com.uxonauts.resqadmin.models.Laporan
import com.uxonauts.resqadmin.models.SosAlert
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetugasHomeScreen(
    navController: NavController,
    controller: PetugasHomeController = viewModel()
) {
    val context = LocalContext.current
    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var petugasName by remember { mutableStateOf("") }

    var selectedTab by remember { mutableStateOf("SOS") }
    var searchQuery by remember { mutableStateOf("") }
    var showNotificationSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            try {
                val doc = FirebaseFirestore.getInstance()
                    .collection("petugas").document(uid).get().await()
                petugasName = doc.getString("namaLengkap") ?: "Petugas"
            } catch (_: Exception) {
            }
        }
    }

    
    val sosNotif = controller.newAlertNotification
    if (sosNotif != null) {
        AlertDialog(
            onDismissRequest = { controller.dismissNotification() },
            icon = {
                Icon(
                    Icons.Default.Warning, null,
                    tint = Color(0xFFF44336),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    "🚨 SOS BARU!",
                    fontWeight = FontWeight.Bold, color = Color(0xFFF44336)
                )
            },
            text = {
                Column {
                    Text("Kategori: ${sosNotif.category}", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("Pelapor: ${sosNotif.userName}")
                    Spacer(Modifier.height(4.dp))
                    Text("Lokasi: ${sosNotif.address}", fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { controller.dismissNotification() }) {
                    Text("Tutup", color = Color(0xFF0084FF), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Scaffold(
        bottomBar = {
            PetugasBottomBar(
                selectedTab = "home",
                onHomeClick = {},
                onArtikelClick = { navController.navigate("petugas_artikel") },
                onProfileClick = { navController.navigate("petugas_profile") }
            )
        },
        containerColor = Color(0xFFFBFBFB)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari laporan...", color = Color.Gray) },
                    leadingIcon = {
                        Icon(Icons.Outlined.Search, null, tint = Color(0xFF0084FF))
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color(0xFF0084FF),
                        unfocusedBorderColor = Color(0xFF0084FF),
                    ),
                    singleLine = true
                )
                Spacer(Modifier.width(12.dp))

                
                val unreadCount = controller.laporanList.count { it.status == "Menunggu" }
                Box {
                    Icon(
                        Icons.Outlined.Notifications, "Notifikasi",
                        tint = Color(0xFF0084FF),
                        modifier = Modifier
                            .size(32.dp)
                            .clickable { showNotificationSheet = true }
                    )
                    if (unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-4).dp)
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF44336)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (unreadCount > 9) "9+" else "$unreadCount",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFEFF6FF))
                    .padding(4.dp)
            ) {
                TabButton(
                    label = "SOS Darurat",
                    count = controller.sosAlerts.count { it.status == "active" },
                    selected = selectedTab == "SOS",
                    onClick = {
                        selectedTab = "SOS"
                        controller.selectedFilter = "Semua"
                    },
                    modifier = Modifier.weight(1f)
                )
                TabButton(
                    label = "Laporan",
                    count = controller.laporanList.count { it.status == "Menunggu" },
                    selected = selectedTab == "Laporan",
                    onClick = {
                        selectedTab = "Laporan"
                        controller.selectedFilter = "Semua"
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(16.dp))

            
            val filters = if (selectedTab == "SOS") {
                listOf("Semua", "Active", "Accepted", "Completed")
            } else {
                listOf("Semua", "Menunggu", "Diterima", "Selesai")
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEach { filter ->
                    val isSelected = controller.selectedFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) Color(0xFF0084FF)
                                else Color(0xFFB3D9FF).copy(alpha = 0.5f)
                            )
                            .clickable { controller.selectedFilter = filter }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(
                            filter,
                            color = if (isSelected) Color.White else Color.Black,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            
            if (selectedTab == "SOS") {
                val alerts = controller.filteredAlerts().filter {
                    if (searchQuery.isBlank()) true
                    else it.category.contains(searchQuery, ignoreCase = true) ||
                            it.userName.contains(searchQuery, ignoreCase = true) ||
                            it.address.contains(searchQuery, ignoreCase = true)
                }
                if (alerts.isEmpty()) {
                    EmptyState(
                        if (searchQuery.isBlank()) "Belum ada laporan SOS"
                        else "Tidak ditemukan laporan SOS untuk \"$searchQuery\""
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(alerts) { alert ->
                            SosAlertCard(
                                alert = alert,
                                onTerima = {
                                    try {
                                        val hasPerm = ContextCompat.checkSelfPermission(
                                            context, Manifest.permission.ACCESS_FINE_LOCATION
                                        ) == PackageManager.PERMISSION_GRANTED

                                        if (hasPerm) {
                                            val cts = CancellationTokenSource()
                                            fusedClient.getCurrentLocation(
                                                Priority.PRIORITY_HIGH_ACCURACY, cts.token
                                            ).addOnSuccessListener { loc ->
                                                if (loc != null) {
                                                    controller.terimaLaporan(
                                                        alert, loc.latitude, loc.longitude, petugasName
                                                    )
                                                    navController.navigate("petugas_tracking/${alert.alertId}")
                                                }
                                            }
                                        }
                                    } catch (_: SecurityException) {
                                    }
                                }
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            } else {
                val laporan = controller.filteredLaporan().filter {
                    if (searchQuery.isBlank()) true
                    else it.judul.contains(searchQuery, ignoreCase = true) ||
                            it.subJenis.contains(searchQuery, ignoreCase = true) ||
                            it.namaPelapor.contains(searchQuery, ignoreCase = true)
                }
                if (laporan.isEmpty()) {
                    EmptyState(
                        if (searchQuery.isBlank()) "Belum ada laporan masuk"
                        else "Tidak ditemukan laporan untuk \"$searchQuery\""
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(laporan) { lap ->
                            LaporanCard(
                                laporan = lap,
                                onClick = {
                                    navController.navigate("petugas_laporan_detail/${lap.reportId}")
                                }
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }

    
    if (showNotificationSheet) {
        ModalBottomSheet(
            onDismissRequest = { showNotificationSheet = false },
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .heightIn(max = 500.dp)
            ) {
                Text(
                    "Notifikasi",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Laporan non-darurat yang menunggu",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(Modifier.height(16.dp))

                val notifs = controller.laporanList.filter { it.status == "Menunggu" }

                if (notifs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.Notifications,
                                null,
                                tint = Color.LightGray,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("Tidak ada notifikasi baru", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(notifs) { lap ->
                            NotificationItem(lap) {
                                showNotificationSheet = false
                                navController.navigate("petugas_laporan_detail/${lap.reportId}")
                            }
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationItem(laporan: Laporan, onClick: () -> Unit) {
    val dateText = laporan.tanggalLapor?.toDate()?.let {
        SimpleDateFormat("d MMM, HH:mm", Locale("id", "ID")).format(it)
    } ?: "-"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF8F9FA))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFC107)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Description, null, tint = Color.White,
                modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                laporan.subJenis.ifEmpty { laporan.jenisLaporan },
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                laporan.judul,
                fontSize = 11.sp,
                color = Color.DarkGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "Oleh: ${laporan.namaPelapor} • $dateText",
                fontSize = 10.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun TabButton(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Color(0xFF0084FF) else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = if (selected) Color.White else Color(0xFF0084FF),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        if (count > 0) {
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (selected) Color.White else Color(0xFFF44336))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    "$count",
                    color = if (selected) Color(0xFF0084FF) else Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Description,
                null,
                tint = Color.LightGray,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(message, color = Color.Gray, fontSize = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
fun SosAlertCard(alert: SosAlert, onTerima: () -> Unit) {
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val alreadyResponded = alert.respondingPetugas.containsKey(currentUid)
    val isSos = alert.status == "active"
    val dateText = alert.timestamp?.toDate()?.let {
        SimpleDateFormat("d MMM, HH:mm", Locale("id", "ID")).format(it)
    } ?: "-"

    val statusLabel = when (alert.status) {
        "active" -> "Menunggu"
        "accepted" -> "Diterima"
        "on_the_way" -> "Diproses"
        "arrived" -> "Diproses"
        "completed" -> "Selesai"
        else -> alert.status
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isSos) Color(0xFFF44336) else Color(0xFFFFC107)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PriorityHigh, null, tint = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    alert.category,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFB3D9FF).copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(statusLabel, fontSize = 12.sp, color = Color.Black)
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(alert.userName, fontSize = 14.sp, color = Color.DarkGray)

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocationOn, null,
                    tint = Color.Gray, modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(alert.address.ifEmpty { "-" }, fontSize = 13.sp, color = Color.DarkGray)
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AccessTime, null,
                    tint = Color.Gray, modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(dateText, fontSize = 13.sp, color = Color.DarkGray)
            }

            if (!alreadyResponded && alert.status != "completed") {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onTerima,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0084FF)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Terima Laporan", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun LaporanCard(
    laporan: Laporan,
    onClick: () -> Unit
) {
    val dateText = laporan.tanggalLapor?.toDate()?.let {
        SimpleDateFormat("d MMM, HH:mm", Locale("id", "ID")).format(it)
    } ?: "-"

    val statusColor = when (laporan.status) {
        "Menunggu" -> Color(0xFFFFC107)
        "Diterima", "Diverifikasi", "Ditindaklanjuti", "Diproses", "Sedang Diproses" -> Color(0xFF2196F3)
        "Selesai" -> Color(0xFF4CAF50)
        else -> Color.Gray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(statusColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Description, null, tint = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        laporan.subJenis.ifEmpty { laporan.jenisLaporan },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        laporan.jenisLaporan,
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        laporan.status,
                        fontSize = 12.sp,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                laporan.judul,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Oleh: ${laporan.namaPelapor}",
                fontSize = 12.sp,
                color = Color.Gray
            )

            if (laporan.kronologi.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    laporan.kronologi,
                    fontSize = 13.sp,
                    color = Color.DarkGray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocationOn, null,
                    tint = Color.Gray, modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(laporan.lokasi.ifEmpty { "-" }, fontSize = 12.sp, color = Color.DarkGray)
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AccessTime, null,
                    tint = Color.Gray, modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(dateText, fontSize = 12.sp, color = Color.DarkGray)
            }
        }
    }
}
