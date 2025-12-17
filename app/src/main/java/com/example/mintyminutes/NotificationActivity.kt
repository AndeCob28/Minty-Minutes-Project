package com.example.mintyminutes

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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

class NotificationActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var menuIcon: ImageView
    private lateinit var notificationRecyclerView: RecyclerView

    // Bottom navigation - REMOVED addBtn
    private lateinit var homeBtn: ImageButton
    private lateinit var scheduleBtn: ImageButton
    private lateinit var notificationBtn: ImageButton

    private lateinit var notificationAdapter: NotificationAdapter
    private val notificationList = mutableListOf<Notification>()

    // Firebase Manager
    private val firebaseManager = FirebaseManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        initializeViews()
        setupRecyclerView()
        setupClickListeners()
        setupSidebarMenu()
        updateBottomNavSelection(notificationBtn)
        loadNotificationsFromFirebase()
    }

    private fun initializeViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        menuIcon = findViewById(R.id.menuIcon)
        notificationRecyclerView = findViewById(R.id.notificationRecyclerView)

        // Bottom navigation - REMOVED addBtn initialization
        homeBtn = findViewById(R.id.homeBtn)
        scheduleBtn = findViewById(R.id.scheduleBtn)
        notificationBtn = findViewById(R.id.notificationBtn)
    }

    private fun setupRecyclerView() {
        notificationAdapter = NotificationAdapter(
            notificationList,
            onDeleteClick = { notification -> showDeleteConfirmation(notification) }
        )
        notificationRecyclerView.layoutManager = LinearLayoutManager(this)
        notificationRecyclerView.adapter = notificationAdapter
    }

    private fun loadNotificationsFromFirebase() {
        val userId = firebaseManager.getCurrentUserId()
        if (userId == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        notificationList.clear()
        notificationAdapter.notifyDataSetChanged()

        firebaseManager.getNotifications { notifications ->
            runOnUiThread {
                notificationList.clear()
                notificationList.addAll(notifications)
                notificationAdapter.notifyDataSetChanged()

                if (notifications.isEmpty()) {
                    Toast.makeText(this, "No notifications yet", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupClickListeners() {
        menuIcon.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        homeBtn.setOnClickListener {
            updateBottomNavSelection(homeBtn)
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }

        scheduleBtn.setOnClickListener {
            updateBottomNavSelection(scheduleBtn)
            val intent = Intent(this, ScheduleActivity::class.java)
            startActivity(intent)
            finish()
        }

        notificationBtn.setOnClickListener {
            // Already on notifications screen
            Toast.makeText(this, "Already in Notifications", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateBottomNavSelection(selectedButton: ImageButton) {
        // UPDATED: Only 3 buttons now
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

    private fun showDeleteConfirmation(notification: Notification) {
        AlertDialog.Builder(this)
            .setTitle("Delete Notification")
            .setMessage("Are you sure you want to delete this notification?")
            .setPositiveButton("Delete") { _, _ ->
                CoroutineScope(Dispatchers.Main).launch {
                    val success = firebaseManager.deleteNotification(notification.id)
                    if (success) {
                        notificationAdapter.removeItem(notification)
                        showSuccessDialog("Notification Deleted!")
                    } else {
                        Toast.makeText(
                            this@NotificationActivity,
                            "Failed to delete notification",
                            Toast.LENGTH_SHORT
                        ).show()
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