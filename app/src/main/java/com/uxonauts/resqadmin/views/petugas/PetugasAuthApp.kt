package com.uxonauts.resqadmin.views.petugas

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.uxonauts.resqadmin.controllers.PetugasAuthController

@Composable
fun PetugasAuthApp() {
    val navController = rememberNavController()
    val authController: PetugasAuthController = viewModel()

    
    val startDest = if (FirebaseAuth.getInstance().currentUser != null) {
        "petugas_home"
    } else {
        "petugas_login"
    }

    NavHost(navController = navController, startDestination = startDest) {
        composable("petugas_login") {
            PetugasLoginScreen(navController, authController)
        }
        composable("petugas_home") {
            PetugasHomeScreen(navController)
        }
        composable("petugas_artikel") { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry("petugas_home")
            }
            val artikelController: com.uxonauts.resqadmin.controllers.PetugasArtikelController =
                androidx.lifecycle.viewmodel.compose.viewModel(parentEntry)
            PetugasArtikelScreen(navController, artikelController)
        }

        composable("petugas_artikel_editor") { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry("petugas_home")
            }
            val artikelController: com.uxonauts.resqadmin.controllers.PetugasArtikelController =
                androidx.lifecycle.viewmodel.compose.viewModel(parentEntry)
            PetugasArtikelEditorScreen(navController, artikelController)
        }
        composable("petugas_profile") {
            PetugasProfileScreen(navController)
        }
        composable("petugas_edit_profile") {
            PetugasEditProfileScreen(navController)
        }
        composable(
            route = "petugas_tracking/{alertId}",
            arguments = listOf(navArgument("alertId") { type = NavType.StringType })
        ) { backStackEntry ->
            val alertId = backStackEntry.arguments?.getString("alertId") ?: ""
            PetugasTrackingScreen(navController, alertId)
        }

        composable(
            route = "petugas_laporan_detail/{reportId}",
            arguments = listOf(navArgument("reportId") { type = NavType.StringType })
        ) { backStackEntry ->
            val reportId = backStackEntry.arguments?.getString("reportId") ?: ""
            PetugasLaporanDetailScreen(navController, reportId)
        }

        composable(
            route = "petugas_laporan_detail/{reportId}",
            arguments = listOf(
                androidx.navigation.navArgument("reportId") {
                    type = androidx.navigation.NavType.StringType
                }
            )
        ) { backStackEntry ->
            val reportId = backStackEntry.arguments?.getString("reportId") ?: ""
            PetugasLaporanDetailScreen(navController, reportId)
        }
    }
}