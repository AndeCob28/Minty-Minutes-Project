package com.example.mintyminutes

interface HomeContract {

    interface View {
        fun showProgress(current: Int, total: Int)
        fun updateSessionDots(morning: Boolean, afternoon: Boolean, evening: Boolean)
        fun showDeviceStatus(connected: Boolean)  // Removed batteryLevel parameter
        fun addEventLog(event: String)
        fun clearEventLog()
        fun showToast(message: String)
        fun updateWelcomeText(name: String)
        fun updateReminderText(progress: Int)
        fun showLoading(show: Boolean)
        fun navigateToSchedule()
        fun navigateToNotifications()
        fun navigateToHistory()
        fun navigateToProfile()
        fun navigateToSettings()
        fun navigateToLogin()
        fun closeDrawer()
        fun showSessionInProgress(inProgress: Boolean, duration: Int)
        fun showConnectionDialog()
        fun showConnectionOptionsDialog()  // Added for auto-discovery
    }

    interface Presenter {
        fun onViewCreated()
        fun onResume()
        fun onPause()
        fun onSessionAddClicked()
        fun onDeviceStatusClicked()
        fun onClearLogClicked()
        fun onMenuItemSelected(itemId: Int)
        fun onLogoutClicked()
        fun onDestroy()
        fun connectToESP32(ipAddress: String)
        fun disconnectFromESP32()
    }
}

enum class SessionType(val value: String) {
    MORNING("morning"),
    AFTERNOON("afternoon"),
    EVENING("evening")
}