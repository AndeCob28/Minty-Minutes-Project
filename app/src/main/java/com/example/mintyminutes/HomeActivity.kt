package com.example.mintyminutes

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView

class HomeActivity : AppCompatActivity(), HomeContract.View {

    private lateinit var presenter: HomeContract.Presenter
    private lateinit var firebaseManager: FirebaseManager

    // UI components
    private lateinit var welcomeText: TextView
    private lateinit var subtitleText: TextView
    private lateinit var menuIcon: ImageView
    private lateinit var progressCircle: ImageView
    private lateinit var progressCountText: TextView
    private lateinit var morningDot: ImageView
    private lateinit var afternoonDot: ImageView
    private lateinit var eveningDot: ImageView
    private lateinit var deviceStatusCard: LinearLayout
    private lateinit var deviceStatusText: TextView
    private lateinit var wifiIcon: ImageView
    private lateinit var eventsLogText: TextView
    private lateinit var clearLogBtn: Button
    private lateinit var homeBtn: ImageButton
    private lateinit var calendarBtn: ImageButton
    private lateinit var notificationBtn: ImageButton
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        firebaseManager = FirebaseManager()
        val model = HomeModel(this)
        presenter = HomePresenter(this, model, firebaseManager)

        initializeViews()
        setupClickListeners()
        setupSidebarMenu()
        updateBottomNavSelection(homeBtn)

        presenter.onViewCreated()
    }

    override fun onResume() {
        super.onResume()
        presenter.onResume()
    }

    override fun onPause() {
        super.onPause()
        (presenter as? HomePresenter)?.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDestroy()
    }

    private fun initializeViews() {
        welcomeText = findViewById(R.id.welcomeText)
        subtitleText = findViewById(R.id.subtitleText)
        progressCircle = findViewById(R.id.progressCircle)
        progressCountText = findViewById(R.id.progressCountText)
        morningDot = findViewById(R.id.morningDot)
        afternoonDot = findViewById(R.id.afternoonDot)
        eveningDot = findViewById(R.id.eveningDot)
        deviceStatusCard = findViewById(R.id.deviceStatusCard)
        deviceStatusText = findViewById(R.id.deviceStatusText)
        wifiIcon = findViewById(R.id.wifiIcon)
        eventsLogText = findViewById(R.id.eventsLogText)
        clearLogBtn = findViewById(R.id.clearLogBtn)
        homeBtn = findViewById(R.id.homeBtn)
        calendarBtn = findViewById(R.id.calendarBtn)
        notificationBtn = findViewById(R.id.notificationBtn)
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        menuIcon = findViewById(R.id.menuIcon)

        subtitleText.text = "I'm glad to see you back"
    }

    private fun setupClickListeners() {
        menuIcon.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        homeBtn.setOnClickListener {
            updateBottomNavSelection(homeBtn)
            presenter.onResume()
        }

        calendarBtn.setOnClickListener {
            updateBottomNavSelection(calendarBtn)
            navigateToSchedule()
        }

        notificationBtn.setOnClickListener {
            updateBottomNavSelection(notificationBtn)
            navigateToNotifications()
        }

        deviceStatusCard.setOnClickListener {
            presenter.onDeviceStatusClicked()
        }

        clearLogBtn.setOnClickListener {
            presenter.onClearLogClicked()
        }
    }

    private fun setupSidebarMenu() {
        navigationView.setNavigationItemSelectedListener { menuItem ->
            presenter.onMenuItemSelected(menuItem.itemId)
            true
        }
    }

    // ==================== MVP VIEW IMPLEMENTATIONS ====================

    override fun showProgress(current: Int, total: Int) {
        progressCountText.text = "$current/$total"

        val progressDrawable = when (current) {
            0 -> R.drawable.circle_progress_0
            1 -> R.drawable.circle_progress_1
            2 -> R.drawable.circle_progress_2
            3 -> R.drawable.circle_progress_3
            else -> R.drawable.circle_progress_0
        }
        progressCircle.setImageResource(progressDrawable)
    }

    override fun updateSessionDots(morning: Boolean, afternoon: Boolean, evening: Boolean) {
        val completedDrawable = ContextCompat.getDrawable(this, R.drawable.circle_filled)
        val emptyDrawable = ContextCompat.getDrawable(this, R.drawable.circle_empty)

        morningDot.setImageDrawable(if (morning) completedDrawable else emptyDrawable)
        afternoonDot.setImageDrawable(if (afternoon) completedDrawable else emptyDrawable)
        eveningDot.setImageDrawable(if (evening) completedDrawable else emptyDrawable)
    }

    override fun showDeviceStatus(connected: Boolean) {
        if (connected) {
            // Device Connected - Green background
            deviceStatusCard.setBackgroundResource(R.drawable.rounded_rectangle)
            deviceStatusCard.backgroundTintList =
                ContextCompat.getColorStateList(this, R.color.green_400)

            deviceStatusText.text = "Device Connected"
            deviceStatusText.setTextColor(ContextCompat.getColor(this, android.R.color.white))

            wifiIcon.setImageResource(R.drawable.ic_wifi)
            wifiIcon.imageTintList = ContextCompat.getColorStateList(this, android.R.color.white)

            // Show LIVE indicator in Events Log
            findViewById<LinearLayout>(R.id.liveIndicator)?.visibility = View.VISIBLE

        } else {
            // Device Disconnected - Red background
            deviceStatusCard.setBackgroundResource(R.drawable.rounded_rectangle)
            deviceStatusCard.backgroundTintList =
                ContextCompat.getColorStateList(this, R.color.red_400)

            deviceStatusText.text = "Tap to Connect ESP32"
            deviceStatusText.setTextColor(ContextCompat.getColor(this, android.R.color.white))

            wifiIcon.setImageResource(R.drawable.ic_wifi_off)
            wifiIcon.imageTintList = ContextCompat.getColorStateList(this, android.R.color.white)

            // Hide LIVE indicator
            findViewById<LinearLayout>(R.id.liveIndicator)?.visibility = View.GONE
        }
    }

    override fun showSessionInProgress(inProgress: Boolean, duration: Int) {
        if (inProgress) {
            val minutes = duration / 60
            val seconds = duration % 60
            val timeText = String.format("%02d:%02d", minutes, seconds)
            subtitleText.text = "Session in progress: $timeText"
            subtitleText.setTextColor(ContextCompat.getColor(this, R.color.green_400))
        } else {
            subtitleText.text = "I'm glad to see you back"
            subtitleText.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
        }
    }

    override fun showConnectionDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Connect to ESP32 Device")

        val input = EditText(this)
        input.hint = "Enter ESP32 IP Address (e.g., 192.168.1.100)"
        input.setText("192.168.1.100") // Default IP
        builder.setView(input)

        builder.setPositiveButton("Connect") { dialog, _ ->
            val ipAddress = input.text.toString()
            if (ipAddress.isNotEmpty()) {
                presenter.connectToESP32(ipAddress)
            } else {
                showToast("Please enter IP address")
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    override fun showConnectionOptionsDialog() {
        if (presenter is HomePresenter && (presenter as HomePresenter).isDeviceConnected()) {
            // Already connected - show disconnect option
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Device Connected")
            builder.setMessage("Do you want to disconnect from the ESP32?")
            builder.setPositiveButton("Disconnect") { dialog, _ ->
                presenter.disconnectFromESP32()
                dialog.dismiss()
            }
            builder.setNegativeButton("Cancel", null)
            builder.show()
        } else {
            // Not connected - show connection options
            val options = arrayOf("Auto-Discover Device", "Enter IP Manually")
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Connect to ESP32")
            builder.setItems(options) { dialog, which ->
                when (which) {
                    0 -> startAutoDiscovery()
                    1 -> showConnectionDialog()
                }
                dialog.dismiss()
            }
            builder.setNegativeButton("Cancel", null)
            builder.show()
        }
    }

    override fun addEventLog(event: String) {
        val currentLog = eventsLogText.text.toString()

        if (currentLog == "Waiting for device events...") {
            eventsLogText.text = event
        } else {
            eventsLogText.append("\n$event")
        }

        eventsLogText.post {
            val scrollView = eventsLogText.parent.parent as? androidx.core.widget.NestedScrollView
            scrollView?.fullScroll(View.FOCUS_DOWN)
        }
    }

    override fun clearEventLog() {
        eventsLogText.text = "Waiting for device events..."
    }

    override fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun updateWelcomeText(name: String) {
        welcomeText.text = name
    }

    override fun updateReminderText(progress: Int) {
        // Reminder text removed from compressed layout
    }

    override fun showLoading(show: Boolean) {
        // No longer using addBtn
    }

    override fun navigateToSchedule() {
        val intent = Intent(this, ScheduleActivity::class.java)
        startActivity(intent)
    }

    override fun navigateToNotifications() {
        val intent = Intent(this, NotificationActivity::class.java)
        startActivity(intent)
    }

    override fun navigateToHistory() {
        try {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            showToast("Error opening history")
        }
    }

    override fun navigateToProfile() {
        val intent = Intent(this, ProfileActivity::class.java)
        startActivity(intent)
    }

    override fun navigateToSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    override fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun closeDrawer() {
        drawerLayout.closeDrawer(GravityCompat.START)
    }

    private fun updateBottomNavSelection(selectedButton: ImageButton) {
        val buttons = listOf(homeBtn, calendarBtn, notificationBtn)

        buttons.forEach { button ->
            val isSelected = button == selectedButton
            val backgroundRes = if (isSelected) {
                R.drawable.nav_button_selected
            } else {
                R.drawable.nav_button
            }

            button.setBackgroundResource(backgroundRes)

            val tintColor = ContextCompat.getColor(this, android.R.color.white)
            button.imageTintList = android.content.res.ColorStateList.valueOf(tintColor)
        }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    // ==================== AUTO DISCOVERY METHODS ====================

    private fun startAutoDiscovery() {
        val progressDialog = AlertDialog.Builder(this)
            .setTitle("Discovering Devices...")
            .setMessage("Scanning local network for ESP32 devices...")
            .setCancelable(false)
            .create()

        progressDialog.show()

        val discovery = ESP32Discovery(this)
        val foundDevices = mutableListOf<Pair<String, String>>()

        discovery.discoverDevices(object : ESP32Discovery.DiscoveryListener {
            override fun onDeviceFound(ipAddress: String, deviceId: String) {
                runOnUiThread {
                    foundDevices.add(Pair(ipAddress, deviceId))
                    progressDialog.setMessage("Found: $deviceId\nat $ipAddress\n\nSearching for more...")
                }
            }

            override fun onDiscoveryComplete(devicesFound: Int) {
                runOnUiThread {
                    progressDialog.dismiss()

                    if (foundDevices.isEmpty()) {
                        showToast("No ESP32 devices found")
                        // Fallback to manual entry
                        showConnectionDialog()
                    } else if (foundDevices.size == 1) {
                        // Only one device found, connect automatically
                        val device = foundDevices[0]
                        showToast("Connecting to ${device.second}...")
                        presenter.connectToESP32(device.first)
                    } else {
                        // Multiple devices found, let user choose
                        showDeviceSelectionDialog(foundDevices)
                    }
                }
            }

            override fun onError(error: String) {
                runOnUiThread {
                    progressDialog.dismiss()
                    showToast(error)
                    // Fallback to manual entry
                    showConnectionDialog()
                }
            }
        })
    }

    private fun showDeviceSelectionDialog(devices: List<Pair<String, String>>) {
        val deviceNames = devices.map { "${it.second}\n${it.first}" }.toTypedArray()

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select ESP32 Device")
        builder.setItems(deviceNames) { dialog, which ->
            val selectedDevice = devices[which]
            showToast("Connecting to ${selectedDevice.second}...")
            presenter.connectToESP32(selectedDevice.first)
            dialog.dismiss()
        }
        builder.setNegativeButton("Manual Entry") { _, _ ->
            showConnectionDialog()
        }
        builder.show()
    }
}