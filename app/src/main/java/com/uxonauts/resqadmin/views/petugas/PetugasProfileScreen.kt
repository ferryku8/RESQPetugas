package com.uxonauts.resqadmin.views.petugas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.uxonauts.resqadmin.controllers.PetugasProfileController
import com.uxonauts.resqadmin.views.petugas.ui.theme.ResqBlue
import com.uxonauts.resqadmin.views.petugas.ui.theme.TextGray
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import com.uxonauts.resqadmin.services.SosListenerService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetugasProfileScreen(
    navController: NavController,
    controller: PetugasProfileController = viewModel()
) {
    var showSettings by remember { mutableStateOf(false) }
    val context = LocalContext.current
    LaunchedEffect(Unit) { controller.fetchProfile() }

    Scaffold(
        bottomBar = {
            PetugasBottomBar(
                selectedTab = "profile",
                onHomeClick = { navController.navigate("petugas_home") { popUpTo("petugas_home") { inclusive = true } } },
                onArtikelClick = { navController.navigate("petugas_artikel") },
                onProfileClick = {}
            )
        },
        containerColor = Color(0xFFFBFBFB)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = { showSettings = true }) {
                    Icon(Icons.Default.Settings, null, tint = ResqBlue, modifier = Modifier.size(32.dp))
                }
            }

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray)
                        .border(3.dp, ResqBlue, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (controller.petugas.profileImageUrl.isNotEmpty()) {
                        Image(
                            painter = rememberAsyncImagePainter(controller.petugas.profileImageUrl),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Person, null, modifier = Modifier.size(70.dp), tint = Color.Gray)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                controller.petugas.namaLengkap.ifEmpty { "Petugas" },
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    StatItem(
                        value = "${controller.laporanHariIniCount}",
                        label = "Laporan hari ini",
                        modifier = Modifier.weight(1f)
                    )
                    VerticalDivider(color = Color(0xFFE0E0E0))
                    StatItem(
                        value = "${controller.totalLaporanCount}",
                        label = "Total laporan",
                        modifier = Modifier.weight(1f)
                    )
                    VerticalDivider(color = Color(0xFFE0E0E0))
                    StatItem(
                        value = "${controller.responseRatePercent}%",
                        label = "Response Rate",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            ExpandSection("Biodata Diri", controller.biodataExpanded,
                { controller.biodataExpanded = !controller.biodataExpanded }) {
                InfoItem("Nama", controller.petugas.namaLengkap)
                InfoItem("Email", controller.petugas.email)
                InfoItem("No. Telepon", controller.petugas.noTelepon)
                InfoItem("Alamat", controller.petugas.alamat)
            }

            Spacer(Modifier.height(12.dp))

            ExpandSection("Informasi Instansi", controller.kesehatanExpanded,
                { controller.kesehatanExpanded = !controller.kesehatanExpanded }) {
                InfoItem("Instansi", controller.petugas.instansi)
                InfoItem("NIP", controller.petugas.nip)
                InfoItem("Jabatan", controller.petugas.roleName())
            }

            Spacer(Modifier.height(120.dp))
        }
    }


    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            containerColor = Color.White
        ) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Text("Pengaturan", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showSettings = false
                            navController.navigate("petugas_edit_profile")
                        }
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Edit, null, tint = ResqBlue)
                    Spacer(Modifier.width(16.dp))
                    Text("Edit Profil", fontSize = 16.sp, color = ResqBlue, fontWeight = FontWeight.Medium)
                }

                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showSettings = false
                            
                            context.stopService(Intent(context, SosListenerService::class.java))
                            controller.logout(navController)
                        }

                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                    
                ) {
                    Icon(Icons.Default.Logout, null, tint = Color.Red)
                    Spacer(Modifier.width(16.dp))
                    Text("Logout", fontSize = 16.sp, color = Color.Red, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun StatItem(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = ResqBlue)
        Text(label, fontSize = 11.sp, color = Color.Black)
    }
}

@Composable
fun InfoItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(label, fontSize = 12.sp, color = TextGray)
        Text(value.ifEmpty { "-" }, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ExpandSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onToggle() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    null
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp), content = content)
            }
        }
    }
}