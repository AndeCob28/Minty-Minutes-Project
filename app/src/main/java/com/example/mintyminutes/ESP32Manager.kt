package com.example.mintyminutes

import android.util.Log
import okhttp3.*
import okio.ByteString
import java.util.concurrent.TimeUnit

class ESP32Manager(private val listener: ESP32Listener) {

    private var webSocket: WebSocket? = null
    private var isConnected = false
    private val client = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)  // Keep connection alive
        .build()

    interface ESP32Listener {
        fun onConnected()
        fun onDisconnected()
        fun onToothbrushRemoved()
        fun onToothbrushReturned()
        fun onBatteryUpdate(level: Int)
        fun onError(error: String)
        fun onProgressUpdate(current: Int, total: Int)
        fun onSessionDotsUpdate(morning: Boolean, afternoon: Boolean, evening: Boolean)
        fun onEventLog(event: String)
    }

    fun connect(ipAddress: String) {
        disconnect() // Close any existing connection

        val url = "ws://$ipAddress:81"  // ESP32 WebSocket port 81
        val request = Request.Builder()
            .url(url)
            .build()

        Log.d("ESP32Manager", "Connecting to: $url")

        webSocket = client.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                Log.d("ESP32Manager", "✓ WebSocket connected!")
                listener.onConnected()

                // Request initial status
                webSocket.send("GET_STATUS")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("ESP32Manager", "📩 Received: $text")
                handleMessage(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                Log.d("ESP32Manager", "📩 Received bytes: ${bytes.hex()}")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("ESP32Manager", "Closing: $code / $reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
                Log.d("ESP32Manager", "✗ WebSocket closed: $code / $reason")
                listener.onDisconnected()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                Log.e("ESP32Manager", "✗ Connection failed: ${t.message}")
                listener.onError("Connection failed: ${t.message}")
                listener.onDisconnected()
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        isConnected = false
    }

    fun sendMessage(message: String) {
        if (isConnected) {
            webSocket?.send(message)
            Log.d("ESP32Manager", "📤 Sent: $message")
        } else {
            Log.w("ESP32Manager", "Cannot send - not connected")
        }
    }

    private fun handleMessage(message: String) {
        when {
            message.startsWith("CONNECTED:") -> {
                val deviceId = message.substringAfter("CONNECTED:")
                Log.d("ESP32Manager", "Device ID: $deviceId")
            }

            message == "TOOTHBRUSH_REMOVED" -> {
                listener.onToothbrushRemoved()
            }

            message == "TOOTHBRUSH_DETECTED" -> {
                listener.onToothbrushReturned()
            }

            message.startsWith("SESSION_VALID:") -> {
                // Format: SESSION_VALID:seconds:count
                val parts = message.split(":")
                if (parts.size >= 3) {
                    val seconds = parts[1].toIntOrNull() ?: 0
                    val count = parts[2].toIntOrNull() ?: 0
                    Log.d("ESP32Manager", "✓ Valid session: ${seconds}s, Total: $count/3")
                    listener.onProgressUpdate(count, 3)
                }
            }

            message.startsWith("SESSION_TOO_SHORT:") -> {
                // Format: SESSION_TOO_SHORT:seconds:required
                val parts = message.split(":")
                if (parts.size >= 3) {
                    val seconds = parts[1].toIntOrNull() ?: 0
                    val required = parts[2].toIntOrNull() ?: 60
                    Log.d("ESP32Manager", "✗ Session too short: ${seconds}s < ${required}s")
                    listener.onEventLog("Session too short (${seconds}s / ${required}s required)")
                }
            }

            message.startsWith("PROGRESS:") -> {
                // Format: PROGRESS:current:total
                val parts = message.split(":")
                if (parts.size >= 3) {
                    val current = parts[1].toIntOrNull() ?: 0
                    val total = parts[2].toIntOrNull() ?: 3
                    Log.d("ESP32Manager", "Progress update: $current/$total")
                    listener.onProgressUpdate(current, total)
                }
            }

            message.startsWith("DOTS:") -> {
                // Format: DOTS:morning:afternoon:evening (0 or 1)
                val parts = message.split(":")
                if (parts.size >= 4) {
                    val morning = parts[1] == "1" || parts[1] == "true"
                    val afternoon = parts[2] == "1" || parts[2] == "true"
                    val evening = parts[3] == "1" || parts[3] == "true"
                    Log.d("ESP32Manager", "Session dots: AM=$morning NN=$afternoon PM=$evening")
                    listener.onSessionDotsUpdate(morning, afternoon, evening)
                }
            }

            message.startsWith("STATUS:") -> {
                val status = message.substringAfter("STATUS:")
                Log.d("ESP32Manager", "Device status: $status")
            }

            message.startsWith("EVENT:") -> {
                val event = message.substringAfter("EVENT:")
                Log.d("ESP32Manager", "Event: $event")
                listener.onEventLog(event)
            }

            message.startsWith("FEEDBACK:") -> {
                val feedback = message.substringAfter("FEEDBACK:")
                Log.d("ESP32Manager", "Feedback: $feedback")
                if (feedback == "SUCCESS") {
                    listener.onEventLog("✓ Session completed successfully!")
                } else if (feedback == "TOO_SHORT") {
                    listener.onEventLog("✗ Session too short, try again")
                }
            }

            message == "SESSION_LIMIT_REACHED" -> {
                Log.d("ESP32Manager", "Daily limit reached (3/3 sessions)")
                listener.onEventLog("Daily limit reached (3/3 sessions)")
            }

            message == "NEW_DAY" -> {
                Log.d("ESP32Manager", "New day - progress reset")
                listener.onEventLog("New day detected - progress reset to 0/3")
                listener.onProgressUpdate(0, 3)
                listener.onSessionDotsUpdate(false, false, false)
            }

            message == "PONG" -> {
                Log.d("ESP32Manager", "Ping response received")
            }

            else -> {
                Log.d("ESP32Manager", "Unknown message: $message")
            }
        }
    }

    fun isConnected(): Boolean = isConnected
}