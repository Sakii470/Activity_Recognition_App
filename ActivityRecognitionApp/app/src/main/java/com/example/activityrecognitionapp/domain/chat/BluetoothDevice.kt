package com.example.activityrecognitionapp.domain.chat

import android.bluetooth.BluetoothAdapter

typealias BluetoothDeviceDomain = BluetoothDevice

data class BluetoothDevice(
    val name: String?,
    val address: String,
    val signalStrength: Int?
) {
    fun createBond() {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        val device = adapter.getRemoteDevice(address)

    }
}
