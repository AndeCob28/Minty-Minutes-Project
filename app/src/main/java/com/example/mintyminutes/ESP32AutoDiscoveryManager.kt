package com.example.mintyminutes

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class ESP32Discovery(private val context: Context) {

    private var discoveryJob: Job? = null
    private val DISCOVERY_PORT = 8888
    private val DISCOVERY_MESSAGE = "MINTY_DISCOVER"
    private val DISCOVERY_RESPONSE = "MINTY_ESP32"

    interface DiscoveryListener {
        fun onDeviceFound(ipAddress: String, deviceId: String)
        fun onDiscoveryComplete(devicesFound: Int)
        fun onError(error: String)
    }

    // Method 1: UDP Broadcast Discovery (Recommended)
    fun discoverDevices(listener: DiscoveryListener) {
        discoveryJob?.cancel()

        discoveryJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("ESP32Discovery", "🔍 Starting UDP discovery...")

                val socket = DatagramSocket()
                socket.broadcast = true
                socket.soTimeout = 3000  // 3 second timeout

                // Get broadcast address
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val dhcp = wifiManager.dhcpInfo
                val broadcast = getBroadcastAddress(dhcp.ipAddress, dhcp.netmask)

                // Send discovery broadcast
                val sendData = DISCOVERY_MESSAGE.toByteArray()
                val sendPacket = DatagramPacket(
                    sendData,
                    sendData.size,
                    broadcast,
                    DISCOVERY_PORT
                )

                socket.send(sendPacket)
                Log.d("ESP32Discovery", "📡 Broadcast sent to: ${broadcast.hostAddress}")

                // Listen for responses
                var devicesFound = 0
                val receiveData = ByteArray(1024)

                repeat(5) {  // Try to receive up to 5 responses
                    try {
                        val receivePacket = DatagramPacket(receiveData, receiveData.size)
                        socket.receive(receivePacket)

                        val response = String(receivePacket.data, 0, receivePacket.length)

                        if (response.startsWith(DISCOVERY_RESPONSE)) {
                            val deviceId = response.substringAfter(":")
                            val ipAddress = receivePacket.address.hostAddress ?: ""

                            withContext(Dispatchers.Main) {
                                listener.onDeviceFound(ipAddress, deviceId)
                            }

                            devicesFound++
                            Log.d("ESP32Discovery", "✓ Found device: $deviceId at $ipAddress")
                        }
                    } catch (e: Exception) {
                        // Timeout or no more responses
                    }
                }

                socket.close()

                withContext(Dispatchers.Main) {
                    listener.onDiscoveryComplete(devicesFound)
                }

                Log.d("ESP32Discovery", "Discovery complete. Found $devicesFound device(s)")

            } catch (e: Exception) {
                Log.e("ESP32Discovery", "Discovery error: ${e.message}")
                withContext(Dispatchers.Main) {
                    listener.onError("Discovery failed: ${e.message}")
                }
            }
        }
    }

    // Method 2: Scan local network (Slower but more reliable)
    fun scanLocalNetwork(listener: DiscoveryListener) {
        discoveryJob?.cancel()

        discoveryJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("ESP32Discovery", "🔍 Scanning local network...")

                // Get device's IP to determine network range
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val dhcp = wifiManager.dhcpInfo
                val deviceIp = intToIp(dhcp.ipAddress)

                // Extract network prefix (e.g., "192.168.1")
                val networkPrefix = deviceIp.substringBeforeLast(".")

                var devicesFound = 0

                // Scan all IPs in local network (1-254)
                val jobs = mutableListOf<Job>()

                for (i in 1..254) {
                    val job = launch {
                        val testIp = "$networkPrefix.$i"

                        try {
                            val address = InetAddress.getByName(testIp)

                            // Try to reach the host
                            if (address.isReachable(500)) {
                                // Try WebSocket handshake on port 81
                                if (testESP32Connection(testIp)) {
                                    withContext(Dispatchers.Main) {
                                        listener.onDeviceFound(testIp, "ESP32_$i")
                                    }
                                    devicesFound++
                                    Log.d("ESP32Discovery", "✓ Found ESP32 at: $testIp")
                                }
                            }
                        } catch (e: Exception) {
                            // Host not reachable, continue
                        }
                    }
                    jobs.add(job)

                    // Process in batches to avoid overwhelming the network
                    if (jobs.size >= 20) {
                        jobs.joinAll()
                        jobs.clear()
                    }
                }

                jobs.joinAll()

                withContext(Dispatchers.Main) {
                    listener.onDiscoveryComplete(devicesFound)
                }

                Log.d("ESP32Discovery", "Scan complete. Found $devicesFound device(s)")

            } catch (e: Exception) {
                Log.e("ESP32Discovery", "Scan error: ${e.message}")
                withContext(Dispatchers.Main) {
                    listener.onError("Scan failed: ${e.message}")
                }
            }
        }
    }

    fun stopDiscovery() {
        discoveryJob?.cancel()
        Log.d("ESP32Discovery", "Discovery stopped")
    }

    // Helper functions
    private fun getBroadcastAddress(ipAddress: Int, netmask: Int): InetAddress {
        val broadcast = ipAddress or netmask.inv()
        val bytes = byteArrayOf(
            (broadcast and 0xff).toByte(),
            (broadcast shr 8 and 0xff).toByte(),
            (broadcast shr 16 and 0xff).toByte(),
            (broadcast shr 24 and 0xff).toByte()
        )
        return InetAddress.getByAddress(bytes)
    }

    private fun intToIp(ip: Int): String {
        return String.format(
            "%d.%d.%d.%d",
            ip and 0xff,
            ip shr 8 and 0xff,
            ip shr 16 and 0xff,
            ip shr 24 and 0xff
        )
    }

    private fun testESP32Connection(ip: String): Boolean {
        return try {
            val socket = java.net.Socket()
            socket.connect(java.net.InetSocketAddress(ip, 81), 500)
            socket.close()
            true
        } catch (e: Exception) {
            false
        }
    }
}

// ===== UPDATE HomeActivity.kt - Add this method =====

/*
private fun showAutoDiscoveryDialog() {
    val builder = AlertDialog.Builder(this)
    builder.setTitle("Discovering ESP32 Devices...")
    builder.setMessage("Scanning local network...")

    val dialog = builder.create()
    dialog.show()

    val discovery = ESP32Discovery(this)
    val foundDevices = mutableListOf<Pair<String, String>>()

    discovery.discoverDevices(object : ESP32Discovery.DiscoveryListener {
        override fun onDeviceFound(ipAddress: String, deviceId: String) {
            foundDevices.add(Pair(ipAddress, deviceId))
            dialog.setMessage("Found: $deviceId\nat $ipAddress")
        }

        override fun onDiscoveryComplete(devicesFound: Int) {
            dialog.dismiss()

            if (foundDevices.isEmpty()) {
                showToast("No ESP32 devices found")
                // Fallback to manual entry
                showConnectionDialog()
            } else {
                // Show list of found devices
                showDeviceSelectionDialog(foundDevices)
            }
        }

        override fun onError(error: String) {
            dialog.dismiss()
            showToast(error)
            // Fallback to manual entry
            showConnectionDialog()
        }
    })
}

private fun showDeviceSelectionDialog(devices: List<Pair<String, String>>) {
    val deviceNames = devices.map { "${it.second} (${it.first})" }.toTypedArray()

    val builder = AlertDialog.Builder(this)
    builder.setTitle("Select ESP32 Device")
    builder.setItems(deviceNames) { dialog, which ->
        val selectedDevice = devices[which]
        presenter.connectToESP32(selectedDevice.first)
        dialog.dismiss()
    }
    builder.setNegativeButton("Cancel", null)
    builder.show()
}
*/