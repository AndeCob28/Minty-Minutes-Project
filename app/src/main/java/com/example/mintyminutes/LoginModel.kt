package com.example.mintyminutes

import android.util.Patterns
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class LoginModel {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance("https://minty-minutes-cloud-default-rtdb.asia-southeast1.firebasedatabase.app/")

    suspend fun authenticateUser(email: String, password: String, registeredName: String? = null): LoginResult {
        return try {
            // Firebase Authentication
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user

            if (user != null) {
                // Get user data from Realtime Database to get the name
                val userSnapshot = database.getReference("users").child(user.uid).get().await()

                val userName = if (userSnapshot.exists()) {
                    // Get name from database
                    userSnapshot.child("name").getValue(String::class.java) ?: registeredName ?: user.displayName ?: "User"
                } else {
                    // Fallback to registered name or display name
                    registeredName ?: user.displayName ?: "User"
                }

                LoginResult.Success(userName)
            } else {
                LoginResult.Error("Authentication failed")
            }

        } catch (e: Exception) {
            // Handle Firebase Auth errors
            val errorMessage = when {
                e.message?.contains("invalid credential") == true -> {
                    "Invalid email or password"
                }
                e.message?.contains("user not found") == true -> {
                    "No account found with this email"
                }
                e.message?.contains("badly formatted") == true -> {
                    "Invalid email format"
                }
                e.message?.contains("network error") == true -> {
                    "Network error. Please check your internet connection"
                }
                else -> {
                    "Login failed: ${e.message ?: "Unknown error"}"
                }
            }
            LoginResult.Error(errorMessage)
        }
    }

    fun isValidEmail(email: String): Boolean {
        return email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }

    sealed class LoginResult {
        data class Success(val userName: String) : LoginResult()
        data class Error(val message: String) : LoginResult()
    }
}