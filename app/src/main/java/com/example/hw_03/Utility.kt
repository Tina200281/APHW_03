package com.example.hw_03

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun writeToFile(context: Context, logObject: JSONObject = JSONObject()) {
    val logArray = JSONArray().put(logObject)
    val logFile = File(context.filesDir, "logs.json")

    logFile.appendText(logArray.toString())
    Log.v("status", "Log entries saved to: ${logFile.absolutePath}")
}

fun getCurrentTimestamp(): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val date = Date()
    return dateFormat.format(date)
}
