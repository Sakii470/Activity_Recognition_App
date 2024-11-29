package com.example.activityrecognitionapp.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import com.example.activityrecognitionapp.domain.BluetoothDeviceDomain

/**
 * Extension functions to convert [BluetoothDevice] instances into [BluetoothDeviceDomain] objects.
 *
 * These functions facilitate the transformation of Bluetooth device information from the system's
 * representation to the application's domain-specific model, enabling consistent data handling
 * within the app.
 */

/**
 * Converts a [BluetoothDevice] to a [BluetoothDeviceDomain], including the signal strength (RSSI).
 *
 * @param rssi The Received Signal Strength Indicator value of the Bluetooth device.
 * @return A [BluetoothDeviceDomain] instance containing the device's name, address, and signal strength.
 */
@SuppressLint("MissingPermission")
fun BluetoothDevice.toBluetoothDeviceDomain(rssi: Int?): BluetoothDeviceDomain {
    return BluetoothDeviceDomain(
        name = name,
        address = address,
        signalStrength = rssi,
    )
}

/**
 * Converts a [BluetoothDevice] to a [BluetoothDeviceDomain] without including signal strength.
 *
 * @return A [BluetoothDeviceDomain] instance containing the device's name and address.
 */
@SuppressLint("MissingPermission")
fun BluetoothDevice.toBluetoothDeviceDomain(): BluetoothDeviceDomain {
    return BluetoothDeviceDomain(
        name = name,
        address = address,
    )
}
