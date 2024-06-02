package com.example.hw_03

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.provider.Settings
import androidx.work.Worker
import androidx.work.WorkerParameters
import android.util.Log

class BluetoothAirplaneWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val bluetoothStatus = getBluetoothStatus()
        val airplaneModeStatus = getAirplaneModeStatus()

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
}
