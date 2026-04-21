package com.uxonauts.resqadmin.views.petugas

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.uxonauts.resqadmin.R
import com.uxonauts.resqadmin.controllers.PetugasAuthController
import com.uxonauts.resqadmin.views.petugas.ui.theme.ResqBlue

@Composable
fun PetugasLoginScreen(navController: NavController, controller: PetugasAuthController) {
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    fun validateAndLogin() {
        emailError = null
        passwordError = null
        controller.errorMessage = null

        var hasError = false

        if (controller.loginEmail.isBlank()) {
            emailError = "Email tidak boleh kosong"
            hasError = true
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(controller.loginEmail).matches()) {
            emailError = "Format email tidak valid"
            hasError = true
        }

        if (controller.loginPassword.isBlank()) {
            passwordError = "Kata sandi tidak boleh kosong"
            hasError = true
        } else if (controller.loginPassword.length < 6) {
            passwordError = "Kata sandi minimal 6 karakter"
            hasError = true
        }

        if (hasError) return

        controller.doLogin(navController)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "Logo",
            modifier = Modifier.size(120.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text("RESQ Petugas", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = ResqBlue)
        Spacer(Modifier.height(32.dp))

        Text("Masuk ke Akun Petugas", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = controller.loginEmail,
            onValueChange = {
                controller.loginEmail = it
                emailError = null
                controller.errorMessage = null
            },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            isError = emailError != null,
            supportingText = {
                if (emailError != null) {
                    Text(emailError!!, color = MaterialTheme.colorScheme.error)
                }
            },
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = controller.loginPassword,
            onValueChange = {
                controller.loginPassword = it
                passwordError = null
                controller.errorMessage = null
            },
            label = { Text("Kata Sandi") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            isError = passwordError != null,
            supportingText = {
                if (passwordError != null) {
                    Text(passwordError!!, color = MaterialTheme.colorScheme.error)
                }
            },
            singleLine = true
        )

        if (controller.errorMessage != null) {
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = formatPetugasLoginError(controller.errorMessage!!),
                    color = Color(0xFFD32F2F),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(12.dp),
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { validateAndLogin() },
            enabled = !controller.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ResqBlue),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (controller.isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text("Masuk", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun formatPetugasLoginError(error: String): String {
    return when {
        error.contains("bukan akun petugas", ignoreCase = true) ->
            "Akun ini bukan akun petugas. Gunakan akun petugas yang terdaftar."

        error.contains("no user record", ignoreCase = true) ||
                error.contains("USER_NOT_FOUND", ignoreCase = true) ->
            "Akun dengan email ini tidak ditemukan."

        error.contains("password is invalid", ignoreCase = true) ||
                error.contains("INVALID_PASSWORD", ignoreCase = true) ->
            "Kata sandi salah. Silakan coba lagi."

        error.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ||
                error.contains("invalid credential", ignoreCase = true) ->
            "Email atau kata sandi salah. Silakan periksa kembali."

        error.contains("too many", ignoreCase = true) ||
                error.contains("TOO_MANY_ATTEMPTS", ignoreCase = true) ->
            "Terlalu banyak percobaan login gagal. Coba lagi dalam beberapa menit."

        error.contains("network", ignoreCase = true) ->
            "Tidak ada koneksi internet. Periksa jaringan Anda."

        error.contains("disabled", ignoreCase = true) ->
            "Akun ini telah dinonaktifkan. Hubungi administrator."

        else -> "Login gagal. Silakan periksa email dan kata sandi Anda."
    }
}