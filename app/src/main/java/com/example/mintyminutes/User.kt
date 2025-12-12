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
    val dailyGoal: Int = 3,
    val streak: Int = 0,
    val totalSessions: Int = 0,
    val createdAt: Long = Date().time,
    val lastBrushingDate: String = "",
    val profilePictureUrl: String = "" // NEW FIELD
)