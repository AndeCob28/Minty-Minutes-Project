package com.example.mintyminutes

// TEAM: Added Firebase imports for Auth and Database integration

import android.util.Patterns
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Team: Model class responsible for registration data operations and validation logic
 * NOW WITH FIREBASE AUTHENTICATION AND REALTIME DATABASE
 */
class RegisterModel {

    // TEAM: Firebase Auth instance for user authentication
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    // TEAM: Realtime Database instance - points to our Asia-Southeast1 database
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance("https://minty-minutes-cloud-default-rtdb.asia-southeast1.firebasedatabase.app/")

    suspend fun registerUser(
        name: String,
        email: String,
        password: String,
        phone: String // ADDED: Phone number parameter
    ): RegisterResult {
        return try {
            // TEAM: REPLACED mock registration with real Firebase Auth
            // FIREBASE AUTH REGISTRATION
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user

            if (user != null) {
                // TEAM: Set display name in user profile
                // Update user profile with display name
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
                user.updateProfile(profileUpdates).await()

                // TEAM: NEW - Save user data to Realtime Database
                // SAVE USER DATA TO REALTIME DATABASE
                saveUserToDatabase(user.uid, name, email, phone) // UPDATED: Added phone parameter

                RegisterResult.Success
            } else {
                RegisterResult.Error("User creation failed")
            }

        } catch (e: Exception) {
            // TEAM: Enhanced error handling for Firebase-specific errors
            // Handle Firebase Auth specific errors
            val errorMessage = when {
                e.message?.contains("email address is already in use") == true -> {
                    "This email is already registered. Please use a different email or login."
                }
                e.message?.contains("badly formatted") == true -> {
                    "Invalid email format. Please check your email address."
                }
                e.message?.contains("password is invalid") == true -> {
                    "Password should be at least 6 characters long."
                }
                e.message?.contains("network error") == true -> {
                    "Network error. Please check your internet connection."
                }
                else -> {
                    "Registration failed: ${e.message ?: "Unknown error"}"
                }
            }
            RegisterResult.Error(errorMessage)
        }
    }

    /**
     * TEAM: NEW METHOD - Saves user profile to Realtime Database
     * Creates user document with MintyMinutes defaults
     * Called after successful Firebase Auth registration
     */
    private suspend fun saveUserToDatabase(userId: String, name: String, email: String, phone: String) {
        try {
            val user = User(
                id = userId,
                name = name,
                email = email,
                phone = phone, // ADDED: Phone number field
                dailyGoal = 3, // Default goal: 3 brushing sessions per day
                streak = 0,
                totalSessions = 0,
                createdAt = Date().time,
                lastBrushingDate = getCurrentDate()
            )

            // TEAM: Save to "users/{userId}" path in Realtime Database
            // Save to Realtime Database
            database.getReference("users").child(userId).setValue(user).await()

        } catch (e: Exception) {
            // TEAM: Log database errors but don't fail registration
            // If database save fails, we still have auth success but log the error
            println("Failed to save user to database: ${e.message}")
            // You might want to handle this differently based on your requirements
        }
    }

    // TEAM: Helper for date formatting in database
    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }

    fun isValidName(name: String): Boolean {
        return name.trim().length >= 2
    }

    fun isValidEmail(email: String): Boolean {
        return email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }

    fun doPasswordsMatch(password: String, confirmPassword: String): Boolean {
        return password == confirmPassword
    }

    sealed class RegisterResult {
        object Success : RegisterResult()
        data class Error(val message: String) : RegisterResult()
    }
}