package com.example.activityrecognitionapp.data.bluetooth

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * A BroadcastReceiver that listens for Bluetooth device discovery events.
 * When a device is found, it invokes the provided callback with the discovered
 * Bluetooth device. This class handles the ACTION_FOUND broadcast and processes
 * the found devices accordingly.
 *
 * @property onDeviceFound A callback function that is called with the found Bluetooth device.
 */
class FoundDeviceReceiver(
    private val onDeviceFound: (BluetoothDevice) -> Unit
) : BroadcastReceiver() {

    /**
     * Called when the BroadcastReceiver receives an Intent broadcast.
     *
     * @param context The Context in which the receiver is running.
     * @param intent The Intent being received.
     */
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            // Triggered when a new Bluetooth device is discovered during scanning
            BluetoothDevice.ACTION_FOUND -> {
                // Retrieve the BluetoothDevice object from the Intent extras
                val device: BluetoothDevice? = getBluetoothDeviceFromIntent(intent)

                // If a device is found, invoke the callback with the device
                device?.let(onDeviceFound)
            }
        }
    }

    /**
     * Extracts the BluetoothDevice from the received Intent, handling different API levels.
     *
     * @param intent The Intent containing the BluetoothDevice information.
     * @return The [BluetoothDevice] if available, or `null` otherwise.
     */
    private fun getBluetoothDeviceFromIntent(intent: Intent): BluetoothDevice? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android Tiramisu and above, specify the BluetoothDevice class type
            intent.getParcelableExtra(
                BluetoothDevice.EXTRA_DEVICE,
                BluetoothDevice::class.java
            )
        } else {
            // For older Android versions, use the deprecated method
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
    }
}
