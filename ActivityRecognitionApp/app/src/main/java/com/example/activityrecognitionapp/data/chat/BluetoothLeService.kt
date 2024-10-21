package com.example.activityrecognitionapp.data.chat

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.content.ContentValues
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log

class BluetoothLeService : Service(){

    private var bluetoothAdapter: BluetoothAdapter? = null
    fun initialize(): Boolean {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Log.e(ContentValues.TAG, "Unable to obtain a BluetoothAdapter.")
            return false
        }
        return true
    }



    private val binder = LocalBinder()

    override fun onBind(intent: Intent): IBinder? {
        return binder
    }

    inner class LocalBinder : Binder() {
        fun getService() : BluetoothLeService {
            return this@BluetoothLeService
        }
    }
}