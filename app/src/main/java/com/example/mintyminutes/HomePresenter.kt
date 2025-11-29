package com.example.mintyminutes.presenter

import com.example.mintyminutes.model.HomeModel

class HomePresenter(private val view: View) {

    interface View {
        fun showSessions(sessions: List<HomeModel>)
        fun showConnectingMessage()
        fun showScheduleMessage()
    }

    fun onConnectDeviceClicked() {
        view.showConnectingMessage()
        // TODO: Add real device connection logic
    }

    fun onScheduleClicked() {
        view.showScheduleMessage()
        // TODO: Add scheduling logic
    }

    fun loadBrushingSessions() {
        val sessions = listOf(
            HomeModel("Morning Brush", "2m 15s • 8:30 AM", "Perfect!"),
            HomeModel("After Lunch", "1m 45s • 1:15 PM", "Good")
        )
        view.showSessions(sessions)
    }
}
