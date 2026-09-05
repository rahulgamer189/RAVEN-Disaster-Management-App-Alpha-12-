package com.raven.application.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice

data class BluetoothDeviceDomain(
    val name: String?,
    val address: String,
    val isPaired: Boolean = false
)

@SuppressLint("MissingPermission")
fun BluetoothDevice.toDomain(isPaired: Boolean = false): BluetoothDeviceDomain {
    return BluetoothDeviceDomain(
        name = name,
        address = address,
        isPaired = isPaired
    )
}
