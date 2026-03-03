package com.example.tauanitoapp.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.tauanitoapp.MainActivity
import com.example.tauanitoapp.R
import com.example.tauanitoapp.data.repository.NotificationRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class TauanitoMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        try {
            val title = remoteMessage.notification?.title ?: "Tauanito Alert"
            val body = remoteMessage.notification?.body ?: "Nuovo messaggio ricevuto"

            NotificationRepository.addNotification(title, body)
            sendNotification(title, body)
        } catch (e: Exception) {
            Log.e("TauanitoFCM", "Errore ricezione messaggio: ${e.message}")
        }
    }

    override fun onNewToken(token: String) {
        Log.d("TauanitoFCM", "Nuovo token: $token")
    }

    private fun sendNotification(title: String, body: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(this, 0, intent, flags)

        val channelId = "tauanito_alerts_v2"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Avvisi Sensori",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifiche per allarmi sensori e batteria"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher) // Usiamo mipmap che è garantito esistere
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}
