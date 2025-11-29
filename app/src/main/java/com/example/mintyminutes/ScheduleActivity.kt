package com.example.mintyminutes

import androidx.core.content.ContextCompat
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import java.util.*

class ScheduleActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var menuIcon: ImageView
    private lateinit var addReminderBtn: Button
    private lateinit var scheduleRecyclerView: RecyclerView
    private lateinit var homeBtn: ImageButton
    private lateinit var addBtn: ImageButton
    private lateinit var scheduleBtn: ImageButton
    private lateinit var notificationBtn: ImageButton

    private lateinit var scheduleAdapter: ScheduleAdapter
    private val scheduleList = mutableListOf<Schedule>()
    private var nextId = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule)

        initializeViews()
        setupRecyclerView()
        setupClickListeners()
        setupSidebarMenu()
        loadSampleData()
    }

    private fun initializeViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        menuIcon = findViewById(R.id.menuIcon)
        addReminderBtn = findViewById(R.id.addReminderBtn)
        scheduleRecyclerView = findViewById(R.id.scheduleRecyclerView)
        homeBtn = findViewById(R.id.homeBtn)
        addBtn = findViewById(R.id.addBtn)
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

    private fun setupClickListeners() {
        // Menu icon
        menuIcon.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Add Reminder button
        addReminderBtn.setOnClickListener {
            showAddReminderDialog()
        }

        // Bottom navigation
        homeBtn.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }

        addBtn.setOnClickListener {
            Toast.makeText(this, "Add new session", Toast.LENGTH_SHORT).show()
        }

        scheduleBtn.setOnClickListener {
            Toast.makeText(this, "Already in Schedule", Toast.LENGTH_SHORT).show()
        }

        notificationBtn.setOnClickListener {
            val intent = Intent(this, NotificationActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadSampleData() {
        scheduleList.add(Schedule(nextId++, "MintyMinutes", "Morning Brush - 8:00 AM"))
        scheduleList.add(Schedule(nextId++, "MintyMinutes", "Afternoon Brush - 1:00 PM"))
        scheduleList.add(Schedule(nextId++, "MintyMinutes", "Evening Brush - 8:00 PM"))
        scheduleAdapter.notifyDataSetChanged()
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
            // Animate selected button
            selected.animate()
                .scaleX(1.05f)
                .scaleY(1.05f)
                .setDuration(200)
                .start()
            selected.isSelected = true
            selected.setBackgroundColor(ContextCompat.getColor(this@ScheduleActivity, R.color.purple_600))

            // Reset other buttons
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
                scheduleAdapter.addItem(newSchedule)
                dialog.dismiss()
                showSuccessDialog("Reminder Saved!")
            }
        }

        dialog.show()
    }

    private fun showEditDialog(schedule: Schedule) {
        Toast.makeText(this, "Edit ${schedule.time}", Toast.LENGTH_SHORT).show()
        // TODO: Implement edit functionality
    }

    private fun showDeleteConfirmation(schedule: Schedule) {
        AlertDialog.Builder(this)
            .setTitle("Delete Reminder")
            .setMessage("Are you sure you want to delete this reminder?")
            .setPositiveButton("Delete") { _, _ ->
                scheduleAdapter.removeItem(schedule)
                showSuccessDialog("Reminder Deleted!")
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
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_settings -> {
                    Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show()
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_profile -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    startActivity(intent)
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_logout -> {
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