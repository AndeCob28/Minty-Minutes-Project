package com.example.mintyminutes

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class HomePresenter(
    private val view: HomeContract.View,
    private val model: HomeModel,
    private val firebaseManager: FirebaseManager
) : HomeContract.Presenter, ESP32Manager.ESP32Listener {

    private val presenterScope = CoroutineScope(Dispatchers.Main + Job())
    private var currentProgress = 0
    private var isDeviceConnected = false

    // Session tracking
    private var sessionInProgress = false
    private var sessionStartTime: Long = 0
    private var sessionDuration: Int = 0
    private var sessionTimerJob: Job? = null

    // ESP32 Manager
    private val esp32Manager = ESP32Manager(this)
    private val MIN_SESSION_DURATION = 60 // 60 seconds = 1 minute

    override fun onViewCreated() {
        // Check if user is logged in
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            view.showToast("Please login again")
            view.navigateToLogin()
            return
        }

        // Load user data
        val userName = model.getUserName()
        view.updateWelcomeText("Hello, $userName!")

        // Load progress and device status
        loadTodayProgress()
        setupDeviceStatusListener()

        logEvent("App started")
    }

    override fun onResume() {
        loadTodayProgress()
    }

    override fun onPause() {
        // Keep ESP32 connection active even when paused
    }

    override fun onSessionAddClicked() {
        if (currentProgress >= 3) {
            view.showToast("You've completed all sessions today!")
            return
        }

        view.showLoading(true)

        presenterScope.launch {
            try {
                val sessionType = determineSessionType()

                val success = withContext(Dispatchers.IO) {
                    firebaseManager.saveBrushingSession(sessionType)
                }

                view.showLoading(false)

                if (success) {
                    currentProgress++
                    updateProgressDisplay()

                    val message = when (currentProgress) {
                        1 -> "Morning session saved! 🌅"
                        2 -> "Afternoon session saved! ☀️"
                        3 -> "Evening session saved! 🌙 All done for today!"
                        else -> "Session saved!"
                    }
                    view.showToast(message)
                    logEvent("Manual session completed: $sessionType")
                } else {
                    view.showToast("Failed to save session")
                }
            } catch (e: Exception) {
                view.showLoading(false)
                view.showToast("Error: ${e.message}")
                logEvent("Error saving session: ${e.message}")
            }
        }
    }

    override fun onDeviceStatusClicked() {
        if (!isDeviceConnected) {
            // Show auto-discovery options
            view.showConnectionOptionsDialog()
        } else {
            // Disconnect from ESP32
            disconnectFromESP32()
        }
    }

    override fun connectToESP32(ipAddress: String) {
        logEvent("Attempting to connect to ESP32: $ipAddress")
        esp32Manager.connect(ipAddress)
    }

    override fun disconnectFromESP32() {
        esp32Manager.disconnect()
        isDeviceConnected = false
        view.showDeviceStatus(false)
        logEvent("Disconnected from ESP32")
    }

    override fun onClearLogClicked() {
        view.clearEventLog()
        logEvent("Event log cleared")
    }

    override fun onMenuItemSelected(itemId: Int) {
        when (itemId) {
            R.id.nav_home -> {
                view.closeDrawer()
                loadTodayProgress()
            }
            R.id.nav_history -> {
                view.navigateToHistory()
                view.closeDrawer()
            }
            R.id.nav_settings -> {
                view.navigateToSettings()
                view.closeDrawer()
            }
            R.id.nav_profile -> {
                view.navigateToProfile()
                view.closeDrawer()
            }
            R.id.nav_logout -> {
                onLogoutClicked()
            }
        }
    }

    override fun onLogoutClicked() {
        disconnectFromESP32()
        model.clearSession()
        FirebaseAuth.getInstance().signOut()
        view.showToast("Logged out successfully")
        view.navigateToLogin()
    }

    override fun onDestroy() {
        sessionTimerJob?.cancel()
        esp32Manager.disconnect()
        presenterScope.cancel()
    }

    fun isDeviceConnected(): Boolean = isDeviceConnected

    // ==================== ESP32 Listener Methods ====================

    override fun onConnected() {
        isDeviceConnected = true
        view.showDeviceStatus(true)
        logEvent("ESP32 device connected successfully")
        view.showToast("Device connected!")

        // Update Firebase
        presenterScope.launch {
            withContext(Dispatchers.IO) {
                firebaseManager.updateDeviceStatus(true)
            }
        }

        // Refresh current progress
        loadTodayProgress()
    }

    override fun onDisconnected() {
        isDeviceConnected = false
        view.showDeviceStatus(false)
        logEvent("ESP32 device disconnected")

        // If session was in progress, end it
        if (sessionInProgress) {
            endBrushingSession(false)
        }

        // Update Firebase
        presenterScope.launch {
            withContext(Dispatchers.IO) {
                firebaseManager.updateDeviceStatus(false)
            }
        }
    }

    override fun onToothbrushRemoved() {
        logEvent("🟢 Toothbrush removed from holder")
        startBrushingSession()
    }

    override fun onToothbrushReturned() {
        logEvent("🔴 Toothbrush returned to holder")
        endBrushingSession(true)
    }

    override fun onBatteryUpdate(level: Int) {
        // Battery updates removed - no longer tracked
        logEvent("Battery update received: $level%")
    }

    override fun onError(error: String) {
        logEvent("❌ ESP32 Error: $error")
        view.showToast("Device error: $error")
    }

    override fun onProgressUpdate(current: Int, total: Int) {
        currentProgress = current
        view.showProgress(current, total)
        logEvent("Progress updated: $current/$total sessions")
    }

    override fun onSessionDotsUpdate(morning: Boolean, afternoon: Boolean, evening: Boolean) {
        view.updateSessionDots(morning, afternoon, evening)
        logEvent("Session dots updated: AM=$morning NN=$afternoon PM=$evening")
    }

    override fun onEventLog(event: String) {
        logEvent(event)
    }

    // ==================== Session Management ====================

    private fun startBrushingSession() {
        if (sessionInProgress) {
            logEvent("Session already in progress, ignoring")
            return
        }

        if (currentProgress >= 3) {
            view.showToast("All sessions completed for today!")
            logEvent("Cannot start session: Daily limit reached")
            return
        }

        sessionInProgress = true
        sessionStartTime = System.currentTimeMillis()
        sessionDuration = 0

        view.showSessionInProgress(true, 0)
        logEvent("Brushing session started")

        // Start timer
        sessionTimerJob = presenterScope.launch {
            while (sessionInProgress) {
                delay(1000) // Update every second
                sessionDuration = ((System.currentTimeMillis() - sessionStartTime) / 1000).toInt()
                view.showSessionInProgress(true, sessionDuration)
            }
        }
    }

    private fun endBrushingSession(validate: Boolean) {
        if (!sessionInProgress) {
            return
        }

        sessionInProgress = false
        sessionTimerJob?.cancel()

        val duration = ((System.currentTimeMillis() - sessionStartTime) / 1000).toInt()
        view.showSessionInProgress(false, 0)

        if (!validate) {
            logEvent("Session ended without validation")
            return
        }

        // Validate session duration
        if (duration >= MIN_SESSION_DURATION) {
            // Valid session - save it
            logEvent("Session completed: ${duration}s (VALID)")
            saveAutomaticSession(duration)
        } else {
            // Too short
            val message = "Session too short! (${duration}s / ${MIN_SESSION_DURATION}s required)"
            view.showToast(message)
            logEvent(message)
        }
    }

    private fun saveAutomaticSession(duration: Int) {
        presenterScope.launch {
            try {
                val sessionType = determineSessionType()

                val success = withContext(Dispatchers.IO) {
                    firebaseManager.saveBrushingSession(sessionType)
                }

                if (success) {
                    currentProgress++
                    updateProgressDisplay()

                    if (currentProgress >= 3) {
                        view.showToast("🎉 All 3 sessions completed today! Great job!")
                        logEvent("Daily goal achieved! 3/3 sessions completed")
                    } else {
                        val remaining = 3 - currentProgress
                        view.showToast("✅ Session saved! $remaining more to go today")
                        logEvent("Session auto-saved: $sessionType ($duration seconds)")
                    }
                } else {
                    view.showToast("Failed to save session")
                    logEvent("Error: Failed to save automatic session")
                }
            } catch (e: Exception) {
                view.showToast("Error saving session: ${e.message}")
                logEvent("Error: ${e.message}")
            }
        }
    }

    private fun determineSessionType(): String {
        return when (currentProgress) {
            0 -> SessionType.MORNING.value
            1 -> SessionType.AFTERNOON.value
            2 -> SessionType.EVENING.value
            else -> "general"
        }
    }

    // ==================== Data Loading ====================

    private fun loadTodayProgress() {
        firebaseManager.getTodayProgress { progress ->
            progress?.let {
                val completedSessions = it["completedSessions"] as? Int ?: 0
                currentProgress = completedSessions

                updateProgressDisplay()

                view.updateSessionDots(
                    morning = it["morningCompleted"] as? Boolean ?: false,
                    afternoon = it["afternoonCompleted"] as? Boolean ?: false,
                    evening = it["eveningCompleted"] as? Boolean ?: false
                )

                logEvent("Progress loaded: $completedSessions/3 sessions")
            } ?: run {
                currentProgress = 0
                updateProgressDisplay()
                view.updateSessionDots(false, false, false)
                logEvent("No progress data found for today")
            }
        }
    }

    private fun setupDeviceStatusListener() {
        firebaseManager.listenToDeviceStatus { status ->
            status?.let {
                val fbConnected = it["isConnected"] as? Boolean ?: false

                // Only update if not already connected via ESP32
                if (!isDeviceConnected && fbConnected) {
                    // Device connected from Firebase
                }
            }
        }
    }

    private fun updateProgressDisplay() {
        view.showProgress(currentProgress, 3)
        view.updateReminderText(currentProgress)
    }

    private fun logEvent(event: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        view.addEventLog("[$timestamp] $event")
    }
}