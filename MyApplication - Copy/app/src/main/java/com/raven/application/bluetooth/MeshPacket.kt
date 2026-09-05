package com.raven.application.bluetooth

import java.security.MessageDigest

class MeshRouter {
    private val seen = HashSet<String>()
    
    fun accept(message: BluetoothMessage): Boolean {
        return seen.add(message.id) && message.ttl > 0
    }
    
    fun forward(message: BluetoothMessage): BluetoothMessage? {
        return if (accept(message)) message.relay() else null
    }
    
    companion object {
        fun id(sender: String, body: String): String = 
            MessageDigest.getInstance("SHA-256").digest("$sender:$body".toByteArray())
                .joinToString("") { "%02x".format(it) }
    }
}
