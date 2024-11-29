package com.example.activityrecognitionapp.domain.usecases.Bluetooth

import com.example.activityrecognitionapp.domain.repository.BluetoothRepository
import javax.inject.Inject

/**
 * StopScanUseCase stops scanning for Bluetooth devices.
 */
class StopScanUseCase @Inject constructor(
    private val repository: BluetoothRepository
) {
    operator fun invoke() {
        repository.stopScan()
    }
}