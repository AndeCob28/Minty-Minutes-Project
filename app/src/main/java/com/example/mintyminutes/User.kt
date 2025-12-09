package com.example.mintyminutes

import java.util.Date

/**
 * TEAM NOTE: Added User data model for Firebase Realtime Database
 * Stores user profile data with MintyMinutes-specific fields
 * Created as part of Firebase integration - [Date]
 */

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val dailyGoal: Int = 3,  // Default: 3 brushing sessions per day
    val streak: Int = 0,     // Current streak in days
    val totalSessions: Int = 0,
    val createdAt: Long = Date().time,
    val lastBrushingDate: String = "" // Format: "YYYY-MM-DD"
)