package com.example.mintyminutes

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView

class HomeActivity : AppCompatActivity() {

    private var currentProgress = 1
    private var isDeviceConnected = false // Default to disconnected
    private var userName: String = "User" // Default name

    // UI components
    private lateinit var progressText: TextView
    private lateinit var welcomeText: TextView
    private lateinit var subtitleText: TextView
    private lateinit var reminderText: TextView
    private lateinit var progressContainer: LinearLayout // Changed from progressCircle
    private lateinit var deviceStatusCard: LinearLayout
    private lateinit var deviceStatusText: TextView
    private lateinit var wifiIcon: ImageView
    private lateinit var addSessionBtn: Button
    // Removed deviceToggleBtn
    private lateinit var homeBtn: ImageButton
    private lateinit var addBtn: ImageButton
    private lateinit var calendarBtn: ImageButton
    private lateinit var notificationBtn: ImageButton

    // Sidebar components
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var menuIcon: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Get user name from intent
        userName = intent.getStringExtra("user_name") ?: "User"

        initializeViews()
        setupUI()
        setupClickListeners()
        setupSidebarMenu()
        updateProgressDisplay(currentProgress) // Changed from updateProgressCircle
        updateDeviceStatus(isDeviceConnected)

        // TODO: Add IoT device connection logic here
        // This will update the device status based on actual IoT connection
    }

    private fun initializeViews() {
        progressText = findViewById(R.id.progressText)
        welcomeText = findViewById(R.id.welcomeText)
        subtitleText = findViewById(R.id.subtitleText)
        reminderText = findViewById(R.id.reminderText)
        progressContainer = findViewById(R.id.progressContainer) // Changed from progressCircle
        deviceStatusCard = findViewById(R.id.deviceStatusCard)
        deviceStatusText = findViewById(R.id.deviceStatusText)
        wifiIcon = findViewById(R.id.wifiIcon)
        addSessionBtn = findViewById(R.id.addSessionBtn)
        // Removed deviceToggleBtn initialization
        homeBtn = findViewById(R.id.homeBtn)
        addBtn = findViewById(R.id.addBtn)
        calendarBtn = findViewById(R.id.calendarBtn)
        notificationBtn = findViewById(R.id.notificationBtn)

        // Initialize sidebar components
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        menuIcon = findViewById(R.id.menuIcon)
    }

    private fun setupUI() {
        // Set initial progress text - will be updated from database
        progressText.text = "Loading today's progress..."

        // Set user name
        welcomeText.text = "Hello, $userName!"
        subtitleText.text = "I'm glad to see you back"

        // Set reminder text
        reminderText.text = "Remember to brush 3 times everyday."
    }

    private fun setupClickListeners() {
        // Menu icon click listener to open sidebar
        menuIcon.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        addSessionBtn.setOnClickListener {
            if (currentProgress < 3) {
                currentProgress++
                updateProgressDisplay(currentProgress)
                // TODO: Update this in Firebase database
                // progressText.text = "$currentProgress / 3"

                val message = when (currentProgress) {
                    2 -> "Great! One more session to go!"
                    3 -> "Excellent! You've completed all sessions today!"
                    else -> "Session added!"
                }
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "You've already completed all sessions today!", Toast.LENGTH_SHORT).show()
            }
        }

        // Removed deviceToggleBtn click listener

        homeBtn.setOnClickListener {
            Toast.makeText(this, "Home", Toast.LENGTH_SHORT).show()
        }

        addBtn.setOnClickListener {
            Toast.makeText(this, "Add new session", Toast.LENGTH_SHORT).show()
        }

        calendarBtn.setOnClickListener {
            val intent = Intent(this, ScheduleActivity::class.java)
            startActivity(intent)
        }

        notificationBtn.setOnClickListener {
            Toast.makeText(this, "Notifications", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSidebarMenu() {
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    // Already in Home, just close drawer
                    Toast.makeText(this, "Already in Home", Toast.LENGTH_SHORT).show()
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_history -> {
                    // Add debug logging
                    println("DEBUG: History button clicked")
                    try {
                        val intent = Intent(this, HistoryActivity::class.java)
                        startActivity(intent)
                        drawerLayout.closeDrawer(GravityCompat.START)
                        println("DEBUG: HistoryActivity started successfully")
                    } catch (e: Exception) {
                        println("DEBUG: Error starting HistoryActivity: ${e.message}")
                        e.printStackTrace()
                        Toast.makeText(this, "Error opening history: ${e.message}", Toast.LENGTH_SHORT).show()
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
                    // Logout and go back to LoginActivity
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                    Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> {
                    println("DEBUG: Unknown menu item clicked: ${menuItem.itemId}")
                    false
                }
            }
        }
    }

    // Updated method to handle progress display (replaces updateProgressCircle)
    private fun updateProgressDisplay(progress: Int) {
        // TODO: Update this method to display progress data from database
        // For now, showing basic progress info
        val progressInfo = when (progress) {
            1 -> "1 session completed today\n2 sessions remaining"
            2 -> "2 sessions completed today\n1 session remaining"
            3 -> "All 3 sessions completed today!\nGreat job!"
            else -> "No sessions recorded today"
        }
        progressText.text = progressInfo
    }

    // Method to update progress from database (to be called when data is fetched)
    fun updateProgressFromDatabase(sessionsCompleted: Int, totalSessions: Int) {
        currentProgress = sessionsCompleted
        progressText.text = "$sessionsCompleted / $totalSessions sessions completed"

        // You can also update the visual representation here
        // For example, change background color or add progress indicators
        when (sessionsCompleted) {
            0 -> progressContainer.setBackgroundColor(ContextCompat.getColor(this, R.color.red_400))
            1, 2 -> progressContainer.setBackgroundColor(ContextCompat.getColor(this, R.color.yellow_400))
            3 -> progressContainer.setBackgroundColor(ContextCompat.getColor(this, R.color.green_400))
        }
    }

    // Method to update device status from IoT connection
    fun updateDeviceConnectionStatus(connected: Boolean) {
        isDeviceConnected = connected
        updateDeviceStatus(connected)

        if (connected) {
            Toast.makeText(this, "IoT device connected", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "IoT device disconnected", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateDeviceStatus(connected: Boolean) {
        if (connected) {
            deviceStatusCard.setBackgroundColor(
                ContextCompat.getColor(this, R.color.green_400)
            )
            deviceStatusText.text = "Device Connected"
            wifiIcon.setImageResource(R.drawable.ic_wifi)
        } else {
            deviceStatusCard.setBackgroundColor(
                ContextCompat.getColor(this, R.color.red_400)
            )
            deviceStatusText.text = "Device Disconnected"
            wifiIcon.setImageResource(R.drawable.ic_wifi_off)
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