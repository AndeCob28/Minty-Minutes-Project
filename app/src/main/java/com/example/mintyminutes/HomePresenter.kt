package com.example.mintyminutes

import android.util.Log
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

    override fun onViewCreated() {
        try {
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
        } catch (e: Exception) {
            Log.e("HomePresenter", "onViewCreated error: ${e.message}", e)
            view.showToast("Initialization error: ${e.message}")
        }
    }

    override fun onResume() {
        try {
            loadTodayProgress()
        } catch (e: Exception) {
            Log.e("HomePresenter", "onResume error: ${e.message}", e)
        }
    }

    override fun onPause() {}

    override fun onSessionAddClicked() {
        view.showToast("Please use your smart toothbrush to start a session")
    }

    override fun onDeviceStatusClicked() {
        try {
            if (!isDeviceConnected) {
                view.showConnectionOptionsDialog()
            } else {
                disconnectFromESP32()
            }
        } catch (e: Exception) {
            Log.e("HomePresenter", "onDeviceStatusClicked error: ${e.message}", e)
            view.showToast("Error: ${e.message}")
        }
    }

    override fun connectToESP32(ipAddress: String) {
        try {
            logEvent("Connecting to ESP32: $ipAddress")

            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                view.showToast("Error: No user ID found")
                logEvent("Connection failed: No user ID")
                return
            }

            Log.d("HomePresenter", "Attempting connection to $ipAddress")
            // ESP32Manager now gets userId automatically, no need to pass it
            esp32Manager.connect(ipAddress)
        } catch (e: Exception) {
            Log.e("HomePresenter", "connectToESP32 error: ${e.message}", e)
            view.showToast("Connection error: ${e.message}")
            logEvent("Connection error: ${e.message}")
        }
    }

    override fun disconnectFromESP32() {
        try {
            esp32Manager.disconnect()
            isDeviceConnected = false
            view.showDeviceStatus(false)
            logEvent("Disconnected from ESP32")
        } catch (e: Exception) {
            Log.e("HomePresenter", "disconnectFromESP32 error: ${e.message}", e)
        }
    }

    override fun onClearLogClicked() {
        try {
            view.clearEventLog()
            logEvent("Event log cleared")
        } catch (e: Exception) {
            Log.e("HomePresenter", "onClearLogClicked error: ${e.message}", e)
        }
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
        try {
            disconnectFromESP32()
            model.clearSession()
            FirebaseAuth.getInstance().signOut()
            view.showToast("Logged out successfully")
            view.navigateToLogin()
        } catch (e: Exception) {
            Log.e("HomePresenter", "onLogoutClicked error: ${e.message}", e)
        }
    }

    override fun onDestroy() {
        try {
            sessionTimerJob?.cancel()
            esp32Manager.disconnect()
            presenterScope.cancel()
        } catch (e: Exception) {
            Log.e("HomePresenter", "onDestroy error: ${e.message}", e)
        }
    }

    fun isDeviceConnected(): Boolean = isDeviceConnected

    // ========== ESP32 Listener Methods ==========

    override fun onConnected() {
        presenterScope.launch(Dispatchers.Main) {
            try {
                isDeviceConnected = true
                view.showDeviceStatus(true)
                logEvent("✓ ESP32 device connected successfully")
                view.showToast("Device connected!")

                // ESP32 will send current progress automatically
            } catch (e: Exception) {
                Log.e("HomePresenter", "onConnected error: ${e.message}", e)
            }
        }
    }

    override fun onDisconnected() {
        presenterScope.launch(Dispatchers.Main) {
            try {
                isDeviceConnected = false
                view.showDeviceStatus(false)
                logEvent("ESP32 device disconnected")

                if (sessionInProgress) {
                    endSession(true)
                }
            } catch (e: Exception) {
                Log.e("HomePresenter", "onDisconnected error: ${e.message}", e)
            }
        }
    }

    override fun onToothbrushRemoved() {
        presenterScope.launch(Dispatchers.Main) {
            try {
                logEvent("🟢 Toothbrush removed - Session started")
                startSession()
            } catch (e: Exception) {
                Log.e("HomePresenter", "onToothbrushRemoved error: ${e.message}", e)
            }
        }
    }

    override fun onToothbrushReturned() {
        presenterScope.launch(Dispatchers.Main) {
            try {
                logEvent("🔴 Toothbrush returned to holder")
                endSession(false)
            } catch (e: Exception) {
                Log.e("HomePresenter", "onToothbrushReturned error: ${e.message}", e)
            }
        }
    }

    override fun onSessionComplete(duration: Int, valid: Boolean) {
        presenterScope.launch(Dispatchers.Main) {
            try {
                if (valid) {
                    logEvent("✅ Session completed: ${duration}s (VALID)")
                    view.showToast("Great job! Session saved")
                } else {
                    logEvent("❌ Session too short: ${duration}s (< 60s required)")
                    view.showToast("Session too short! Brush for at least 60 seconds")
                }
            } catch (e: Exception) {
                Log.e("HomePresenter", "onSessionComplete error: ${e.message}", e)
            }
        }
    }

    override fun onProgressUpdate(current: Int, total: Int) {
        presenterScope.launch(Dispatchers.Main) {
            try {
                currentProgress = current
                view.showProgress(current, total)
                logEvent("Progress updated: $current/$total sessions")
            } catch (e: Exception) {
                Log.e("HomePresenter", "onProgressUpdate error: ${e.message}", e)
            }
        }
    }

    override fun onSessionDotsUpdate(morning: Boolean, afternoon: Boolean, evening: Boolean) {
        presenterScope.launch(Dispatchers.Main) {
            try {
                view.updateSessionDots(morning, afternoon, evening)
                logEvent("Session dots: AM=$morning NN=$afternoon PM=$evening")
            } catch (e: Exception) {
                Log.e("HomePresenter", "onSessionDotsUpdate error: ${e.message}", e)
            }
        }
    }

    override fun onEventLog(event: String) {
        presenterScope.launch(Dispatchers.Main) {
            try {
                logEvent(event)
            } catch (e: Exception) {
                Log.e("HomePresenter", "onEventLog error: ${e.message}", e)
            }
        }
    }

    override fun onError(error: String) {
        presenterScope.launch(Dispatchers.Main) {
            try {
                logEvent("❌ Error: $error")
                view.showToast("Error: $error")
            } catch (e: Exception) {
                Log.e("HomePresenter", "onError error: ${e.message}", e)
            }
        }
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
        try {
            firebaseManager.getTodayProgress { progress ->
                presenterScope.launch(Dispatchers.Main) {
                    try {
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
                    } catch (e: Exception) {
                        Log.e("HomePresenter", "loadTodayProgress callback error: ${e.message}", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("HomePresenter", "loadTodayProgress error: ${e.message}", e)
        }
    }

    private fun logEvent(event: String) {
        try {
            val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            view.addEventLog("[$timestamp] $event")
        } catch (e: Exception) {
            Log.e("HomePresenter", "logEvent error: ${e.message}", e)
        }
    }
}