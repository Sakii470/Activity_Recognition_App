package com.example.activityrecognitionapp.data.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * A BroadcastReceiver that listens for Bluetooth state change events, specifically
 * the connection and disconnection of Bluetooth devices.
 *
 * When a device connects or disconnects, it triggers the corresponding callback
 * with the new connection state and the associated Bluetooth device.
 *
 * @property onBluetoothStateChanged Callback invoked when Bluetooth is enabled or disabled.
 * @property onConnectionStateChanged Callback invoked when a Bluetooth device connects or disconnects.
 */
class BluetoothStateReceiver(
    private val onBluetoothStateChanged: (isEnabled: Boolean) -> Unit,
    private val onConnectionStateChanged: (isConnected: Boolean, device: BluetoothDevice) -> Unit
) : BroadcastReceiver() {

    /**
     * Receives and handles Bluetooth-related intents.
     *
     * @param context The context in which the receiver is running.
     * @param intent The intent being received.
     */
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            // Handles changes in the overall Bluetooth adapter state (e.g., turned on/off)
            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                val isEnabled = state == BluetoothAdapter.STATE_ON
                onBluetoothStateChanged(isEnabled)
            }
            // Handles the event when a Bluetooth device is connected
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                val device: BluetoothDevice? = getBluetoothDeviceFromIntent(intent)
                device?.let {
                    onConnectionStateChanged(true, it)
                }
            }
            // Handles the event when a Bluetooth device is disconnected
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                val device: BluetoothDevice? = getBluetoothDeviceFromIntent(intent)
                device?.let {
                    onConnectionStateChanged(false, it)
                }
            }
        }
    }

    /**
     * Retrieves the BluetoothDevice from the intent, handling different API levels.
     *
     * @param intent The intent containing the BluetoothDevice information.
     * @return The [BluetoothDevice] if available, or `null` otherwise.
     */
    private fun getBluetoothDeviceFromIntent(intent: Intent): BluetoothDevice? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
    }
}
