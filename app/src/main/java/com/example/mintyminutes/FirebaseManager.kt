package com.example.mintyminutes

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class FirebaseManager {

    companion object {
        private const val TAG = "FirebaseManager"
    }

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance(
        "https://minty-minutes-cloud-default-rtdb.asia-southeast1.firebasedatabase.app/"
    )

    // ==================== USER INFO ====================

    fun getCurrentUserId(): String? {
        return try {
            val userId = auth.currentUser?.uid
            Log.d(TAG, "getCurrentUserId(): $userId")
            userId
        } catch (e: Exception) {
            Log.e(TAG, "Error in getCurrentUserId(): ${e.message}")
            null
        }
    }

    fun getUserEmail(): String? {
        return try {
            val user = auth.currentUser
            val email = user?.email
            Log.d(TAG, "getUserEmail(): $email")
            email
        } catch (e: Exception) {
            Log.e(TAG, "Error in getUserEmail(): ${e.message}")
            null
        }
    }

    fun isUserLoggedIn(): Boolean {
        return try {
            val isLoggedIn = auth.currentUser != null
            Log.d(TAG, "isUserLoggedIn(): $isLoggedIn")
            isLoggedIn
        } catch (e: Exception) {
            Log.e(TAG, "Error in isUserLoggedIn(): ${e.message}")
            false
        }
    }

    // ==================== ACCOUNT DELETION ====================

    /**
     * Delete current user account immediately
     * Returns DeletionResult (Success or Error)
     */
    suspend fun deleteCurrentUserAccountImmediately(password: String): DeletionResult {
        Log.d(TAG, "deleteCurrentUserAccountImmediately() started")

        return try {
            val user = auth.currentUser
            if (user == null) {
                Log.e(TAG, "No user logged in")
                return DeletionResult.Error("No user logged in")
            }

            val userId = user.uid
            val userEmail = user.email
            Log.d(TAG, "Deleting account for user: $userId, email: $userEmail")

            // Step 1: Re-authenticate user (required by Firebase for deletion)
            if (userEmail.isNullOrEmpty()) {
                return DeletionResult.Error("User email not found")
            }

            Log.d(TAG, "Re-authenticating user...")
            val credential = EmailAuthProvider.getCredential(userEmail, password)
            user.reauthenticate(credential).await()

            // Step 2: Delete all user data from Firebase Database
            Log.d(TAG, "Deleting user data from database...")
            val databaseDeletionResult = deleteAllUserData(userId)
            if (!databaseDeletionResult) {
                return DeletionResult.Error("Failed to delete user data from database")
            }

            // Step 3: Delete user from Firebase Authentication
            Log.d(TAG, "Deleting user from Firebase Authentication...")
            user.delete().await()

            // Step 4: Sign out locally
            auth.signOut()

            Log.d(TAG, "Account deletion completed successfully")
            DeletionResult.Success("Account and all data deleted successfully")

        } catch (e: com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
            Log.e(TAG, "Invalid password: ${e.message}")
            DeletionResult.Error("Invalid password. Please try again.")

        } catch (e: com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
            Log.e(TAG, "Re-authentication required: ${e.message}")
            DeletionResult.Error("Re-authentication required. Please login again.")

        } catch (e: com.google.firebase.auth.FirebaseAuthException) {
            Log.e(TAG, "Firebase auth error: ${e.message}")
            DeletionResult.Error("Authentication error: ${e.message ?: "Unknown error"}")

        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during account deletion: ${e.message}", e)
            DeletionResult.Error("Failed to delete account: ${e.message ?: "Unknown error"}")
        }
    }

    /**
     * Delete all user data from Firebase Realtime Database
     */
    private suspend fun deleteAllUserData(userId: String): Boolean {
        Log.d(TAG, "deleteAllUserData() for user: $userId")

        return try {
            // Delete entire user node and all sub-nodes
            val userRef = database.getReference("users").child(userId)

            // Get all child references to delete
            val childRefs = listOf(
                userRef.child("schedules"),
                userRef.child("notifications"),
                userRef.child("brushingSessions"),
                userRef.child("dailyProgress"),
                userRef.child("deviceStatus"),
                userRef.child("totalSessions")
            )

            // Delete all child references
            for (ref in childRefs) {
                try {
                    ref.removeValue().await()
                    Log.d(TAG, "Deleted reference: ${ref.key}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting ${ref.key}: ${e.message}")
                }
            }

            // Finally delete the main user reference
            userRef.removeValue().await()
            Log.d(TAG, "Deleted main user reference")

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error in deleteAllUserData: ${e.message}", e)
            false
        }
    }

    // ==================== DELETION RESULT SEALED CLASS ====================

    sealed class DeletionResult {
        data class Success(val message: String) : DeletionResult()
        data class Error(val message: String) : DeletionResult()
    }

    // ==================== UTILITY METHODS ====================

    private fun getTodayDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    // ==================== SCHEDULES ====================

    suspend fun saveSchedule(schedule: Schedule): Boolean {
        Log.d(TAG, "saveSchedule(): ${schedule.title}")
        return try {
            val userId = getCurrentUserId() ?: return false
            database.getReference("users")
                .child(userId)
                .child("schedules")
                .child(schedule.id.toString())
                .setValue(schedule)
                .await()
            Log.d(TAG, "Schedule saved successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving schedule: ${e.message}")
            false
        }
    }

    fun getSchedules(callback: (List<Schedule>) -> Unit) {
        val userId = getCurrentUserId()
        if (userId == null) {
            Log.w(TAG, "getSchedules(): No user ID")
            callback(emptyList())
            return
        }

        Log.d(TAG, "getSchedules() for user: $userId")
        database.getReference("users")
            .child(userId)
            .child("schedules")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val schedules = mutableListOf<Schedule>()

                    if (!snapshot.exists()) {
                        Log.d(TAG, "No schedules found for user $userId")
                        callback(emptyList())
                        return
                    }

                    for (child in snapshot.children) {
                        try {
                            child.getValue(Schedule::class.java)?.let {
                                schedules.add(it)
                                Log.d(TAG, "Loaded schedule: ${it.title}")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing schedule: ${e.message}")
                        }
                    }

                    Log.d(TAG, "Total schedules loaded: ${schedules.size}")
                    callback(schedules)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error loading schedules: ${error.message}")
                    callback(emptyList())
                }
            })
    }

    suspend fun deleteSchedule(scheduleId: Int): Boolean {
        Log.d(TAG, "deleteSchedule(): $scheduleId")
        return try {
            val userId = getCurrentUserId() ?: return false
            database.getReference("users")
                .child(userId)
                .child("schedules")
                .child(scheduleId.toString())
                .removeValue()
                .await()
            Log.d(TAG, "Schedule deleted successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting schedule: ${e.message}")
            false
        }
    }

    // ==================== NOTIFICATIONS ====================

    suspend fun saveNotification(notification: Notification): Boolean {
        Log.d(TAG, "saveNotification(): ${notification.title}")
        return try {
            val userId = getCurrentUserId() ?: return false
            database.getReference("users")
                .child(userId)
                .child("notifications")
                .child(notification.id.toString())
                .setValue(notification)
                .await()
            Log.d(TAG, "Notification saved successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving notification: ${e.message}")
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
        Log.d(TAG, "deleteNotification(): $notificationId")
        return try {
            val userId = getCurrentUserId() ?: return false
            database.getReference("users")
                .child(userId)
                .child("notifications")
                .child(notificationId.toString())
                .removeValue()
                .await()
            Log.d(TAG, "Notification deleted successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting notification: ${e.message}")
            false
        }
    }

    // ==================== BRUSHING SESSIONS ====================

    suspend fun saveBrushingSession(
        sessionType: String, // "morning", "afternoon", "evening"
        duration: Int = 120, // in seconds
        score: Int = 85 // brushing quality score (0-100)
    ): Boolean {
        Log.d(TAG, "saveBrushingSession(): $sessionType")
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

            Log.d(TAG, "Brushing session saved successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving brushing session: ${e.message}")
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
            Log.e(TAG, "Error updating daily progress: ${e.message}")
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
                    Log.e(TAG, "Error loading progress: ${error.message}")
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
            Log.e(TAG, "Error updating total sessions: ${e.message}")
        }
    }

    // ==================== DEVICE STATUS ====================

    suspend fun updateDeviceStatus(
        isConnected: Boolean,
        batteryLevel: Int = 100,
        deviceName: String = "Smart Toothbrush"
    ): Boolean {
        Log.d(TAG, "updateDeviceStatus(): isConnected=$isConnected")
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

            Log.d(TAG, "Device status updated successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating device status: ${e.message}")
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
                    Log.e(TAG, "Error loading device status: ${error.message}")
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
                    Log.e(TAG, "Error fetching total time: ${error.message}")
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
                                Log.e(TAG, "Error parsing session date: ${e.message}")
                            }
                        }
                    }

                    callback(weeklyData)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error fetching weekly data: ${error.message}")
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
                                Log.e(TAG, "Error parsing session date: ${e.message}")
                            }
                        }
                    }

                    callback(weeklyData)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error fetching weekly session counts: ${error.message}")
                    callback(emptyMap())
                }
            })
    }
}