package com.example.mintyminutes

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.Calendar

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "minty_minutes_reminders"
        const val CHANNEL_NAME = "Brushing Reminders"
        const val NOTIFICATION_PERMISSION_REQUEST = 100
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Notifications for brushing reminders"
                enableVibration(true)
                setShowBadge(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleNotification(scheduleId: Int, title: String, message: String, time: String) {
        println("DEBUG NotificationHelper: scheduleNotification called")
        println("DEBUG NotificationHelper: Schedule ID: $scheduleId")
        println("DEBUG NotificationHelper: Title: $title")
        println("DEBUG NotificationHelper: Message: $message")
        println("DEBUG NotificationHelper: Time string: $time")

        // Parse time string (format: "8:00 AM - 9:00 AM")
        val timeParts = time.split(" - ")
        if (timeParts.isEmpty()) {
            println("DEBUG NotificationHelper: ❌ Time string is empty or invalid")
            return
        }

        val startTime = timeParts[0].trim() // "8:00 AM"
        println("DEBUG NotificationHelper: Extracted start time: $startTime")

        val calendar = parseTimeToCalendar(startTime)

        if (calendar == null) {
            println("DEBUG NotificationHelper: ❌ Failed to parse time: $startTime")
            return
        }

        println("DEBUG NotificationHelper: Parsed calendar time: ${calendar.time}")
        println("DEBUG NotificationHelper: Current time: ${Calendar.getInstance().time}")

        // If the time is in the past today, schedule for tomorrow
        val now = System.currentTimeMillis()
        if (calendar.timeInMillis <= now) {
            println("DEBUG NotificationHelper: Time is in the past, scheduling for tomorrow")
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            println("DEBUG NotificationHelper: New scheduled time: ${calendar.time}")
        } else {
            println("DEBUG NotificationHelper: Time is in the future today")
        }

        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra("scheduleId", scheduleId)
            putExtra("title", title)
            putExtra("message", message)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            scheduleId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        try {
            // Use setExactAndAllowWhileIdle for better reliability
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }

            val timeUntilAlarm = (calendar.timeInMillis - now) / 1000 / 60 // minutes
            println("DEBUG NotificationHelper: ✅ Alarm set successfully!")
            println("DEBUG NotificationHelper: Alarm will trigger in $timeUntilAlarm minutes")
            println("DEBUG NotificationHelper: Trigger time: ${calendar.time}")
        } catch (e: Exception) {
            println("DEBUG NotificationHelper: ❌ Error setting alarm: ${e.message}")
            e.printStackTrace()
        }
    }

    fun cancelNotification(scheduleId: Int) {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            scheduleId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)

        println("DEBUG NotificationHelper: Cancelled alarm for schedule ID: $scheduleId")
    }

    private fun parseTimeToCalendar(timeStr: String): Calendar? {
        try {
            // Parse "8:00 AM" or "12:30 PM"
            val parts = timeStr.trim().split(" ")
            if (parts.size != 2) return null

            val timePart = parts[0] // "8:00"
            val amPm = parts[1].uppercase() // "AM" or "PM"

            val timeComponents = timePart.split(":")
            if (timeComponents.size != 2) return null

            var hour = timeComponents[0].toInt()
            val minute = timeComponents[1].toInt()

            // Convert to 24-hour format
            if (amPm == "PM" && hour != 12) {
                hour += 12
            } else if (amPm == "AM" && hour == 12) {
                hour = 0
            }

            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            return calendar
        } catch (e: Exception) {
            println("DEBUG NotificationHelper: Error parsing time: ${e.message}")
            return null
        }
    }

    fun showNotification(notificationId: Int, title: String, message: String) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            println("DEBUG NotificationHelper: Notification permission not granted")
            return
        }

        val intent = Intent(context, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(1000, 1000, 1000))
            .build()

        with(NotificationManagerCompat.from(context)) {
            notify(notificationId, notification)
        }

        println("DEBUG NotificationHelper: Notification shown: $title - $message")
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // No permission needed for older versions
        }
    }
}