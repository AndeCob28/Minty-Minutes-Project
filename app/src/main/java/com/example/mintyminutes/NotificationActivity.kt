package com.example.mintyminutes

import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Button
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView

class NotificationActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var menuIcon: ImageView
    private lateinit var notificationRecyclerView: RecyclerView
    private lateinit var homeBtn: ImageButton
    private lateinit var addBtn: ImageButton
    private lateinit var scheduleBtn: ImageButton
    private lateinit var notificationBtn: ImageButton

    private lateinit var notificationAdapter: NotificationAdapter
    private val notificationList = mutableListOf<Notification>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        initializeViews()
        setupRecyclerView()
        setupClickListeners()
        setupSidebarMenu()
        loadSampleNotifications()
    }

    private fun initializeViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        menuIcon = findViewById(R.id.menuIcon)
        notificationRecyclerView = findViewById(R.id.notificationRecyclerView)
        homeBtn = findViewById(R.id.homeBtn)
        addBtn = findViewById(R.id.addBtn)
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

    private fun setupClickListeners() {
        menuIcon.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        homeBtn.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }

        addBtn.setOnClickListener {
            Toast.makeText(this, "Add new session", Toast.LENGTH_SHORT).show()
        }

        scheduleBtn.setOnClickListener {
            val intent = Intent(this, ScheduleActivity::class.java)
            startActivity(intent)
        }

        notificationBtn.setOnClickListener {
            Toast.makeText(this, "Already in Notifications", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadSampleNotifications() {
        notificationList.add(Notification(1, "MintyMinutes", "Toothbrush Reminder"))
        notificationList.add(Notification(2, "MintyMinutes", "Toothbrush Reminder"))
        notificationList.add(Notification(3, "MintyMinutes", "Toothbrush Reminder"))
        notificationList.add(Notification(4, "MintyMinutes", "Toothbrush Reminder"))
        notificationList.add(Notification(5, "MintyMinutes", "Toothbrush Reminder"))
        notificationList.add(Notification(6, "MintyMinutes", "Toothbrush Reminder"))
        notificationList.add(Notification(7, "MintyMinutes", "Toothbrush Reminder"))
        notificationList.add(Notification(8, "MintyMinutes", "Toothbrush Reminder"))
        notificationAdapter.notifyDataSetChanged()
    }

    private fun showDeleteConfirmation(notification: Notification) {
        AlertDialog.Builder(this)
            .setTitle("Delete Notification")
            .setMessage("Are you sure you want to delete this notification?")
            .setPositiveButton("Delete") { _, _ ->
                notificationAdapter.removeItem(notification)
                showSuccessDialog("Notification Deleted!")
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

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}