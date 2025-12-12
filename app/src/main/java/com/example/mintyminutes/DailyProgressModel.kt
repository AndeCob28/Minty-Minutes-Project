package com.example.mintyminutes.model

data class DailyProgressModel(
    val userId: String = "",
    val date: String = "",
    val totalSessions: Int = 3, // Always 3 per day
    val completedSessions: Int = 0,
    val morningCompleted: Boolean = false,
    val afternoonCompleted: Boolean = false,
    val eveningCompleted: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)