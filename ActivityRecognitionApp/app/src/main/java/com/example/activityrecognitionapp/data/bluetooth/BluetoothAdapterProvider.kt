package com.example.activityrecognitionapp.data.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides access to the device's [BluetoothAdapter] using the system's [BluetoothManager].
 *
 * This singleton class is responsible for retrieving the [BluetoothAdapter],
 * which is essential for performing Bluetooth operations such as scanning and connecting to devices.
 *
 * @property context The application context used to access system services.
 */
@Singleton
class BluetoothAdapterProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Retrieves the device's [BluetoothAdapter].
     *
     * @return The [BluetoothAdapter] if available, or `null` if Bluetooth is not supported on the device.
     */
    fun getBluetoothAdapter(): BluetoothAdapter? {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return bluetoothManager?.adapter
    }
}
