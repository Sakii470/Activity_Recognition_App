package com.example.activityrecognitionapp.data.chat

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import com.example.activityrecognitionapp.domain.chat.BluetoothDeviceDomain





@SuppressLint("MissingPermission")
fun BluetoothDevice.toBluetoothDeviceDomain(rssi: Int?): BluetoothDeviceDomain {
    return BluetoothDeviceDomain(
        name = name,
        address = address,
        signalStrength = rssi,
    )
}
@SuppressLint("MissingPermission")
fun BluetoothDevice.toBluetoothDeviceDomain(): BluetoothDeviceDomain{
    return BluetoothDeviceDomain(
        name = name,
        address = address,
    )
}



