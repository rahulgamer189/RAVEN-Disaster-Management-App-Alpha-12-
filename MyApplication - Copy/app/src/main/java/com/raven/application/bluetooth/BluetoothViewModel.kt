package com.raven.application.bluetooth

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.raven.application.R
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.InputStream
import java.io.InputStreamReader

@SuppressLint("MissingPermission")
class BluetoothViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val context: Context = application.applicationContext
    private val bluetoothManager: BluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    private val _scannedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    val scannedDevices: StateFlow<List<BluetoothDeviceDomain>> = _scannedDevices.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDeviceDomain>> = _pairedDevices.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _connectedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    val connectedDevices: StateFlow<List<BluetoothDeviceDomain>> = _connectedDevices.asStateFlow()

    private val _selectedDevice = MutableStateFlow<BluetoothDeviceDomain?>(null)
    val selectedDevice: StateFlow<BluetoothDeviceDomain?> = _selectedDevice.asStateFlow()

    private val _messages = MutableStateFlow<List<BluetoothMessage>>(emptyList())
    val messages: StateFlow<List<BluetoothMessage>> = _messages.asStateFlow()

    private val _peerTelemetry = MutableStateFlow<Map<String, BluetoothMessage>>(emptyMap())
    val peerTelemetry: StateFlow<Map<String, BluetoothMessage>> = _peerTelemetry.asStateFlow()

    private val _camps = MutableStateFlow<List<Camp>>(emptyList())
    val camps: StateFlow<List<Camp>> = _camps.asStateFlow()

    private var bluetoothService: BluetoothService? = null
    private var isServiceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: android.content.ComponentName?, service: IBinder?) {
            val binder = service as BluetoothService.BluetoothBinder
            bluetoothService = binder.getService()
            isServiceBound = true
            
            // Observe messages from service
            bluetoothService?.messages?.onEach { message ->
                _messages.update { it + message }
                if (message.senderName != context.getString(R.string.label_sender_me)) {
                    _peerTelemetry.update { it + (message.senderName to message) }
                }
                
                if (message.messageType == "CAMP_UPSERT") {
                    val parts = message.message.split("|")
                    if (parts.size >= 3) {
                        try {
                            val camp = Camp(
                                name = parts[0],
                                latitude = parts[1].toDouble(),
                                longitude = parts[2].toDouble(),
                                version = parts.getOrNull(3)?.split("=")?.getOrNull(1)?.toLong() ?: System.currentTimeMillis()
                            )
                            _camps.update { current ->
                                val existing = current.find { it.name == camp.name }
                                if (existing == null || existing.version < camp.version) {
                                    (current.filter { it.name != camp.name } + camp).sortedBy { it.name }
                                } else current
                            }
                        } catch (e: Exception) {
                            Log.e("BluetoothViewModel", "Failed to parse camp message: ${message.message}", e)
                        }
                    }
                }
            }?.launchIn(viewModelScope)

            // Observe connected devices from service
            bluetoothService?.connectedDevices?.onEach { devices ->
                _connectedDevices.value = devices
                _isConnected.value = devices.isNotEmpty()
            }?.launchIn(viewModelScope)
        }

        override fun onServiceDisconnected(name: android.content.ComponentName?) {
            bluetoothService = null
            isServiceBound = false
        }
    }

    private val deviceFoundReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when(intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        val domainDevice = it.toDomain()
                        Log.d("BluetoothViewModel", "Device found: ${domainDevice.name} (${domainDevice.address})")
                        _scannedDevices.update { devices ->
                            if (devices.any { d -> d.address == domainDevice.address }) devices else devices + domainDevice
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    Log.d("BluetoothViewModel", "Discovery started")
                    _isScanning.value = true
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Log.d("BluetoothViewModel", "Discovery finished")
                    _isScanning.value = false
                }
            }
        }
    }

    init {
        updatePairedDevices()
        loadMockTeammates()
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        context.registerReceiver(deviceFoundReceiver, filter)
        
        // Start and bind to service
        val intent = Intent(context, BluetoothService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun startScanning() {
        Log.d("BluetoothViewModel", "Requesting start scanning")
        _scannedDevices.value = emptyList()
        val started = bluetoothAdapter?.startDiscovery() ?: false
        if (!started) {
            Log.e("BluetoothViewModel", "Failed to start discovery")
            _isScanning.value = false
        }
    }

    fun stopScanning() {
        Log.d("BluetoothViewModel", "Requesting stop scanning")
        bluetoothAdapter?.cancelDiscovery()
        _isScanning.value = false
    }

    fun updatePairedDevices() {
        val devices = bluetoothAdapter?.bondedDevices?.map { it.toDomain(isPaired = true) } ?: emptyList()
        _pairedDevices.value = devices
    }

    fun startServer() {
        // Server is now managed by the service
    }

    fun connectToDevice(device: BluetoothDeviceDomain) {
        Log.d("BluetoothViewModel", "Connecting to device via service: ${device.address}")
        _selectedDevice.value = device
        bluetoothService?.connectToDevice(device.address)
    }

    fun disconnect() {
        // For multi-peer, we might want to disconnect a specific device or all.
        _messages.value = emptyList()
    }

    fun sendMessage(text: String, type: String = "CHAT") {
        if (text.isBlank()) return
        bluetoothService?.sendMessage(text, type)
    }

    fun shareLocation() {
        bluetoothService?.shareLocation()
    }

    fun saveCamp(name: String, lat: Double, lon: Double) {
        val messageBody = "$name|$lat|$lon|version=${System.currentTimeMillis()}"
        sendMessage(messageBody, type = "CAMP_UPSERT")
        
        // Also update local state immediately
        val camp = Camp(name, lat, lon)
        _camps.update { current ->
            (current.filter { it.name != name } + camp).sortedBy { it.name }
        }
    }

    private fun loadMockTeammates() {
        try {
            val inputStream = context.assets.open("teammate_locations.json")
            val json = inputStream.bufferedReader().use { it.readText() }
            val mockMessages = Json.decodeFromString<List<BluetoothMessage>>(json)
            _peerTelemetry.update { current ->
                val newMap = current.toMutableMap()
                mockMessages.forEach { msg ->
                    newMap[msg.senderName] = msg
                }
                newMap
            }
        } catch (e: Exception) {
            Log.e("BluetoothViewModel", "Failed to load mock teammates", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("BluetoothViewModel", "onCleared - cleaning up")
        context.unregisterReceiver(deviceFoundReceiver)
        if (isServiceBound) {
            context.unbindService(serviceConnection)
            isServiceBound = false
        }
    }
}
