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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ViewCarousel
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
import com.uxonauts.resqadmin.controllers.PetugasArtikelController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetugasArtikelEditorScreen(
    navController: NavController,
    controller: PetugasArtikelController = viewModel()
) {
    val context = LocalContext.current
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { controller.editImageUri = it } }

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
                title = {
                    Text(
                        if (controller.editingArticle == null) "Konten Baru" else "Edit Konten",
                        fontWeight = FontWeight.Bold
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            
            Text(
                "Jenis Konten",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFEFF6FF))
                    .padding(4.dp)
            ) {
                TypeSegment(
                    icon = Icons.Default.Article,
                    label = "Artikel",
                    selected = controller.editType == "article",
                    onClick = { controller.editType = "article" },
                    modifier = Modifier.weight(1f)
                )
                TypeSegment(
                    icon = Icons.Default.ViewCarousel,
                    label = "Banner",
                    selected = controller.editType == "banner",
                    onClick = { controller.editType = "banner" },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(6.dp))
            Text(
                if (controller.editType == "article")
                    "Akan tampil di bagian Artikel pada aplikasi pengguna"
                else
                    "Akan tampil sebagai banner utama di halaman home pengguna",
                fontSize = 11.sp,
                color = Color.Gray
            )

            Spacer(Modifier.height(24.dp))

            
            Text(
                "Gambar Cover",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray
            )
            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF5F5F5))
                    .border(
                        1.dp,
                        Color(0xFFE0E0E0),
                        RoundedCornerShape(16.dp)
                    )
                    .clickable { galleryLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                when {
                    controller.editImageUri != null -> {
                        Image(
                            painter = rememberAsyncImagePainter(controller.editImageUri),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    controller.editingArticle?.gambarUrl?.isNotEmpty() == true -> {
                        Image(
                            painter = rememberAsyncImagePainter(
                                controller.editingArticle!!.gambarUrl
                            ),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    else -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.AddPhotoAlternate,
                                null,
                                tint = Color(0xFF0084FF),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("Ketuk untuk pilih gambar", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            
            Text(
                "Judul",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = controller.editJudul,
                onValueChange = { controller.editJudul = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Masukkan judul") },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))

            
            Text(
                "Konten",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = controller.editKonten,
                onValueChange = { controller.editKonten = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                placeholder = { Text("Tulis konten di sini...") },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    controller.saveArticle {
                        navController.popBackStack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0084FF)),
                shape = RoundedCornerShape(14.dp),
                enabled = !controller.isLoading
            ) {
                if (controller.isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "Publikasikan",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun TypeSegment(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
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
        Icon(
            icon,
            null,
            tint = if (selected) Color.White else Color(0xFF0084FF),
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            color = if (selected) Color.White else Color(0xFF0084FF),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}
