package com.example.hw_03

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.hw_03.ui.theme.HW_03Theme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val logStateFlow = MutableStateFlow<List<String>>(emptyList())

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkForPermissions()

        val serviceIntent = Intent(this, InternetService::class.java)
        startService(serviceIntent)

        setupPeriodicWork()

        lifecycleScope.launch {
            logStateFlow.value = readLogsFromFile(this@MainActivity)
        }

        setContent {
            HW_03Theme {
                MyUI(context = this, logStateFlow = logStateFlow.asStateFlow())
            }
        }
    }

    private fun setupPeriodicWork() {
        val workRequest =
            PeriodicWorkRequestBuilder<BluetoothAirplaneWorker>(15, TimeUnit.MINUTES).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "BluetoothAirplaneModeWorker",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun checkForPermissions() {
        val permissionState = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
        if (permissionState == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Permission granted, proceed with functionality that depends on this permission.
            } else {
                // Permission denied, inform the user about the limitation or request again.
            }
        }
    }

    private fun readLogsFromFile(context: Context): List<String> {
        val logFile = File(context.filesDir, "logs.json")
        return if (logFile.exists()) {
            try {
                val logs = mutableListOf<String>()
                val logArray = JSONArray(logFile.readText())
                for (i in 0 until logArray.length()) {
                    val logObject = logArray.getJSONObject(i)
                    val timestamp = logObject.optString("timestamp", "Unknown Timestamp")
                    val type = logObject.optString("type", "Unknown Type")
                    val status = logObject.optString("status", "Unknown Status")
                    logs.add("$timestamp - $type: $status")
                }
                logs.sortedByDescending { it.split(" - ")[0] } // مرتب‌سازی بر اساس زمان
            } catch (e: Exception) {
                Log.e("readLogsFromFile", "Error reading log file: ${e.message}")
                emptyList()
            }
        } else {
            emptyList()
        }
    }
}

@Composable
fun MyUI(context: Context, logStateFlow: StateFlow<List<String>>) {
    val logs by logStateFlow.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "Bluetooth and Airplane Mode Logs", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 18.sp)
        LogList(logs = logs)
    }
}

@Composable
fun LogList(logs: List<String>, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier) {
        items(logs) { log ->
            LogItem(log = log)
        }
    }
}

@Composable
fun LogItem(log: String) {
    Text(text = log, fontSize = 14.sp, color = Color.Black, modifier = Modifier.padding(8.dp))
}
