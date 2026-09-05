package com.raven.application.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

class BluetoothClient(
    private val adapter: BluetoothAdapter,
    private val uuid: String = "fa86c741-5e58-4538-adff-03e14a8b4e4e"
) {
    @SuppressLint("MissingPermission")
    suspend fun connect(address: String): BluetoothSocket? {
        return withContext(Dispatchers.IO) {
            Log.d("BluetoothClient", "Attempting to connect to $address with UUID: $uuid")
            val device = adapter.getRemoteDevice(address)
            val socket = device.createRfcommSocketToServiceRecord(UUID.fromString(uuid))
            try {
                if (adapter.isDiscovering) {
                    Log.d("BluetoothClient", "Discovery is active, canceling it")
                    adapter.cancelDiscovery()
                }
                Log.d("BluetoothClient", "Connecting socket...")
                socket.connect()
                Log.d("BluetoothClient", "Connection successful")
                socket
            } catch (e: IOException) {
                Log.e("BluetoothClient", "Connection failed", e)
                try {
                    socket.close()
                } catch (closeException: IOException) {
                    Log.e("BluetoothClient", "Error closing socket after failure", closeException)
                }
                null
            }
        }
    }
}
