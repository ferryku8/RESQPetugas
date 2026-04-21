package com.uxonauts.resqadmin.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.uxonauts.resqadmin.MainActivity
import com.uxonauts.resqadmin.R

class SosListenerService : Service() {

    private val db = FirebaseFirestore.getInstance()
    private var sosListener: ListenerRegistration? = null
    private var laporanListener: ListenerRegistration? = null
    private var petugasRole: Int = 0
    private var isFirstSosSnapshot = true
    private var isFirstLaporanSnapshot = true

    companion object {
        const val FOREGROUND_CHANNEL = "resq_foreground"
        const val ALERT_CHANNEL = "resq_alerts"
        const val FOREGROUND_ID = 1001
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        startForegroundNotification()
        fetchRoleAndStartListening()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY  // Service restart otomatis kalau di-kill OS
    }

    override fun onDestroy() {
        super.onDestroy()
        sosListener?.remove()
        laporanListener?.remove()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val foregroundChannel = NotificationChannel(
                FOREGROUND_CHANNEL,
                "RESQ Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Memantau laporan darurat"
                setShowBadge(false)
            }
            manager.createNotificationChannel(foregroundChannel)
            val alertChannel = NotificationChannel(
                ALERT_CHANNEL,
                "Laporan Darurat",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi SOS dan laporan baru"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }
            manager.createNotificationChannel(alertChannel)
        }
    }

    private fun startForegroundNotification() {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, FOREGROUND_CHANNEL)
            .setContentTitle("RESQ Petugas")
            .setContentText("Memantau laporan darurat...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()

        startForeground(FOREGROUND_ID, notification)
    }

    private fun fetchRoleAndStartListening() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.collection("petugas").document(uid).get()
            .addOnSuccessListener { doc ->
                petugasRole = (doc.getLong("role") ?: 2L).toInt()
                startSosListener()
                startLaporanListener()
            }
    }

    private fun startSosListener() {
        sosListener = db.collection("sos_alerts")
            .whereArrayContains("targetRoles", petugasRole)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                if (isFirstSosSnapshot) {
                    isFirstSosSnapshot = false
                    return@addSnapshotListener
                }
                for (change in snapshot.documentChanges) {
                    if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                        val doc = change.document
                        val category = doc.getString("category") ?: "SOS"
                        val userName = doc.getString("userName") ?: "Seseorang"
                        val address = doc.getString("address") ?: ""

                        showAlertNotification(
                            title = "🚨 SOS Darurat: $category",
                            body = "$userName membutuhkan bantuan di $address",
                            notifId = doc.id.hashCode()
                        )
                    }
                }
            }
    }

    private fun startLaporanListener() {
        laporanListener = db.collection("reports")
            .whereArrayContains("targetRoles", petugasRole)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                if (isFirstLaporanSnapshot) {
                    isFirstLaporanSnapshot = false
                    return@addSnapshotListener
                }

                for (change in snapshot.documentChanges) {
                    if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                        val doc = change.document
                        val subJenis = doc.getString("subJenis") ?: "Laporan"
                        val judul = doc.getString("judul") ?: ""
                        val pelapor = doc.getString("namaPelapor") ?: ""

                        showAlertNotification(
                            title = "📋 Laporan Baru: $subJenis",
                            body = "$judul — oleh $pelapor",
                            notifId = doc.id.hashCode() + 10000
                        )
                    }
                }
            }
    }

    private fun showAlertNotification(title: String, body: String, notifId: Int) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, notifId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(notifId, notification)
    }
}