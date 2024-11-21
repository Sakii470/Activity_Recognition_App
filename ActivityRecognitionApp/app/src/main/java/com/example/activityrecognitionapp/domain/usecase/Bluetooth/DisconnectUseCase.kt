package com.example.activityrecognitionapp.domain.usecase.Bluetooth

import com.example.activityrecognitionapp.domain.repository.BluetoothRepository
import javax.inject.Inject

/**
 * DisconnectUseCase handles disconnecting from the current Bluetooth device.
 */
class DisconnectUseCase @Inject constructor(
    private val repository: BluetoothRepository
) {
    operator fun invoke() {
        repository.disconnect()
    }
}