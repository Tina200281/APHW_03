package com.example.hw_03

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import androidx.core.content.ContextCompat

object ConnectStatus {
    private val _isInternetConnected = MutableStateFlow("")
    val isInternetConnected = _isInternetConnected.asStateFlow()

    fun updateStatus(status: String) {
        _isInternetConnected.value = status
    }
}

class InternetService : Service() {
    private var receiver: BroadcastReceiver? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val cm = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val networkInfo = cm.activeNetworkInfo
                val status = if (networkInfo != null && networkInfo.isConnected) {
                    "Connected."
                } else {
                    "Disconnected."
                }
                ConnectStatus.updateStatus(status)
                context?.let { safeContext ->
                    if (hasNotificationPermission(safeContext)) {
                        sendNotification(safeContext, "Status", "Internet $status")
                    }
                    logStatus(safeContext, status)
                }
            }
        }
        registerReceiver(receiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun logStatus(context: Context, status: String) {
        val currentDate = getCurrentTimestamp()
        val logObject = JSONObject()
        logObject.put("timestamp", currentDate)
        logObject.put("type", "Internet")
        logObject.put("status", status)
        writeToFile(context, logObject)
    }

    private fun sendNotification(context: Context, title: String, message: String) {
        val notificationId = 1
        val channelId = "default_channel_id"
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, "android.permission.POST_NOTIFICATIONS") == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Default Channel"
            val descriptionText = "This is the default notification channel."
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("default_channel_id", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
