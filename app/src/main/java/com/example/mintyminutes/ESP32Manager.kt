package com.example.mintyminutes

import android.util.Log
import okhttp3.*
import okio.ByteString
import java.util.concurrent.TimeUnit

class ESP32Manager(private val listener: ESP32Listener) {

    private var webSocket: WebSocket? = null
    private var isConnected = false
    private var currentUserId: String = ""

    private val client = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    interface ESP32Listener {
        fun onConnected()
        fun onDisconnected()
        fun onToothbrushRemoved()
        fun onToothbrushReturned()
        fun onSessionComplete(duration: Int, valid: Boolean)
        fun onError(error: String)
    }

    fun connect(ipAddress: String, userId: String) {
        disconnect()
        currentUserId = userId

        val url = "ws://$ipAddress:81"
        val request = Request.Builder().url(url).build()

        Log.d(TAG, "Connecting to: $url")

        webSocket = client.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                Log.d(TAG, "✓ WebSocket connected!")

                // Send user ID to ESP32
                webSocket.send("USER_ID:$currentUserId")

                listener.onConnected()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "📩 Received: $text")
                handleMessage(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                Log.d(TAG, "📩 Received bytes: ${bytes.hex()}")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
                Log.d(TAG, "✗ WebSocket closed")
                listener.onDisconnected()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                Log.e(TAG, "✗ Connection failed: ${t.message}")
                listener.onError("Connection failed: ${t.message}")
                listener.onDisconnected()
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        isConnected = false
        currentUserId = ""
    }

    fun sendMessage(message: String) {
        if (isConnected) {
            webSocket?.send(message)
            Log.d(TAG, "📤 Sent: $message")
        }
    }

    private fun handleMessage(message: String) {
        when {
            message.startsWith("CONNECTED:") -> {
                val deviceId = message.substringAfter("CONNECTED:")
                Log.d(TAG, "Device ID: $deviceId")
            }

            message == "USER_ID_CONFIRMED" -> {
                Log.d(TAG, "✓ User ID confirmed by ESP32")
            }

            message == "TOOTHBRUSH_REMOVED" -> {
                listener.onToothbrushRemoved()
            }

            message == "TOOTHBRUSH_DETECTED" -> {
                listener.onToothbrushReturned()
            }

            message.startsWith("SESSION_COMPLETE:") -> {
                // Format: SESSION_COMPLETE:60:VALID or SESSION_COMPLETE:45:TOO_SHORT
                val parts = message.split(":")
                if (parts.size >= 3) {
                    val duration = parts[1].toIntOrNull() ?: 0
                    val valid = parts[2] == "VALID"
                    listener.onSessionComplete(duration, valid)
                }
            }

            message.startsWith("STATUS:") -> {
                val status = message.substringAfter("STATUS:")
                Log.d(TAG, "Device status: $status")
            }

            message == "PONG" -> {
                Log.d(TAG, "Ping response received")
            }

            else -> {
                Log.d(TAG, "Unknown message: $message")
            }
        }
    }

    fun isConnected(): Boolean = isConnected

    companion object {
        private const val TAG = "ESP32Manager"
    }
}