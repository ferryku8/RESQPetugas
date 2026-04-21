package com.uxonauts.resqadmin.views.petugas

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.uxonauts.resqadmin.controllers.PetugasArtikelController
import com.uxonauts.resqadmin.models.Artikel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetugasArtikelScreen(
    navController: NavController,
    controller: PetugasArtikelController = viewModel()
) {
    val context = LocalContext.current

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
        bottomBar = {
            PetugasBottomBar(
                selectedTab = "artikel",
                onHomeClick = { navController.navigate("petugas_home") { popUpTo("petugas_home") { inclusive = true } } },
                onArtikelClick = {},
                onProfileClick = { navController.navigate("petugas_profile") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    controller.startNewArticle()
                    navController.navigate("petugas_artikel_editor")
                },
                containerColor = Color(0xFF0084FF),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, "Tambah Konten")
            }
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
            Text(
                "Artikel",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))

            if (controller.isLoading && controller.artikelList.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF0084FF))
                }
            } else if (controller.artikelList.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Belum ada artikel", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(controller.artikelList) { artikel ->
                        ArtikelAdminCard(
                            artikel = artikel,
                            onUpdate = {
                                controller.startEditArticle(artikel)
                                navController.navigate("petugas_artikel_editor")
                            },
                            onDelete = { controller.deleteArticle(artikel.articleId) }
                        )
                    }
                    item { Spacer(Modifier.height(100.dp)) }
                }
            }
        }
    }
}

@Composable
fun ArtikelAdminCard(artikel: Artikel, onUpdate: () -> Unit, onDelete: () -> Unit) {
    val dateText = artikel.tglPublish?.toDate()?.let {
        SimpleDateFormat("d MMMM yyyy", Locale("id", "ID")).format(it)
    } ?: "-"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                if (artikel.gambarUrl.isNotEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(artikel.gambarUrl),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Image, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                }
            }
            
            if (artikel.type == "banner") {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFFFC107))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("BANNER", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(Modifier.height(6.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                artikel.judul,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                Spacer(Modifier.width(4.dp))
                Text(dateText, fontSize = 11.sp, color = Color.Gray)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                artikel.konten,
                fontSize = 12.sp,
                color = Color.Gray,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onUpdate,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0084FF))
                ) { Text("Update") }
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f)
                ) { Text("Hapus", color = Color(0xFFB3D9FF)) }
            }
        }
    }
}