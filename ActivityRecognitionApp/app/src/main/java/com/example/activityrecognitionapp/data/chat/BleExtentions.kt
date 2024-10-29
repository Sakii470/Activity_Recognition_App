package com.example.activityrecognitionapp.data.chat

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.util.Log
import java.util.Locale

/**
 * Extension functions and utilities for handling Bluetooth GATT (Generic Attribute Profile) operations.
 * This includes printing GATT services and characteristics, checking their properties,
 * and converting byte arrays to hex strings.
 */

const val CCCD_DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805F9B34FB"

/**
 * Prints the GATT services and their characteristics along with their properties
 * to the log. It checks if there are any services available before attempting to
 * print them.
 */
fun BluetoothGatt.printGattTable() {
    if (services.isEmpty()) {
        Log.d("BluetoothGatt","No service and characteristic available, call discoverServices() first?")
        return
    }
    // Iterate through each service to log its characteristics and their properties
    services.forEach { service ->
        val characteristicsTable = service.characteristics.joinToString(
            separator = "\n|--",
            prefix = "|--"
        ) { char ->
            var description = "${char.uuid}: ${char.printProperties()}"
            // If the characteristic has descriptors, log their properties too
            if (char.descriptors.isNotEmpty()) {
                description += "\n" + char.descriptors.joinToString(
                    separator = "\n|------",
                    prefix = "|------"
                ) { descriptor ->
                    "${descriptor.uuid}: ${descriptor.printProperties()}"
                }
            }
            description
        }
        Log.d("BluetoothGatt","Service ${service.uuid}\nCharacteristics:\n$characteristicsTable")
    }
}

/**
 * Returns a string representing the properties of a BluetoothGattCharacteristic.
 */
fun BluetoothGattCharacteristic.printProperties(): String = mutableListOf<String>().apply {
    if (isReadable()) add("READABLE") // Checks if characteristic is readable
    if (isWritable()) add("WRITABLE") // Checks if characteristic is writable
    if (isWritableWithoutResponse()) add("WRITABLE WITHOUT RESPONSE") // Checks for writable without response
    if (isIndicatable()) add("INDICATABLE") // Checks if characteristic can indicate
    if (isNotifiable()) add("NOTIFIABLE") // Checks if characteristic can notify
    if (isEmpty()) add("EMPTY") // Checks if there are no properties
}.joinToString()

// Check if the characteristic is readable
fun BluetoothGattCharacteristic.isReadable(): Boolean =
    containsProperty(BluetoothGattCharacteristic.PROPERTY_READ)

// Check if the characteristic is writable
fun BluetoothGattCharacteristic.isWritable(): Boolean =
    containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE)

// Check if the characteristic is writable without response
fun BluetoothGattCharacteristic.isWritableWithoutResponse(): Boolean =
    containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)

// Check if the characteristic can indicate
fun BluetoothGattCharacteristic.isIndicatable(): Boolean =
    containsProperty(BluetoothGattCharacteristic.PROPERTY_INDICATE)

// Check if the characteristic can notify
fun BluetoothGattCharacteristic.isNotifiable(): Boolean =
    containsProperty(BluetoothGattCharacteristic.PROPERTY_NOTIFY)

// Helper function to check if a characteristic contains a specific property
fun BluetoothGattCharacteristic.containsProperty(property: Int): Boolean =
    properties and property != 0

/**
 * Returns a string representing the properties of a BluetoothGattDescriptor.
 */
fun BluetoothGattDescriptor.printProperties(): String = mutableListOf<String>().apply {
    if (isReadable()) add("READABLE") // Checks if descriptor is readable
    if (isWritable()) add("WRITABLE") // Checks if descriptor is writable
    if (isEmpty()) add("EMPTY") // Checks if there are no properties
}.joinToString()

// Check if the descriptor is readable
fun BluetoothGattDescriptor.isReadable(): Boolean =
    containsPermission(BluetoothGattDescriptor.PERMISSION_READ)

// Check if the descriptor is writable
fun BluetoothGattDescriptor.isWritable(): Boolean =
    containsPermission(BluetoothGattDescriptor.PERMISSION_WRITE)

// Helper function to check if a descriptor contains a specific permission
fun BluetoothGattDescriptor.containsPermission(permission: Int): Boolean =
    permissions and permission != 0

// Check if the descriptor is a Client Characteristic Configuration Descriptor (CCCD)
fun BluetoothGattDescriptor.isCccd() =
    uuid.toString().uppercase(Locale.US) == CCCD_DESCRIPTOR_UUID.uppercase(Locale.US)

/**
 * Converts a ByteArray to a hex string representation.
 */
fun ByteArray.toHexString(): String =
    joinToString(separator = " ", prefix = "0x") { String.format("%02X", it) }
