package com.uxonauts.resqadmin.views.petugas

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.uxonauts.resqadmin.controllers.PetugasProfileController
import com.uxonauts.resqadmin.views.petugas.ui.theme.ResqBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetugasEditProfileScreen(
    navController: NavController,
    controller: PetugasProfileController = viewModel()
) {
    val context = LocalContext.current
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { controller.uploadProfilePhoto(it) } }

    LaunchedEffect(controller.successMessage, controller.errorMessage) {
        controller.successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            controller.clearMessages()
        }
        controller.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            controller.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Edit Profil", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Kembali")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFFFBFBFB))
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
            Spacer(Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .clip(CircleShape)
                            .background(Color.LightGray)
                            .border(3.dp, ResqBlue, CircleShape)
                            .clickable { galleryLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (controller.isUploadingPhoto) {
                            CircularProgressIndicator(color = ResqBlue)
                        } else if (controller.petugas.profileImageUrl.isNotEmpty()) {
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
                    Box(
                        modifier = Modifier.size(38.dp).clip(CircleShape).background(ResqBlue)
                            .clickable { galleryLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Text("Biodata", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = controller.editNama,
                onValueChange = { controller.editNama = it },
                label = { Text("Nama Lengkap") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = controller.editTelepon,
                onValueChange = { controller.editTelepon = it },
                label = { Text("No. Telepon") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = controller.editAlamat,
                onValueChange = { controller.editAlamat = it },
                label = { Text("Alamat") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = controller.editInstansi,
                onValueChange = { controller.editInstansi = it },
                label = { Text("Instansi") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = controller.editNip,
                onValueChange = { controller.editNip = it },
                label = { Text("NIP") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { controller.saveProfile() },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ResqBlue)
            ) { Text("Simpan Perubahan", fontSize = 16.sp) }

            Spacer(Modifier.height(40.dp))
        }
    }
}