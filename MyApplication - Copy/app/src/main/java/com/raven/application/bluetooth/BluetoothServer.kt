package com.raven.application.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

class BluetoothServer(
    private val adapter: BluetoothAdapter,
    private val uuid: String = "fa86c741-5e58-4538-adff-03e14a8b4e4e"
) {
    private var serverSocket: BluetoothServerSocket? = null

    @SuppressLint("MissingPermission")
    suspend fun listen(): BluetoothSocket? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("BluetoothServer", "Starting server socket listener with UUID: $uuid")
                // Best practice: cancel discovery before accepting connection
                if (adapter.isDiscovering) {
                    Log.d("BluetoothServer", "Discovery is active, canceling it before starting server")
                    adapter.cancelDiscovery()
                }
                
                serverSocket = adapter.listenUsingRfcommWithServiceRecord("Raven", UUID.fromString(uuid))
                Log.d("BluetoothServer", "Waiting for client connection...")
                val socket = serverSocket?.accept()
                Log.d("BluetoothServer", "Client connected: ${socket?.remoteDevice?.address}")
                socket
            } catch (e: IOException) {
                Log.e("BluetoothServer", "Error in server socket listener", e)
                null
            } finally {
                Log.d("BluetoothServer", "Closing server socket")
                serverSocket?.close()
            }
        }
    }

    fun stop() {
        try {
            serverSocket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
