package com.example.mintyminutes

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

/**
 * Centralized Firebase manager for user-specific data operations
 * Handles schedules, notifications, sessions, progress, and device status
 */
class FirebaseManager {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance(
        "https://minty-minutes-cloud-default-rtdb.asia-southeast1.firebasedatabase.app/"
    )

    // Get current user ID
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    // Get today's date in YYYY-MM-DD format
    private fun getTodayDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    // Get reference to current user's data
    private fun getUserReference() = getCurrentUserId()?.let {
        database.getReference("users").child(it)
    }

    // ==================== SCHEDULES ====================

    suspend fun saveSchedule(schedule: Schedule): Boolean {
        return try {
            val userId = getCurrentUserId() ?: return false
            database.getReference("users")
                .child(userId)
                .child("schedules")
                .child(schedule.id.toString())
                .setValue(schedule)
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getSchedules(callback: (List<Schedule>) -> Unit) {
        val userId = getCurrentUserId()
        if (userId == null) {
            callback(emptyList())
            return
        }

        database.getReference("users")
            .child(userId)
            .child("schedules")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val schedules = mutableListOf<Schedule>()

                    if (!snapshot.exists()) {
                        println("DEBUG FirebaseManager: No schedules found for user $userId")
                        callback(emptyList())
                        return
                    }

                    for (child in snapshot.children) {
                        try {
                            child.getValue(Schedule::class.java)?.let {
                                schedules.add(it)
                                println("DEBUG FirebaseManager: Loaded schedule - ${it.title}: ${it.time}")
                            }
                        } catch (e: Exception) {
                            println("DEBUG FirebaseManager: Error parsing schedule: ${e.message}")
                        }
                    }

                    println("DEBUG FirebaseManager: Total schedules loaded: ${schedules.size}")
                    callback(schedules)
                }

                override fun onCancelled(error: DatabaseError) {
                    println("DEBUG FirebaseManager: Error loading schedules - ${error.message}")
                    callback(emptyList())
                }
            })
    }

    suspend fun deleteSchedule(scheduleId: Int): Boolean {
        return try {
            val userId = getCurrentUserId() ?: return false
            database.getReference("users")
                .child(userId)
                .child("schedules")
                .child(scheduleId.toString())
                .removeValue()
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ==================== NOTIFICATIONS ====================

    suspend fun saveNotification(notification: Notification): Boolean {
        return try {
            val userId = getCurrentUserId() ?: return false
            database.getReference("users")
                .child(userId)
                .child("notifications")
                .child(notification.id.toString())
                .setValue(notification)
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getNotifications(callback: (List<Notification>) -> Unit) {
        val userId = getCurrentUserId() ?: return

        database.getReference("users")
            .child(userId)
            .child("notifications")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val notifications = mutableListOf<Notification>()
                    for (child in snapshot.children) {
                        child.getValue(Notification::class.java)?.let { notifications.add(it) }
                    }
                    callback(notifications)
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(emptyList())
                }
            })
    }

    suspend fun deleteNotification(notificationId: Int): Boolean {
        return try {
            val userId = getCurrentUserId() ?: return false
            database.getReference("users")
                .child(userId)
                .child("notifications")
                .child(notificationId.toString())
                .removeValue()
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ==================== BRUSHING SESSIONS ====================

    suspend fun saveBrushingSession(
        sessionType: String, // "morning", "afternoon", "evening"
        duration: Int = 120, // in seconds
        score: Int = 85 // brushing quality score (0-100)
    ): Boolean {
        return try {
            val userId = getCurrentUserId() ?: return false
            val date = getTodayDate()

            // Create session ID
            val sessionId = database.reference.child("brushingSessions").push().key
                ?: System.currentTimeMillis().toString()

            // Create session data
            val session = mapOf(
                "sessionId" to sessionId,
                "userId" to userId,
                "date" to date,
                "timestamp" to System.currentTimeMillis(),
                "sessionType" to sessionType,
                "duration" to duration,
                "score" to score
            )

            // Save session
            database.getReference("users")
                .child(userId)
                .child("brushingSessions")
                .child(sessionId)
                .setValue(session)
                .await()

            // Update daily progress
            updateDailyProgress(sessionType, date)

            // Update total sessions count
            updateTotalSessions()

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private suspend fun updateDailyProgress(sessionType: String, date: String) {
        try {
            val userId = getCurrentUserId() ?: return

            val progressRef = database.getReference("users")
                .child(userId)
                .child("dailyProgress")
                .child(date)

            // Get current progress
            val snapshot = progressRef.get().await()

            val currentProgress = if (snapshot.exists()) {
                // Parse existing progress
                val completedSessions = snapshot.child("completedSessions").getValue(Int::class.java) ?: 0
                val morningCompleted = snapshot.child("morningCompleted").getValue(Boolean::class.java) ?: false
                val afternoonCompleted = snapshot.child("afternoonCompleted").getValue(Boolean::class.java) ?: false
                val eveningCompleted = snapshot.child("eveningCompleted").getValue(Boolean::class.java) ?: false

                mapOf(
                    "userId" to userId,
                    "date" to date,
                    "completedSessions" to completedSessions + 1,
                    "morningCompleted" to (morningCompleted || sessionType == "morning"),
                    "afternoonCompleted" to (afternoonCompleted || sessionType == "afternoon"),
                    "eveningCompleted" to (eveningCompleted || sessionType == "evening"),
                    "lastUpdated" to System.currentTimeMillis()
                )
            } else {
                // Create new progress
                mapOf(
                    "userId" to userId,
                    "date" to date,
                    "completedSessions" to 1,
                    "morningCompleted" to (sessionType == "morning"),
                    "afternoonCompleted" to (sessionType == "afternoon"),
                    "eveningCompleted" to (sessionType == "evening"),
                    "lastUpdated" to System.currentTimeMillis()
                )
            }

            // Save updated progress
            progressRef.setValue(currentProgress).await()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getTodaySessions(callback: (Int) -> Unit) {
        val userId = getCurrentUserId() ?: return
        val today = getTodayDate()

        database.getReference("users")
            .child(userId)
            .child("brushingSessions")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var count = 0
                    for (child in snapshot.children) {
                        val sessionDate = child.child("date").getValue(String::class.java)
                        if (sessionDate == today) {
                            count++
                        }
                    }
                    callback(count)
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(0)
                }
            })
    }

    fun getTodayProgress(callback: (Map<String, Any>?) -> Unit) {
        val userId = getCurrentUserId() ?: return
        val date = getTodayDate()

        database.getReference("users")
            .child(userId)
            .child("dailyProgress")
            .child(date)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val progress = snapshot.getValue(object :
                            com.google.firebase.database.GenericTypeIndicator<Map<String, Any>>() {})
                        callback(progress)
                    } else {
                        // Return default empty progress
                        val defaultProgress = mapOf(
                            "userId" to userId,
                            "date" to date,
                            "completedSessions" to 0,
                            "morningCompleted" to false,
                            "afternoonCompleted" to false,
                            "eveningCompleted" to false,
                            "lastUpdated" to System.currentTimeMillis()
                        )
                        callback(defaultProgress)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    println("DEBUG: Error loading progress - ${error.message}")
                    callback(null)
                }
            })
    }

    private suspend fun updateTotalSessions() {
        try {
            val userId = getCurrentUserId() ?: return
            val userRef = database.getReference("users").child(userId)

            val snapshot = userRef.child("totalSessions").get().await()
            val currentTotal = snapshot.getValue(Int::class.java) ?: 0

            userRef.child("totalSessions").setValue(currentTotal + 1).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ==================== DEVICE STATUS ====================

    suspend fun updateDeviceStatus(
        isConnected: Boolean,
        batteryLevel: Int = 100,
        deviceName: String = "Smart Toothbrush"
    ): Boolean {
        return try {
            val userId = getCurrentUserId() ?: return false

            val deviceStatus = mapOf(
                "userId" to userId,
                "deviceName" to deviceName,
                "isConnected" to isConnected,
                "lastConnected" to if (isConnected) System.currentTimeMillis() else 0,
                "batteryLevel" to batteryLevel,
                "lastUpdated" to System.currentTimeMillis()
            )

            database.getReference("users")
                .child(userId)
                .child("deviceStatus")
                .setValue(deviceStatus)
                .await()

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun listenToDeviceStatus(callback: (Map<String, Any>?) -> Unit) {
        val userId = getCurrentUserId() ?: return

        database.getReference("users")
            .child(userId)
            .child("deviceStatus")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val status = snapshot.getValue(object :
                            com.google.firebase.database.GenericTypeIndicator<Map<String, Any>>() {})
                        callback(status)
                    } else {
                        // Default disconnected status
                        val defaultStatus = mapOf(
                            "userId" to userId,
                            "isConnected" to false,
                            "deviceName" to "Smart Toothbrush",
                            "batteryLevel" to 0,
                            "lastUpdated" to System.currentTimeMillis()
                        )
                        callback(defaultStatus)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    println("DEBUG: Error loading device status - ${error.message}")
                    callback(null)
                }
            })
    }

    // ==================== USER DATA ====================

    fun getUserData(callback: (User?) -> Unit) {
        val userId = getCurrentUserId() ?: return

        database.getReference("users")
            .child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)
                    callback(user)
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(null)
                }
            })
    }

    // ==================== SESSION HISTORY ====================

    fun getSessionHistory(limit: Int = 30, callback: (List<Map<String, Any>>) -> Unit) {
        val userId = getCurrentUserId() ?: return

        database.getReference("users")
            .child(userId)
            .child("brushingSessions")
            .limitToLast(limit)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val sessions = mutableListOf<Map<String, Any>>()

                    for (child in snapshot.children) {
                        val session = child.getValue(object :
                            com.google.firebase.database.GenericTypeIndicator<Map<String, Any>>() {})
                        session?.let { sessions.add(it) }
                    }

                    // Sort by timestamp (newest first)
                    sessions.sortByDescending { it["timestamp"] as? Long ?: 0L }
                    callback(sessions)
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(emptyList())
                }
            })
    }

    // Add this method to FirebaseManager.kt
    fun getTodayTotalTime(callback: (Int) -> Unit) {
        val userId = getCurrentUserId() ?: return callback(0)
        val today = getTodayDate()

        database.getReference("users")
            .child(userId)
            .child("brushingSessions")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var totalSeconds = 0

                    for (child in snapshot.children) {
                        val sessionDate = child.child("date").getValue(String::class.java)
                        val duration = child.child("duration").getValue(Int::class.java)

                        if (sessionDate == today && duration != null) {
                            totalSeconds += duration
                        }
                    }

                    callback(totalSeconds)
                }

                override fun onCancelled(error: DatabaseError) {
                    println("DEBUG: Error fetching total time - ${error.message}")
                    callback(0)
                }
            })
    }

    // ==================== WEEKLY DATA ====================

    fun getWeeklyData(callback: (Map<String, Int>) -> Unit) {
        val userId = getCurrentUserId() ?: return callback(emptyMap())

        database.getReference("users")
            .child(userId)
            .child("brushingSessions")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val weeklyData = mutableMapOf<String, Int>()

                    // Initialize all days with 0
                    val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
                    daysOfWeek.forEach { day ->
                        weeklyData[day] = 0
                    }

                    // Get dates for the past 7 days
                    val calendar = Calendar.getInstance()
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())

                    // Process each session
                    for (child in snapshot.children) {
                        val sessionDateStr = child.child("date").getValue(String::class.java)
                        val duration = child.child("duration").getValue(Int::class.java) ?: 0

                        if (sessionDateStr != null && duration > 0) {
                            try {
                                val sessionDate = dateFormat.parse(sessionDateStr)
                                val today = Date()
                                val sevenDaysAgo = Calendar.getInstance().apply {
                                    add(Calendar.DAY_OF_YEAR, -7)
                                }.time

                                // Check if session is within the last 7 days
                                if (sessionDate != null && sessionDate.after(sevenDaysAgo) && !sessionDate.after(today)) {
                                    calendar.time = sessionDate
                                    val dayOfWeek = dayFormat.format(sessionDate)
                                    weeklyData[dayOfWeek] = weeklyData.getOrDefault(dayOfWeek, 0) + duration
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }

                    callback(weeklyData)
                }

                override fun onCancelled(error: DatabaseError) {
                    println("DEBUG: Error fetching weekly data - ${error.message}")
                    callback(emptyMap())
                }
            })
    }

    // Alternative: Get weekly session counts (number of brushing sessions per day)
    fun getWeeklySessionCounts(callback: (Map<String, Int>) -> Unit) {
        val userId = getCurrentUserId() ?: return callback(emptyMap())

        database.getReference("users")
            .child(userId)
            .child("brushingSessions")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val weeklyData = mutableMapOf<String, Int>()

                    // Initialize all days with 0
                    val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
                    daysOfWeek.forEach { day ->
                        weeklyData[day] = 0
                    }

                    // Get dates for the past 7 days
                    val calendar = Calendar.getInstance()
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())

                    // Process each session
                    for (child in snapshot.children) {
                        val sessionDateStr = child.child("date").getValue(String::class.java)

                        if (sessionDateStr != null) {
                            try {
                                val sessionDate = dateFormat.parse(sessionDateStr)
                                val today = Date()
                                val sevenDaysAgo = Calendar.getInstance().apply {
                                    add(Calendar.DAY_OF_YEAR, -7)
                                }.time

                                // Check if session is within the last 7 days
                                if (sessionDate != null && sessionDate.after(sevenDaysAgo) && !sessionDate.after(today)) {
                                    calendar.time = sessionDate
                                    val dayOfWeek = dayFormat.format(sessionDate)
                                    weeklyData[dayOfWeek] = weeklyData.getOrDefault(dayOfWeek, 0) + 1
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }

                    callback(weeklyData)
                }

                override fun onCancelled(error: DatabaseError) {
                    println("DEBUG: Error fetching weekly session counts - ${error.message}")
                    callback(emptyMap())
                }
            })
    }
}