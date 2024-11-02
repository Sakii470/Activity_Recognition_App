package com.example.activityrecognitionapp.domain

/**
 * Represents a Bluetooth device with relevant details such as its name, address,
 * and signal strength. This class is used to manage and display information
 * about discovered Bluetooth devices in the application.
 *
 * @property name The name of the Bluetooth device, which may be null if not available.
 * @property address The unique address of the Bluetooth device.
 * @property signalStrength The signal strength of the Bluetooth device, represented as an integer,
 *                          which may be null if not available.
 */

typealias BluetoothDeviceDomain = BluetoothDevice

data class BluetoothDevice(
    val name: String?,
    val address: String,
    val signalStrength: Int?=null,

)


