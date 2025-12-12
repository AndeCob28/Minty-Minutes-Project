package com.example.mintyminutes

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val scheduleId = intent.getIntExtra("scheduleId", -1)
        val title = intent.getStringExtra("title") ?: "MintyMinutes"
        val message = intent.getStringExtra("message") ?: "Time to brush your teeth!"

        println("DEBUG ReminderBroadcastReceiver: Received alarm for schedule ID: $scheduleId")
        println("DEBUG ReminderBroadcastReceiver: Title: $title, Message: $message")

        // Show the notification on device
        val notificationHelper = NotificationHelper(context)
        notificationHelper.showNotification(scheduleId, title, message)

        // Save notification to Firebase
        saveNotificationToFirebase(context, scheduleId, title, message)
    }

    private fun saveNotificationToFirebase(context: Context, scheduleId: Int, title: String, message: String) {
        // Create notification object with unique ID based on timestamp
        val notification = Notification(
            id = System.currentTimeMillis().toInt(), // Unique ID
            title = title,
            message = message
        )

        println("DEBUG ReminderBroadcastReceiver: Saving notification to Firebase - ID: ${notification.id}, Title: $title")

        // Save to Firebase using background scope
        val firebaseManager = FirebaseManager()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val success = firebaseManager.saveNotification(notification)
                if (success) {
                    println("DEBUG ReminderBroadcastReceiver: ✅ Notification saved to Firebase successfully!")
                } else {
                    println("DEBUG ReminderBroadcastReceiver: ❌ Failed to save notification to Firebase")
                }
            } catch (e: Exception) {
                println("DEBUG ReminderBroadcastReceiver: ❌ Error saving notification: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}