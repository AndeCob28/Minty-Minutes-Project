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
        fun onProgressUpdate(current: Int, total: Int)
        fun onSessionDotsUpdate(morning: Boolean, afternoon: Boolean, evening: Boolean)
        fun onEventLog(event: String)
        fun onError(error: String)
    }

    fun connect(ipAddress: String) {
        disconnect()

        // Get current user ID from Firebase Auth
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.e(TAG, "Cannot connect: No user logged in")
            listener.onError("Please login first")
            return
        }
        currentUserId = userId

        val url = "ws://$ipAddress:81"
        val request = Request.Builder()
            .url(url)
            .build()

        Log.d(TAG, "========================================")
        Log.d(TAG, "Connecting to: $url")
        Log.d(TAG, "User ID: $currentUserId")
        Log.d(TAG, "========================================")

        webSocket = client.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                Log.d(TAG, "✓ WebSocket OPENED successfully!")
                Log.d(TAG, "Response: ${response.message}")

                // Send user ID to ESP32
                webSocket.send("USER_ID:$currentUserId")
                Log.d(TAG, "📤 Sent USER_ID: $currentUserId")

                // Request current status
                webSocket.send("GET_STATUS")

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
                Log.d(TAG, "Closing: $code / $reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
                Log.d(TAG, "✗ WebSocket closed: $code / $reason")
                listener.onDisconnected()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                Log.e(TAG, "========================================")
                Log.e(TAG, "✗✗✗ CONNECTION FAILED ✗✗✗")
                Log.e(TAG, "URL: $url")
                Log.e(TAG, "Error: ${t.message}")
                Log.e(TAG, "Error type: ${t.javaClass.simpleName}")
                Log.e(TAG, "Response: ${response?.message ?: "null"}")
                Log.e(TAG, "Stack trace:", t)
                Log.e(TAG, "========================================")

                val errorMsg = when {
                    t.message?.contains("failed to connect", ignoreCase = true) == true ->
                        "Cannot reach ESP32 at $ipAddress. Check:\n1. ESP32 is powered on\n2. Both on same WiFi\n3. IP address is correct"
                    t.message?.contains("timeout", ignoreCase = true) == true ->
                        "Connection timeout. ESP32 not responding."
                    else -> "Connection failed: ${t.message}"
                }

                listener.onError(errorMsg)
                listener.onDisconnected()
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        isConnected = false
        currentUserId = ""
        Log.d(TAG, "Disconnected")
    }

    fun sendMessage(message: String) {
        if (isConnected) {
            webSocket?.send(message)
            Log.d(TAG, "📤 Sent: $message")
        } else {
            Log.w(TAG, "Cannot send - not connected")
        }
    }

    private fun handleMessage(message: String) {
        when {
            message.startsWith("CONNECTED:") -> {
                val deviceId = message.substringAfter("CONNECTED:")
                Log.d(TAG, "✓ Device ID: $deviceId")
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

            message.startsWith("SESSION_VALID:") -> {
                // Format: SESSION_VALID:60:1 (duration:count)
                val parts = message.split(":")
                if (parts.size >= 3) {
                    val duration = parts[1].toIntOrNull() ?: 0
                    val count = parts[2].toIntOrNull() ?: 0
                    Log.d(TAG, "✓ Valid session: ${duration}s, Total: $count/3")
                    listener.onSessionComplete(duration, true)
                    listener.onProgressUpdate(count, 3)
                }
            }

            message.startsWith("SESSION_TOO_SHORT:") -> {
                // Format: SESSION_TOO_SHORT:45:60 (duration:required)
                val parts = message.split(":")
                if (parts.size >= 3) {
                    val duration = parts[1].toIntOrNull() ?: 0
                    val required = parts[2].toIntOrNull() ?: 60
                    Log.d(TAG, "✗ Session too short: ${duration}s < ${required}s")
                    listener.onSessionComplete(duration, false)
                }
            }

            message.startsWith("PROGRESS:") -> {
                // Format: PROGRESS:1:3 (current:total)
                val parts = message.split(":")
                if (parts.size >= 3) {
                    val current = parts[1].toIntOrNull() ?: 0
                    val total = parts[2].toIntOrNull() ?: 3
                    Log.d(TAG, "Progress: $current/$total")
                    listener.onProgressUpdate(current, total)
                }
            }

            message.startsWith("DOTS:") -> {
                // Format: DOTS:1:0:0 (morning:afternoon:evening)
                val parts = message.split(":")
                if (parts.size >= 4) {
                    val morning = parts[1] == "1" || parts[1] == "true"
                    val afternoon = parts[2] == "1" || parts[2] == "true"
                    val evening = parts[3] == "1" || parts[3] == "true"
                    Log.d(TAG, "Session dots: AM=$morning NN=$afternoon PM=$evening")
                    listener.onSessionDotsUpdate(morning, afternoon, evening)
                }
            }

            message.startsWith("EVENT:") -> {
                val event = message.substringAfter("EVENT:")
                Log.d(TAG, "Event: $event")
                listener.onEventLog(event)
            }

            message.startsWith("STATUS:") -> {
                val status = message.substringAfter("STATUS:")
                Log.d(TAG, "Device status: $status")
            }

            message == "SESSION_LIMIT_REACHED" -> {
                Log.d(TAG, "Daily limit reached (3/3 sessions)")
                listener.onEventLog("Daily limit reached (3/3 sessions)")
            }

            message == "NEW_DAY" -> {
                Log.d(TAG, "New day - progress reset")
                listener.onEventLog("New day detected - progress reset to 0/3")
                listener.onProgressUpdate(0, 3)
                listener.onSessionDotsUpdate(false, false, false)
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