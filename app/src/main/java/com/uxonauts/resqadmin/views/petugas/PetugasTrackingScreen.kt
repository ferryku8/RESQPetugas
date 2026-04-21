package com.uxonauts.resqadmin.views.petugas

import android.Manifest
import android.content.pm.PackageManager
import android.preference.PreferenceManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

data class PetugasOnMap(
    val uid: String,
    val name: String,
    val role: Int,
    val lat: Double,
    val lng: Double,
    val status: String  
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetugasTrackingScreen(
    navController: NavController,
    alertId: String
) {
    val context = LocalContext.current
    val firestore = remember { FirebaseFirestore.getInstance() }
    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val scope = rememberCoroutineScope()
    val myUid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }

    var globalStatus by remember { mutableStateOf("active") }
    var category by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("Memuat...") }
    var userPhone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var userLat by remember { mutableStateOf(0.0) }
    var userLng by remember { mutableStateOf(0.0) }

    var bloodType by remember { mutableStateOf("-") }
    var allergies by remember { mutableStateOf("-") }
    var medicalHistory by remember { mutableStateOf("-") }

    var myLat by remember { mutableStateOf(0.0) }
    var myLng by remember { mutableStateOf(0.0) }

    var allPetugas by remember { mutableStateOf<List<PetugasOnMap>>(emptyList()) }
    var myStatus by remember { mutableStateOf("on_the_way") }

    var route by remember { mutableStateOf<com.uxonauts.resqadmin.utils.RouteResult?>(null) }
    var hasCenteredMap by remember { mutableStateOf(false) }

    
    LaunchedEffect(alertId) {
        firestore.collection("sos_alerts").document(alertId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    globalStatus = snapshot.getString("status") ?: "active"
                    category = snapshot.getString("category") ?: "-"
                    userName = snapshot.getString("userName") ?: "Pengguna"
                    userPhone = snapshot.getString("userPhone") ?: ""
                    address = snapshot.getString("address")
                        ?: snapshot.getString("location") ?: "-"
                    userLat = snapshot.getDouble("latitude") ?: 0.0
                    userLng = snapshot.getDouble("longitude") ?: 0.0

                    @Suppress("UNCHECKED_CAST")
                    val medMap = snapshot.get("medicalInfo") as? Map<String, String>
                    if (medMap != null) {
                        bloodType = medMap["bloodType"] ?: "-"
                        allergies = medMap["allergies"] ?: "-"
                        medicalHistory = medMap["medicalHistory"] ?: "-"
                    }

                    @Suppress("UNCHECKED_CAST")
                    val respMap = snapshot.get("respondingPetugas") as? Map<String, Map<String, Any>>
                    if (respMap != null) {
                        allPetugas = respMap.map { (uid, data) ->
                            PetugasOnMap(
                                uid = uid,
                                name = (data["name"] as? String) ?: "Petugas",
                                role = ((data["role"] as? Long) ?: 2L).toInt(),
                                lat = (data["lat"] as? Double) ?: 0.0,
                                lng = (data["lng"] as? Double) ?: 0.0,
                                status = (data["status"] as? String) ?: "on_the_way"
                            )
                        }
                        
                        myStatus = allPetugas.find { it.uid == myUid }?.status ?: "on_the_way"
                    }
                }
            }
    }

    
    LaunchedEffect(Unit) {
        while (true) {
            try {
                val hasPerm = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                if (hasPerm && myUid.isNotEmpty() && myStatus != "completed") {
                    val cts = CancellationTokenSource()
                    fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                        .addOnSuccessListener { loc ->
                            if (loc != null) {
                                myLat = loc.latitude
                                myLng = loc.longitude
                                firestore.collection("sos_alerts").document(alertId)
                                    .update(
                                        mapOf(
                                            "respondingPetugas.$myUid.lat" to loc.latitude,
                                            "respondingPetugas.$myUid.lng" to loc.longitude
                                        )
                                    )
                            }
                        }
                }
            } catch (_: SecurityException) {}
            delay(5000)
        }
    }

    
    LaunchedEffect(myLat, myLng) {
        if (myLat != 0.0 && userLat != 0.0) {
            val r = com.uxonauts.resqadmin.utils.RoutingHelper.getRoute(
                myLat, myLng, userLat, userLng
            )
            if (r != null) route = r
        }
    }

    
    fun updateMyStatus(newStatus: String) {
        scope.launch {
            firestore.collection("sos_alerts").document(alertId)
                .update("respondingPetugas.$myUid.status", newStatus)
                .addOnSuccessListener {
                    
                    if (newStatus == "completed") {
                        firestore.collection("sos_alerts").document(alertId).get()
                            .addOnSuccessListener { doc ->
                                @Suppress("UNCHECKED_CAST")
                                val resp = doc.get("respondingPetugas") as? Map<String, Map<String, Any>>
                                if (resp != null) {
                                    val allCompleted = resp.values.all {
                                        (it["status"] as? String) == "completed"
                                    }
                                    if (allCompleted) {
                                        firestore.collection("sos_alerts").document(alertId)
                                            .update("status", "completed")
                                    }
                                }
                            }
                        Toast.makeText(context, "Tugas Anda selesai", Toast.LENGTH_SHORT).show()
                        navController.navigate("petugas_home") {
                            popUpTo("petugas_home") { inclusive = true }
                        }
                    }
                }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                Configuration.getInstance().load(
                    ctx, PreferenceManager.getDefaultSharedPreferences(ctx)
                )
                Configuration.getInstance().userAgentValue = ctx.packageName
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(16.0)
                    controller.setCenter(GeoPoint(userLat, userLng))
                }
            },
            update = { mv ->
                mv.overlays.clear()

                
                if (userLat != 0.0) {
                    val userMarker = Marker(mv)
                    userMarker.position = GeoPoint(userLat, userLng)
                    userMarker.title = "Korban: $userName"
                    userMarker.icon = com.uxonauts.resqadmin.utils.MapHelpers.createPinMarker(
                        mv.context, android.graphics.Color.parseColor("#F44336")
                    )
                    userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    mv.overlays.add(userMarker)
                }

                
                allPetugas.forEach { p ->
                    if (p.lat != 0.0 && p.lng != 0.0 && p.status != "completed") {
                        val color = when (p.role) {
                            2 -> "#0084FF"; 3 -> "#4CAF50"; 4 -> "#FF9800"; else -> "#9C27B0"
                        }
                        val marker = Marker(mv)
                        marker.position = GeoPoint(p.lat, p.lng)
                        marker.title = "${roleLabel(p.role)}: ${p.name}"
                        marker.icon = com.uxonauts.resqadmin.utils.MapHelpers.createDotMarker(
                            mv.context, android.graphics.Color.parseColor(color)
                        )
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        mv.overlays.add(marker)
                    }
                }

                
                route?.let { r ->
                    if (r.points.isNotEmpty()) {
                        val shadow = org.osmdroid.views.overlay.Polyline()
                        shadow.setPoints(r.points)
                        shadow.outlinePaint.color = android.graphics.Color.parseColor("#33000000")
                        shadow.outlinePaint.strokeWidth = 22f
                        shadow.outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                        shadow.outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
                        mv.overlays.add(shadow)

                        val main = org.osmdroid.views.overlay.Polyline()
                        main.setPoints(r.points)
                        main.outlinePaint.color = android.graphics.Color.parseColor("#0084FF")
                        main.outlinePaint.strokeWidth = 14f
                        main.outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                        main.outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
                        mv.overlays.add(main)
                    }
                }

                if (!hasCenteredMap && myLat != 0.0 && userLat != 0.0) {
                    mv.controller.setCenter(GeoPoint((userLat + myLat) / 2, (userLng + myLng) / 2))
                    mv.controller.setZoom(14.5)
                    hasCenteredMap = true
                }
                mv.invalidate()
            }
        )

        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.padding(top = 40.dp, start = 16.dp)
                .background(Color.White, CircleShape).size(48.dp)
        ) {
            Icon(Icons.Default.ArrowBack, "Kembali", tint = Color.Black)
        }

        
        Card(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 550.dp)
                    .verticalScroll(rememberScrollState()).padding(20.dp)
            ) {
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF44336))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(category.uppercase(), color = Color.White,
                            fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(Modifier.width(8.dp))
                    val myStatusLabel = when (myStatus) {
                        "on_the_way" -> "Sedang Menuju"
                        "arrived" -> "Sudah Tiba"
                        "completed" -> "Selesai"
                        else -> myStatus
                    }
                    Text("Status Anda: $myStatusLabel", fontSize = 12.sp, color = Color.Gray)
                }

                Spacer(Modifier.height(12.dp))

                
                if (route != null && myStatus != "completed") {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .background(Color(0xFFF0F8FF), RoundedCornerShape(12.dp)).padding(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Jarak Saya", fontSize = 11.sp, color = Color.Gray)
                            Text(
                                com.uxonauts.resqadmin.utils.RoutingHelper.formatDistance(route!!.distanceMeters),
                                fontSize = 18.sp, fontWeight = FontWeight.Bold
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Estimasi Tiba", fontSize = 11.sp, color = Color.Gray)
                            Text(
                                com.uxonauts.resqadmin.utils.RoutingHelper.formatDuration(route!!.durationSeconds),
                                fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0084FF)
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                
                Text("Informasi Korban", fontSize = 13.sp,
                    fontWeight = FontWeight.Bold, color = Color(0xFF0084FF))
                Spacer(Modifier.height(8.dp))
                InfoLine(Icons.Default.Person, "Nama", userName)
                if (userPhone.isNotEmpty()) InfoLine(Icons.Default.Phone, "Telepon", userPhone)
                InfoLine(Icons.Default.LocationOn, "Lokasi", address)

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFEEEEEE))
                Spacer(Modifier.height(12.dp))

                
                Text("Kondisi Medis", fontSize = 13.sp,
                    fontWeight = FontWeight.Bold, color = Color(0xFFE53935))
                Spacer(Modifier.height(8.dp))
                InfoLine(Icons.Default.Bloodtype, "Golongan Darah", bloodType)
                InfoLine(Icons.Default.Warning, "Alergi", allergies)
                InfoLine(Icons.Default.LocalHospital, "Riwayat Penyakit", medicalHistory)

                
                val otherPetugas = allPetugas.filter { it.uid != myUid }
                if (otherPetugas.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFFEEEEEE))
                    Spacer(Modifier.height(12.dp))
                    Text("Petugas Lain", fontSize = 13.sp,
                        fontWeight = FontWeight.Bold, color = Color(0xFF0084FF))
                    Spacer(Modifier.height(8.dp))
                    otherPetugas.forEach { p ->
                        val roleColor = when (p.role) {
                            2 -> Color(0xFF0084FF); 3 -> Color(0xFF4CAF50)
                            4 -> Color(0xFFFF9800); else -> Color.Gray
                        }
                        val pStatusLabel = when (p.status) {
                            "on_the_way" -> "Menuju"
                            "arrived" -> "Tiba"
                            "completed" -> "Selesai"
                            else -> p.status
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                .background(roleColor.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(roleColor))
                            Spacer(Modifier.width(8.dp))
                            Text("${roleLabel(p.role)} — ${p.name}",
                                fontSize = 12.sp, fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f))
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                    .background(roleColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(pStatusLabel, fontSize = 10.sp, color = roleColor,
                                    fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                
                when (myStatus) {
                    "on_the_way" -> {
                        Button(
                            onClick = { updateMyStatus("arrived") },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Sudah Tiba di Lokasi", fontWeight = FontWeight.Bold)
                        }
                    }
                    "arrived" -> {
                        Button(
                            onClick = { updateMyStatus("completed") },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Selesaikan Tugas Saya", fontWeight = FontWeight.Bold)
                        }
                    }
                    "completed" -> {
                        Box(
                            modifier = Modifier.fillMaxWidth()
                                .background(Color(0xFFE8F5E9), RoundedCornerShape(14.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, null, tint = Color(0xFF4CAF50))
                                Spacer(Modifier.width(8.dp))
                                Text("Tugas Anda Selesai", color = Color(0xFF4CAF50),
                                    fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun roleLabel(role: Int): String = when (role) {
    2 -> "Polisi"; 3 -> "Medis"; 4 -> "Damkar"; else -> "Petugas"
}

@Composable
private fun InfoLine(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 11.sp, color = Color.Gray)
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}