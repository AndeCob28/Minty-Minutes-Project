package com.example.mintyminutes

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class ScheduleActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var menuIcon: ImageView
    private lateinit var addReminderBtn: Button
    private lateinit var scheduleRecyclerView: RecyclerView

    // Bottom navigation - REMOVED addBtn
    private lateinit var homeBtn: ImageButton
    private lateinit var scheduleBtn: ImageButton
    private lateinit var notificationBtn: ImageButton

    private lateinit var scheduleAdapter: ScheduleAdapter
    private val scheduleList = mutableListOf<Schedule>()
    private var nextId = 1

    // Firebase Manager
    private val firebaseManager = FirebaseManager()

    // Notification Helper
    private lateinit var notificationHelper: NotificationHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule)

        // Initialize notification helper
        notificationHelper = NotificationHelper(this)

        // Request notification permission for Android 13+
        requestNotificationPermission()

        initializeViews()
        setupRecyclerView()
        setupClickListeners()
        setupSidebarMenu()
        updateBottomNavSelection(scheduleBtn)
        loadSchedulesFromFirebase()
    }

    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (!notificationHelper.hasNotificationPermission()) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    NotificationHelper.NOTIFICATION_PERMISSION_REQUEST
                )
            }
        }
    }

    private fun initializeViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        menuIcon = findViewById(R.id.menuIcon)
        addReminderBtn = findViewById(R.id.addReminderBtn)
        scheduleRecyclerView = findViewById(R.id.scheduleRecyclerView)

        // Bottom navigation - REMOVED addBtn initialization
        homeBtn = findViewById(R.id.homeBtn)
        scheduleBtn = findViewById(R.id.scheduleBtn)
        notificationBtn = findViewById(R.id.notificationBtn)
    }

    private fun setupRecyclerView() {
        scheduleAdapter = ScheduleAdapter(
            scheduleList,
            onEditClick = { schedule -> showEditDialog(schedule) },
            onDeleteClick = { schedule -> showDeleteConfirmation(schedule) }
        )
        scheduleRecyclerView.layoutManager = LinearLayoutManager(this)
        scheduleRecyclerView.adapter = scheduleAdapter
    }

    private fun loadSchedulesFromFirebase() {
        val userId = firebaseManager.getCurrentUserId()
        if (userId == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        scheduleList.clear()
        scheduleAdapter.notifyDataSetChanged()

        firebaseManager.getSchedules { schedules ->
            scheduleList.clear()
            scheduleList.addAll(schedules)
            scheduleAdapter.notifyDataSetChanged()

            // Update nextId to avoid conflicts
            nextId = (schedules.maxOfOrNull { it.id } ?: 0) + 1

            if (schedules.isEmpty()) {
                Toast.makeText(this, "No schedules yet. Add your first reminder!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupClickListeners() {
        menuIcon.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        addReminderBtn.setOnClickListener {
            showAddReminderDialog()
        }

        homeBtn.setOnClickListener {
            updateBottomNavSelection(homeBtn)
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }

        scheduleBtn.setOnClickListener {
            // Already on schedule screen
            Toast.makeText(this, "Already in Schedule", Toast.LENGTH_SHORT).show()
        }

        notificationBtn.setOnClickListener {
            updateBottomNavSelection(notificationBtn)
            val intent = Intent(this, NotificationActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun updateBottomNavSelection(selectedButton: ImageButton) {
        // UPDATED: Only 3 buttons now (removed addBtn)
        val buttons = listOf(homeBtn, scheduleBtn, notificationBtn)

        buttons.forEach { button ->
            val isSelected = button == selectedButton
            val backgroundRes = if (isSelected) {
                R.drawable.nav_button_selected
            } else {
                R.drawable.nav_button
            }

            button.setBackgroundResource(backgroundRes)

            val tintColor = ContextCompat.getColor(this, android.R.color.white)
            button.imageTintList = android.content.res.ColorStateList.valueOf(tintColor)
        }
    }

    private fun showAddReminderDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_reminder, null)
        val morningBtn = dialogView.findViewById<Button>(R.id.morningBtn)
        val afternoonBtn = dialogView.findViewById<Button>(R.id.afternoonBtn)
        val eveningBtn = dialogView.findViewById<Button>(R.id.eveningBtn)
        val timeDisplay = dialogView.findViewById<TextView>(R.id.timeDisplay)
        val confirmBtn = dialogView.findViewById<Button>(R.id.confirmBtn)

        var selectedPeriod = ""
        var selectedTime = ""

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        fun updateButtonSelection(selected: Button, vararg others: Button) {
            selected.animate()
                .scaleX(1.05f)
                .scaleY(1.05f)
                .setDuration(200)
                .start()
            selected.isSelected = true
            selected.setBackgroundColor(ContextCompat.getColor(this@ScheduleActivity, R.color.purple_600))

            others.forEach { button ->
                button.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start()
                button.isSelected = false
                button.setBackgroundColor(ContextCompat.getColor(this@ScheduleActivity, R.color.blue_500))
            }
        }

        morningBtn.setOnClickListener {
            selectedPeriod = "Morning Brush"
            updateButtonSelection(morningBtn, afternoonBtn, eveningBtn)
            timeDisplay.text = "_:_ _ PM - _:_ _ PM"
        }

        afternoonBtn.setOnClickListener {
            selectedPeriod = "Afternoon Brush"
            updateButtonSelection(afternoonBtn, morningBtn, eveningBtn)
            timeDisplay.text = "_:_ _ PM - _:_ _ PM"
        }

        eveningBtn.setOnClickListener {
            selectedPeriod = "Evening Brush"
            updateButtonSelection(eveningBtn, morningBtn, afternoonBtn)
            timeDisplay.text = "_:_ _ PM - _:_ _ PM"
        }

        timeDisplay.setOnClickListener {
            if (selectedPeriod.isEmpty()) {
                Toast.makeText(this, "Please select a period first", Toast.LENGTH_SHORT).show()
            } else {
                showTimePickerForDialog { time ->
                    selectedTime = time
                    timeDisplay.text = time
                }
            }
        }

        confirmBtn.setOnClickListener {
            if (selectedPeriod.isEmpty()) {
                Toast.makeText(this, "Please select a period", Toast.LENGTH_SHORT).show()
            } else if (selectedTime.isEmpty()) {
                Toast.makeText(this, "Please set time", Toast.LENGTH_SHORT).show()
            } else {
                val newSchedule = Schedule(nextId++, "MintyMinutes", "$selectedPeriod - $selectedTime")

                // Save to Firebase
                CoroutineScope(Dispatchers.Main).launch {
                    val success = firebaseManager.saveSchedule(newSchedule)
                    if (success) {
                        // Schedule notification alarm
                        notificationHelper.scheduleNotification(
                            scheduleId = newSchedule.id,
                            title = "MintyMinutes Reminder",
                            message = "Time to brush your teeth! ($selectedPeriod)",
                            time = selectedTime
                        )

                        dialog.dismiss()
                        showSuccessDialog("Reminder Saved!")
                    } else {
                        Toast.makeText(this@ScheduleActivity, "Failed to save reminder", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun showEditDialog(schedule: Schedule) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_reminder, null)
        val morningBtn = dialogView.findViewById<Button>(R.id.morningBtn)
        val afternoonBtn = dialogView.findViewById<Button>(R.id.afternoonBtn)
        val eveningBtn = dialogView.findViewById<Button>(R.id.eveningBtn)
        val timeDisplay = dialogView.findViewById<TextView>(R.id.timeDisplay)
        val confirmBtn = dialogView.findViewById<Button>(R.id.confirmBtn)

        var selectedPeriod = ""
        var selectedTime = ""

        // Parse existing schedule
        val scheduleTime = schedule.time
        when {
            scheduleTime.contains("Morning") -> {
                selectedPeriod = "Morning Brush"
                morningBtn.isSelected = true
                morningBtn.setBackgroundColor(ContextCompat.getColor(this, R.color.purple_600))
            }
            scheduleTime.contains("Afternoon") -> {
                selectedPeriod = "Afternoon Brush"
                afternoonBtn.isSelected = true
                afternoonBtn.setBackgroundColor(ContextCompat.getColor(this, R.color.purple_600))
            }
            scheduleTime.contains("Evening") -> {
                selectedPeriod = "Evening Brush"
                eveningBtn.isSelected = true
                eveningBtn.setBackgroundColor(ContextCompat.getColor(this, R.color.purple_600))
            }
        }

        // Extract time
        val timeParts = scheduleTime.split(" - ")
        if (timeParts.size >= 2) {
            selectedTime = timeParts.drop(1).joinToString(" - ")
            timeDisplay.text = selectedTime
        }

        confirmBtn.text = "Update"

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        fun updateButtonSelection(selected: Button, vararg others: Button) {
            selected.animate()
                .scaleX(1.05f)
                .scaleY(1.05f)
                .setDuration(200)
                .start()
            selected.isSelected = true
            selected.setBackgroundColor(ContextCompat.getColor(this@ScheduleActivity, R.color.purple_600))

            others.forEach { button ->
                button.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start()
                button.isSelected = false
                button.setBackgroundColor(ContextCompat.getColor(this@ScheduleActivity, R.color.blue_500))
            }
        }

        morningBtn.setOnClickListener {
            selectedPeriod = "Morning Brush"
            updateButtonSelection(morningBtn, afternoonBtn, eveningBtn)
        }

        afternoonBtn.setOnClickListener {
            selectedPeriod = "Afternoon Brush"
            updateButtonSelection(afternoonBtn, morningBtn, eveningBtn)
        }

        eveningBtn.setOnClickListener {
            selectedPeriod = "Evening Brush"
            updateButtonSelection(eveningBtn, morningBtn, afternoonBtn)
        }

        timeDisplay.setOnClickListener {
            if (selectedPeriod.isEmpty()) {
                Toast.makeText(this, "Please select a period first", Toast.LENGTH_SHORT).show()
            } else {
                showTimePickerForDialog { time ->
                    selectedTime = time
                    timeDisplay.text = time
                }
            }
        }

        confirmBtn.setOnClickListener {
            if (selectedPeriod.isEmpty()) {
                Toast.makeText(this, "Please select a period", Toast.LENGTH_SHORT).show()
            } else if (selectedTime.isEmpty()) {
                Toast.makeText(this, "Please set time", Toast.LENGTH_SHORT).show()
            } else {
                val updatedSchedule = Schedule(schedule.id, "MintyMinutes", "$selectedPeriod - $selectedTime")

                CoroutineScope(Dispatchers.Main).launch {
                    val success = firebaseManager.saveSchedule(updatedSchedule)
                    if (success) {
                        // Reschedule notification
                        notificationHelper.cancelNotification(schedule.id)
                        notificationHelper.scheduleNotification(
                            scheduleId = updatedSchedule.id,
                            title = "MintyMinutes Reminder",
                            message = "Time to brush your teeth! ($selectedPeriod)",
                            time = selectedTime
                        )

                        dialog.dismiss()
                        showSuccessDialog("Reminder Updated!")
                    } else {
                        Toast.makeText(this@ScheduleActivity, "Failed to update reminder", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun showDeleteConfirmation(schedule: Schedule) {
        AlertDialog.Builder(this)
            .setTitle("Delete Reminder")
            .setMessage("Are you sure you want to delete this reminder?")
            .setPositiveButton("Delete") { _, _ ->
                // Cancel the notification alarm
                notificationHelper.cancelNotification(schedule.id)

                // Delete from Firebase
                CoroutineScope(Dispatchers.Main).launch {
                    val success = firebaseManager.deleteSchedule(schedule.id)
                    if (success) {
                        showSuccessDialog("Reminder Deleted!")
                    } else {
                        Toast.makeText(this@ScheduleActivity, "Failed to delete reminder", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSuccessDialog(message: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_success, null)
        val dialogMessage = dialogView.findViewById<TextView>(R.id.dialogMessage)
        val okButton = dialogView.findViewById<Button>(R.id.okButton)

        dialogMessage.text = message

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        okButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showTimePickerForDialog(onTimeSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(this, { _, startHour, startMinute ->
            TimePickerDialog(this, { _, endHour, endMinute ->
                val startTime = formatTime(startHour, startMinute)
                val endTime = formatTime(endHour, endMinute)
                onTimeSelected("$startTime - $endTime")
            }, hour, minute, false).show()
        }, hour, minute, false).show()
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val amPm = if (hour >= 12) "PM" else "AM"
        val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        val displayMinute = String.format("%02d", minute)
        return "$displayHour:$displayMinute $amPm"
    }

    private fun setupSidebarMenu() {
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                    finish()
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_history -> {
                    try {
                        val intent = Intent(this, HistoryActivity::class.java)
                        startActivity(intent)
                        finish()
                        drawerLayout.closeDrawer(GravityCompat.START)
                    } catch (e: Exception) {
                        Toast.makeText(this, "Error opening history", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                R.id.nav_settings -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                    finish()
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_profile -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    startActivity(intent)
                    finish()
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_logout -> {
                    val sessionManager = UserSessionManager(this)
                    sessionManager.clearSession()

                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                    Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}