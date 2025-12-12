package com.example.mintyminutes

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeActivity : AppCompatActivity() {

    private var currentProgress = 0
    private var isDeviceConnected = false
    private var userName: String = "User"
    private var batteryLevel: Int = 0

    // Firebase
    private lateinit var firebaseManager: FirebaseManager
    private val ioScope = CoroutineScope(Dispatchers.IO)

    // UI components - REMOVED old references
    private lateinit var welcomeText: TextView
    private lateinit var subtitleText: TextView
    private lateinit var reminderText: TextView
    private lateinit var deviceStatusCard: LinearLayout
    private lateinit var deviceStatusText: TextView
    private lateinit var wifiIcon: ImageView
    private lateinit var homeBtn: ImageButton
    private lateinit var addBtn: ImageButton
    private lateinit var calendarBtn: ImageButton
    private lateinit var notificationBtn: ImageButton

    // Circular progress views
    private lateinit var progressCircle: ImageView
    private lateinit var progressCountText: TextView
    private lateinit var progressLabelText: TextView
    private lateinit var morningDot: ImageView
    private lateinit var afternoonDot: ImageView
    private lateinit var eveningDot: ImageView

    // Sidebar components
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var menuIcon: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Initialize Firebase
        firebaseManager = FirebaseManager()

        // Check if user is logged in with Firebase
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        // Get user name from session manager
        val sessionManager = UserSessionManager(this)
        userName = sessionManager.getUserName()

        initializeViews()
        setupUI()
        setupClickListeners()
        setupSidebarMenu()
        updateBottomNavSelection(homeBtn)

        // Load data from Firebase
        loadTodayProgress()
        setupDeviceStatusListener()
    }

    override fun onResume() {
        super.onResume()
        // Refresh progress when activity resumes
        loadTodayProgress()
    }

    private fun initializeViews() {
        // Basic views
        welcomeText = findViewById(R.id.welcomeText)
        subtitleText = findViewById(R.id.subtitleText)
        reminderText = findViewById(R.id.reminderText)
        deviceStatusCard = findViewById(R.id.deviceStatusCard)
        deviceStatusText = findViewById(R.id.deviceStatusText)
        wifiIcon = findViewById(R.id.wifiIcon)
        homeBtn = findViewById(R.id.homeBtn)
        addBtn = findViewById(R.id.addBtn)
        calendarBtn = findViewById(R.id.calendarBtn)
        notificationBtn = findViewById(R.id.notificationBtn)

        // Circular progress views
        progressCircle = findViewById(R.id.progressCircle)
        progressCountText = findViewById(R.id.progressCountText)
        progressLabelText = findViewById(R.id.progressLabelText)
        morningDot = findViewById(R.id.morningDot)
        afternoonDot = findViewById(R.id.afternoonDot)
        eveningDot = findViewById(R.id.eveningDot)

        // Sidebar components
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        menuIcon = findViewById(R.id.menuIcon)
    }

    private fun setupUI() {
        welcomeText.text = "Hello, $userName!"
        subtitleText.text = "Track your brushing routine"
    }

    private fun setupClickListeners() {
        menuIcon.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        homeBtn.setOnClickListener {
            updateBottomNavSelection(homeBtn)
            // Refresh data
            loadTodayProgress()
        }

        addBtn.setOnClickListener {
            if (currentProgress < 3) {
                saveBrushingSession()
                updateBottomNavSelection(addBtn)
            } else {
                Toast.makeText(this, "You've completed all sessions today!", Toast.LENGTH_SHORT).show()
                updateBottomNavSelection(addBtn)
            }
        }

        calendarBtn.setOnClickListener {
            val intent = Intent(this, ScheduleActivity::class.java)
            startActivity(intent)
            updateBottomNavSelection(calendarBtn)
        }

        notificationBtn.setOnClickListener {
            val intent = Intent(this, NotificationActivity::class.java)
            startActivity(intent)
            updateBottomNavSelection(notificationBtn)
        }

        // Device status click - toggle connection
        deviceStatusCard.setOnClickListener {
            val newStatus = !isDeviceConnected
            updateDeviceStatusInFirebase(newStatus)
        }
    }

    private fun saveBrushingSession() {
        // Show loading
        addBtn.isEnabled = false

        // Change button text to show loading
        if (addBtn is Button) {
            (addBtn as Button).text = "Saving..."
        }

        ioScope.launch {
            try {
                // Determine session type based on current progress
                val sessionType = when (currentProgress) {
                    0 -> "morning"
                    1 -> "afternoon"
                    2 -> "evening"
                    else -> "general"
                }

                // Save to Firebase
                val success = firebaseManager.saveBrushingSession(sessionType)

                withContext(Dispatchers.Main) {
                    addBtn.isEnabled = true

                    // Reset button text
                    if (addBtn is Button) {
                        (addBtn as Button).text = "Add Session"
                    }

                    if (success) {
                        // Update UI locally
                        currentProgress++
                        updateProgressDisplay(currentProgress)

                        // Show success message
                        val message = when (currentProgress) {
                            1 -> "Morning session saved! 🌅"
                            2 -> "Afternoon session saved! ☀️"
                            3 -> "Evening session saved! 🌙 All done!"
                            else -> "Session saved!"
                        }
                        Toast.makeText(this@HomeActivity, message, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@HomeActivity, "Failed to save session", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    addBtn.isEnabled = true

                    // Reset button text
                    if (addBtn is Button) {
                        (addBtn as Button).text = "Add Session"
                    }

                    Toast.makeText(this@HomeActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadTodayProgress() {
        firebaseManager.getTodayProgress { progress ->
            progress?.let {
                runOnUiThread {
                    // Update progress count
                    val completedSessions = it["completedSessions"] as? Int ?: 0
                    currentProgress = completedSessions

                    // Update circular progress
                    updateProgressDisplay(completedSessions)

                    // Update session dots
                    updateSessionDots(
                        morningCompleted = it["morningCompleted"] as? Boolean ?: false,
                        afternoonCompleted = it["afternoonCompleted"] as? Boolean ?: false,
                        eveningCompleted = it["eveningCompleted"] as? Boolean ?: false
                    )
                }
            } ?: run {
                // No progress data yet
                runOnUiThread {
                    currentProgress = 0
                    updateProgressDisplay(0)
                    updateSessionDots(false, false, false)
                }
            }
        }
    }

    private fun setupDeviceStatusListener() {
        firebaseManager.listenToDeviceStatus { status ->
            status?.let {
                runOnUiThread {
                    isDeviceConnected = it["isConnected"] as? Boolean ?: false
                    batteryLevel = it["batteryLevel"] as? Int ?: 0

                    updateDeviceStatus(isDeviceConnected)

                    // Update status text with battery level if connected
                    if (isDeviceConnected) {
                        deviceStatusText.text = "Connected • $batteryLevel%"
                    } else {
                        deviceStatusText.text = "Device Disconnected"
                    }
                }
            }
        }
    }

    private fun updateDeviceStatusInFirebase(isConnected: Boolean) {
        ioScope.launch {
            val battery = if (isConnected) 85 else 0
            firebaseManager.updateDeviceStatus(isConnected, battery)

            withContext(Dispatchers.Main) {
                val message = if (isConnected) {
                    "Device connected!"
                } else {
                    "Device disconnected"
                }
                Toast.makeText(this@HomeActivity, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateBottomNavSelection(selectedButton: ImageButton) {
        val buttons = listOf(homeBtn, addBtn, calendarBtn, notificationBtn)

        buttons.forEach { button ->
            val isSelected = button == selectedButton
            val backgroundRes = if (isSelected) {
                R.drawable.nav_button_selected
            } else {
                R.drawable.nav_button
            }

            button.setBackgroundResource(backgroundRes)

            // Set tint color for all buttons (including Add button)
            val tintColor = ContextCompat.getColor(this, android.R.color.white)
            button.imageTintList = android.content.res.ColorStateList.valueOf(tintColor)
        }
    }

    // ==================== UI UPDATE METHODS ====================

    private fun updateProgressDisplay(progress: Int) {
        progressCountText.text = "$progress/3"

        // Update circular progress image
        val progressDrawable = when (progress) {
            0 -> R.drawable.circle_progress_0
            1 -> R.drawable.circle_progress_1
            2 -> R.drawable.circle_progress_2
            3 -> R.drawable.circle_progress_3
            else -> R.drawable.circle_progress_0
        }
        progressCircle.setImageResource(progressDrawable)

        // Update reminder text
        val reminder = when (progress) {
            0 -> "Start your first brushing session of the day!"
            1 -> "Great! 2 more sessions to complete today."
            2 -> "Almost there! One more session to go."
            3 -> "Excellent! All sessions completed today! 🎉"
            else -> "Track your brushing routine"
        }
        reminderText.text = reminder
    }

    private fun updateSessionDots(
        morningCompleted: Boolean,
        afternoonCompleted: Boolean,
        eveningCompleted: Boolean
    ) {
        val completedDrawable = ContextCompat.getDrawable(this, R.drawable.circle_filled)
        val emptyDrawable = ContextCompat.getDrawable(this, R.drawable.circle_empty)

        morningDot.setImageDrawable(if (morningCompleted) completedDrawable else emptyDrawable)
        afternoonDot.setImageDrawable(if (afternoonCompleted) completedDrawable else emptyDrawable)
        eveningDot.setImageDrawable(if (eveningCompleted) completedDrawable else emptyDrawable)
    }

    private fun updateDeviceStatus(connected: Boolean) {
        if (connected) {
            deviceStatusCard.setBackgroundColor(
                ContextCompat.getColor(this, R.color.green_400)
            )
            deviceStatusText.text = "Device Connected"
            wifiIcon.setImageResource(R.drawable.ic_wifi)
            wifiIcon.imageTintList = ContextCompat.getColorStateList(this, android.R.color.white)
        } else {
            deviceStatusCard.setBackgroundColor(
                ContextCompat.getColor(this, R.color.red_400)
            )
            deviceStatusText.text = "Device Disconnected"
            wifiIcon.setImageResource(R.drawable.ic_wifi_off)
            wifiIcon.imageTintList = ContextCompat.getColorStateList(this, android.R.color.white)
        }
    }

    private fun setupSidebarMenu() {
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_history -> {
                    try {
                        val intent = Intent(this, HistoryActivity::class.java)
                        startActivity(intent)
                        drawerLayout.closeDrawer(GravityCompat.START)
                    } catch (e: Exception) {
                        Toast.makeText(this, "Error opening history", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                R.id.nav_settings -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
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
                    // Clear session
                    val sessionManager = UserSessionManager(this)
                    sessionManager.clearSession()

                    // Firebase logout
                    FirebaseAuth.getInstance().signOut()

                    // Go to login
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