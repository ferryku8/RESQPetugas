package com.uxonauts.resqadmin.views.petugas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun PetugasBottomBar(
    selectedTab: String,
    onHomeClick: () -> Unit,
    onArtikelClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 16.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .padding(horizontal = 48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onHomeClick) {
                Icon(
                    imageVector = if (selectedTab == "home") Icons.Filled.Home
                    else androidx.compose.material.icons.Icons.Outlined.Home,
                    contentDescription = "Home",
                    tint = Color(0xFF0084FF),
                    modifier = Modifier.size(32.dp)
                )
            }
            IconButton(onClick = onArtikelClick) {
                Icon(
                    imageVector = if (selectedTab == "artikel") Icons.Filled.Article
                    else androidx.compose.material.icons.Icons.Outlined.Article,
                    contentDescription = "Artikel",
                    tint = Color(0xFF0084FF),
                    modifier = Modifier.size(32.dp)
                )
            }
            IconButton(onClick = onProfileClick) {
                Icon(
                    imageVector = if (selectedTab == "profile") Icons.Filled.Person
                    else androidx.compose.material.icons.Icons.Outlined.Person,
                    contentDescription = "Profile",
                    tint = Color(0xFF0084FF),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}