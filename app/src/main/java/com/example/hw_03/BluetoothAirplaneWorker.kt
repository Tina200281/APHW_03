package com.example.hw_03

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.provider.Settings
import androidx.work.Worker
import androidx.work.WorkerParameters
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BluetoothAirplaneWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    private val logStateFlow: MutableStateFlow<List<String>> = MutableStateFlow(emptyList())

    override fun doWork(): Result {
        val bluetoothStatus = getBluetoothStatus()
        val airplaneModeStatus = getAirplaneModeStatus()

        val logObjectBluetooth = JSONObject().apply {
            put("timestamp", getCurrentTimestamp())
            put("type", "Bluetooth")
            put("status", bluetoothStatus)
        }

        val logObjectAirplane = JSONObject().apply {
            put("timestamp", getCurrentTimestamp())
            put("type", "Airplane Mode")
            put("status", airplaneModeStatus)
        }

        writeToFile(applicationContext, logObjectBluetooth)
        writeToFile(applicationContext, logObjectAirplane)

        updateLogs(applicationContext)

        Log.i("worker_airplane", "Bluetooth Status: $bluetoothStatus, Airplane Mode Status: $airplaneModeStatus")

        return Result.success()
    }

    private fun getBluetoothStatus(): String {
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        return if (bluetoothAdapter?.isEnabled == true) "Enabled" else "Disabled"
    }

    private fun getAirplaneModeStatus(): String {
        return if (isAirplaneModeOn(applicationContext)) "Enabled" else "Disabled"
    }

    private fun isAirplaneModeOn(context: Context): Boolean {
        return Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0
    }

    private fun getCurrentTimestamp(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = Date()
        return dateFormat.format(date)
    }

    private fun writeToFile(context: Context, logObject: JSONObject) {
        try {
            val logFile = File(context.filesDir, "logs.json")
            val logArray = if (logFile.exists()) {
                JSONArray(logFile.readText())
            } else {
                JSONArray()
            }
            logArray.put(logObject)
            logFile.writeText(logArray.toString())
        } catch (e: JSONException) {
            Log.e("writeToFile", "JSON error: ${e.message}")
        } catch (e: Exception) {
            Log.e("writeToFile", "File writing error: ${e.message}")
        }
    }

    private fun updateLogs(context: Context) {
        runBlocking {
            logStateFlow.update { readLogsFromFile(context) }
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

    fun getLogStateFlow(): StateFlow<List<String>> = logStateFlow.asStateFlow()
}
