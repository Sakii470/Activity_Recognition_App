package com.example.activityrecognitionapp.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import com.example.activityrecognitionapp.domain.BluetoothDeviceDomain

/**
 * Extension functions for the Android BluetoothDevice class to convert
 * a BluetoothDevice instance into a BluetoothDeviceDomain instance,
 * which is part of the app's domain model.
 */

@SuppressLint("MissingPermission")
fun BluetoothDevice.toBluetoothDeviceDomain(rssi: Int?): BluetoothDeviceDomain {
    return BluetoothDeviceDomain(
        name = name,
        address = address,
        signalStrength = rssi,
    )
}
@SuppressLint("MissingPermission")
fun BluetoothDevice.toBluetoothDeviceDomain(): BluetoothDeviceDomain {
    return BluetoothDeviceDomain(
        name = name,
        address = address,
    )
}



