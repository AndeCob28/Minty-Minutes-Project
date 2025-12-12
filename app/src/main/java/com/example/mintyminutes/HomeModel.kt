package com.example.mintyminutes.model

import java.util.*

data class HomeModel(
    val sessionId: String = "",
    val title: String = "",
    val time: String = "",
    val status: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val sessionType: String = "", // "morning", "afternoon", "evening"
    val date: String = "" // YYYY-MM-DD format
) {
    // Helper method to get current date
    fun getTodayDate(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }
}