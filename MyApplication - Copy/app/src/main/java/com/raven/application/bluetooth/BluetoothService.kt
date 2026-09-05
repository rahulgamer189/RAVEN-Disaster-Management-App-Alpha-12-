package com.raven.application.bluetooth

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.ParcelUuid
import android.util.Log
import com.raven.application.R
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class BluetoothService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val binder = BluetoothBinder()

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var telemetryProvider: TelemetryProvider? = null

    private var advertiser: BluetoothLeAdvertiser? = null
    private var scanner: BluetoothLeScanner? = null
    private val SERVICE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Standard Serial Port UUID

    private val connectionHandlers = ConcurrentHashMap<String, ConnectionHandler>()
    private val meshRouter = MeshRouter()

    private val _messages = MutableSharedFlow<BluetoothMessage>(extraBufferCapacity = 100)
    val messages: SharedFlow<BluetoothMessage> = _messages.asSharedFlow()

    private val _connectedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    val connectedDevices: StateFlow<List<BluetoothDeviceDomain>> = _connectedDevices.asStateFlow()

    private var serverJob: Job? = null

    inner class BluetoothBinder : Binder() {
        fun getService(): BluetoothService = this@BluetoothService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        telemetryProvider = TelemetryProvider(this)
        
        advertiser = bluetoothAdapter.bluetoothLeAdvertiser
        scanner = bluetoothAdapter.bluetoothLeScanner

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification(getString(R.string.msg_raven_active)))
        
        startServer()
        startHighPowerDiscovery()
    }

    private fun startHighPowerDiscovery() {
        // High Power BLE Advertising for maximum range discovery
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH) // MAX RANGE
            .setConnectable(true)
            .build()
        
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .addServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()

        advertiser?.startAdvertising(settings, data, object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                Log.d("BluetoothService", "High Power BLE Advertising started")
            }
        })

        // High Power BLE Scanning
        val filter = ScanFilter.Builder().setServiceUuid(ParcelUuid(SERVICE_UUID)).build()
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .build()
        
        scanner?.startScan(listOf(filter), scanSettings, object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                // Auto-initiate connection if a new node is discovered
                val address = result.device.address
                if (!connectionHandlers.containsKey(address)) {
                    Log.d("BluetoothService", "New node discovered via BLE: $address (RSSI: ${result.rssi})")
                    connectToDevice(address)
                }
            }
        })
    }

    private fun startServer() {
        serverJob?.cancel()
        serverJob = serviceScope.launch {
            val server = BluetoothServer(bluetoothAdapter)
            while (isActive) {
                val socket = server.listen()
                socket?.let {
                    handleConnection(it)
                }
                delay(1000) // Delay before next accept attempt if one fails or closes
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun handleConnection(socket: android.bluetooth.BluetoothSocket) {
        val handler = ConnectionHandler(socket)
        val address = handler.remoteDeviceAddress
        connectionHandlers[address] = handler
        
        updateConnectedDevices()

        serviceScope.launch {
            handler.listenForMessages()
                .onEach { message ->
                    processReceivedMessage(message, address)
                }
                .onCompletion {
                    connectionHandlers.remove(address)
                    updateConnectedDevices()
                    handler.close()
                }
                .collect()
        }
    }

    private suspend fun processReceivedMessage(message: BluetoothMessage, senderAddress: String) {
        val relayedMessage = meshRouter.forward(message)
        
        if (relayedMessage != null) {
            _messages.emit(message)

            // Always relay if forward returns a message (TTL > 0 and not seen before)
            relayMessage(relayedMessage, senderAddress)

            if (message.messageType == "SOS" || message.messageType == "BROADCAST") {
                val title = if (message.messageType == "SOS") getString(R.string.label_sos_emergency) else getString(R.string.label_incoming_broadcast)
                showNotification(title, "${message.senderName}: ${message.message}", message.messageType == "SOS")
            }
        }
    }

    private suspend fun relayMessage(message: BluetoothMessage, excludeAddress: String) {
        connectionHandlers.filter { it.key != excludeAddress }.forEach { (_, handler) ->
            handler.sendMessage(message)
        }
    }

    private fun updateConnectedDevices() {
        _connectedDevices.value = connectionHandlers.values.map { handler ->
            BluetoothDeviceDomain(
                name = handler.remoteDeviceName ?: "Unknown",
                address = handler.remoteDeviceAddress,
                isPaired = false // We don't necessarily know if it's paired here
            )
        }
    }

    fun connectToDevice(address: String) {
        serviceScope.launch {
            val client = BluetoothClient(bluetoothAdapter)
            val socket = client.connect(address)
            socket?.let {
                handleConnection(it)
            }
        }
    }

    fun sendMessage(text: String, type: String = "CHAT") {
        serviceScope.launch {
            val telemetry = if (type == "SOS" || type == "LOCATION") telemetryProvider?.getTelemetry(highAccuracy = true) else null
            val message = BluetoothMessage(
                senderName = bluetoothAdapter.name ?: getString(R.string.label_sender_me),
                message = text,
                messageType = type,
                latitude = telemetry?.latitude,
                longitude = telemetry?.longitude,
                batteryPercentage = telemetry?.batteryPercentage
            )
            
            meshRouter.accept(message)
            _messages.emit(message)
            
            connectionHandlers.forEach { (_, handler) ->
                handler.sendMessage(message)
            }
        }
    }

    fun shareLocation() {
        sendMessage(getString(R.string.msg_location_share), type = "LOCATION")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val meshChannel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.label_mesh_service),
                NotificationManager.IMPORTANCE_LOW
            )
            
            val sosChannel = NotificationChannel(
                SOS_CHANNEL_ID,
                getString(R.string.label_sos_alerts),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical SOS alerts from the mesh network"
                enableLights(true)
                lightColor = android.graphics.Color.RED
                enableVibration(true)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(meshChannel)
            manager.createNotificationChannel(sosChannel)
        }
    }

    private fun createNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .build()
    }

    private fun showNotification(title: String, content: String, isSos: Boolean = false) {
        val channelId = if (isSos) SOS_CHANNEL_ID else CHANNEL_ID
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(if (isSos) android.R.drawable.ic_dialog_alert else android.R.drawable.stat_notify_chat)
            .setAutoCancel(true)
            .setPriority(if (isSos) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_LOW)
            .apply {
                if (isSos) {
                    setCategory(NotificationCompat.CATEGORY_ALARM)
                    setDefaults(Notification.DEFAULT_ALL)
                }
            }
            .build()
        
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(if (isSos) SOS_NOTIFICATION_ID else System.currentTimeMillis().toInt(), notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        advertiser?.stopAdvertising(object : AdvertiseCallback() {})
        scanner?.stopScan(object : ScanCallback() {})
        serviceScope.cancel()
        connectionHandlers.values.forEach { it.close() }
        serverJob?.cancel()
    }

    companion object {
        private const val CHANNEL_ID = "mesh_service_channel"
        private const val SOS_CHANNEL_ID = "sos_service_channel"
        private const val NOTIFICATION_ID = 1
        private const val SOS_NOTIFICATION_ID = 911
    }
}
