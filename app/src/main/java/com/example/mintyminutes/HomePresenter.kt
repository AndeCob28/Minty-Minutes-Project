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

    private var sessionInProgress = false
    private var sessionStartTime: Long = 0
    private var sessionTimerJob: Job? = null

    private val esp32Manager = ESP32Manager(this)
    private val MIN_SESSION_DURATION = 60

    override fun onViewCreated() {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            view.showToast("Please login again")
            view.navigateToLogin()
            return
        }

        val userName = model.getUserName()
        view.updateWelcomeText("Hello, $userName!")

        loadTodayProgress()
        logEvent("App started")
    }

    override fun onResume() {
        loadTodayProgress()
    }

    override fun onPause() {}

    override fun onSessionAddClicked() {
        // Manual session add removed - only automatic via ESP32
        view.showToast("Please use your smart toothbrush to start a session")
    }

    override fun onDeviceStatusClicked() {
        if (!isDeviceConnected) {
            view.showConnectionOptionsDialog()
        } else {
            disconnectFromESP32()
        }
    }

    override fun connectToESP32(ipAddress: String) {
        logEvent("Connecting to ESP32: $ipAddress")

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            view.showToast("Error: No user ID found")
            return
        }

        esp32Manager.connect(ipAddress, userId)
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

    // ========== ESP32 Listener Methods ==========

    override fun onConnected() {
        isDeviceConnected = true
        view.showDeviceStatus(true)
        logEvent("✓ ESP32 device connected successfully")
        view.showToast("Device connected!")

        // Reload progress to sync with ESP32
        loadTodayProgress()
    }

    override fun onDisconnected() {
        isDeviceConnected = false
        view.showDeviceStatus(false)
        logEvent("ESP32 device disconnected")

        if (sessionInProgress) {
            endSession(false)
        }
    }

    override fun onToothbrushRemoved() {
        logEvent("🟢 Toothbrush removed - Session started")
        startSession()
    }

    override fun onToothbrushReturned() {
        logEvent("🔴 Toothbrush returned")
        endSession(false) // Timer will be stopped, validation happens on ESP32
    }

    override fun onSessionComplete(duration: Int, valid: Boolean) {
        if (valid) {
            logEvent("✅ Session completed: ${duration}s (VALID)")
            view.showToast("Great job! Session saved")

            // Reload progress from Firebase (ESP32 already saved it)
            loadTodayProgress()
        } else {
            logEvent("❌ Session too short: ${duration}s (< 60s required)")
            view.showToast("Session too short! Brush for at least 60 seconds")
        }
    }

    override fun onError(error: String) {
        logEvent("❌ Error: $error")
        view.showToast("Error: $error")
    }

    // ========== Session Management ==========

    private fun startSession() {
        if (sessionInProgress) return

        sessionInProgress = true
        sessionStartTime = System.currentTimeMillis()

        view.showSessionInProgress(true, 0)

        sessionTimerJob = presenterScope.launch {
            while (sessionInProgress) {
                delay(1000)
                val duration = ((System.currentTimeMillis() - sessionStartTime) / 1000).toInt()
                view.showSessionInProgress(true, duration)
            }
        }
    }

    private fun endSession(cancelled: Boolean) {
        if (!sessionInProgress) return

        sessionInProgress = false
        sessionTimerJob?.cancel()
        view.showSessionInProgress(false, 0)

        if (cancelled) {
            logEvent("Session cancelled")
        }
    }

    // ========== Data Loading ==========

    private fun loadTodayProgress() {
        firebaseManager.getTodayProgress { progress ->
            progress?.let {
                val completed = it["completedSessions"] as? Int ?: 0
                currentProgress = completed

                view.showProgress(completed, 3)
                view.updateSessionDots(
                    morning = it["morningCompleted"] as? Boolean ?: false,
                    afternoon = it["afternoonCompleted"] as? Boolean ?: false,
                    evening = it["eveningCompleted"] as? Boolean ?: false
                )

                logEvent("Progress loaded: $completed/3 sessions")
            } ?: run {
                currentProgress = 0
                view.showProgress(0, 3)
                view.updateSessionDots(false, false, false)
                logEvent("No progress data for today")
            }
        }
    }

    private fun logEvent(event: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        view.addEventLog("[$timestamp] $event")
    }
}