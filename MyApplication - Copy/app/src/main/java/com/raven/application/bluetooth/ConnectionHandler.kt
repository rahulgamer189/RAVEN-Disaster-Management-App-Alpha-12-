package com.raven.application.bluetooth

import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class ConnectionHandler(
    private val socket: BluetoothSocket
) {
    val remoteDeviceAddress: String = socket.remoteDevice.address
    val remoteDeviceName: String? = try { socket.remoteDevice.name } catch (e: SecurityException) { null }
    
    private val inputStream: InputStream = socket.inputStream
    private val outputStream: OutputStream = socket.outputStream

    fun listenForMessages(): Flow<BluetoothMessage> = flow {
        if (!socket.isConnected) {
            Log.w("ConnectionHandler", "Socket is not connected, stopping listener")
            return@flow
        }
        
        Log.d("ConnectionHandler", "Starting message listener")
        val reader = inputStream.bufferedReader()
        while (true) {
            try {
                val line = withContext(Dispatchers.IO) {
                    reader.readLine()
                } ?: break
                
                Log.d("ConnectionHandler", "Received line: $line")
                try {
                    val message = Json.decodeFromString<BluetoothMessage>(line)
                    emit(message)
                } catch (e: Exception) {
                    Log.e("ConnectionHandler", "Failed to decode message: $line", e)
                }
            } catch (e: IOException) {
                Log.e("ConnectionHandler", "Connection error or socket closed", e)
                break
            }
        }
        Log.d("ConnectionHandler", "Message listener stopped")
    }.flowOn(Dispatchers.IO)

    suspend fun sendMessage(message: BluetoothMessage): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val json = Json.encodeToString(message)
                Log.d("ConnectionHandler", "Sending message: $json")
                outputStream.write((json + "\n").toByteArray())
                outputStream.flush()
                Log.d("ConnectionHandler", "Message sent and flushed")
                true
            } catch (e: IOException) {
                Log.e("ConnectionHandler", "Failed to send message", e)
                false
            }
        }
    }

    fun close() {
        try {
            Log.d("ConnectionHandler", "Closing socket")
            socket.close()
        } catch (e: IOException) {
            Log.e("ConnectionHandler", "Error closing socket", e)
        }
    }
}
