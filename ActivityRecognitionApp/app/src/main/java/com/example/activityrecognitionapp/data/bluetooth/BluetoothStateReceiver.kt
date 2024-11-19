package com.example.activityrecognitionapp.data.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * A BroadcastReceiver that listens for Bluetooth state change events, specifically
 * connection and disconnection of Bluetooth devices. When a device connects or
 * disconnects, it triggers the provided callback with the new connection state
 * and the associated Bluetooth device.
 *
 * @property onStateChange A callback function that is invoked when the Bluetooth
 * connection state changes, providing the connection status and the Bluetooth device.
 */

class BluetoothStateReceiver(
    private val onBluetoothStateChanged: (isConnected: Boolean) -> Unit,
    private val onConnectionStateChanged: (isConnected: Boolean, BluetoothDevice) -> Unit
): BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                val isEnabled = state == BluetoothAdapter.STATE_ON
                onBluetoothStateChanged(isEnabled)
            }
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                } else {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                }
                if (device != null) {
                    onConnectionStateChanged(true, device)
                }
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                } else {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                }
                if (device != null) {
                    onConnectionStateChanged(false, device)
                }
            }
        }
    }

//    override fun onReceive(context: Context?, intent: Intent?) {
//        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            intent?.getParcelableExtra(
//                BluetoothDevice.EXTRA_DEVICE,
//                BluetoothDevice::class.java
//            )
//        } else {
//            intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
//        }
//        when (intent?.action) {
//            BluetoothDevice.ACTION_ACL_CONNECTED -> {
//                onStateChange(true, device ?: return)
//            }
//
//            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
//                onStateChange(false, device ?: return)
//            }
//        }
//    }
}

