package com.example.mintyminutes

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog

class SettingsActivity : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var dailyRemindersSwitch: Switch
    private lateinit var exportDataButton: Button
    private lateinit var deleteAccountButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        initializeViews()
        setupClickListeners()
    }

    private fun initializeViews() {
        backButton = findViewById(R.id.backButton)
        dailyRemindersSwitch = findViewById(R.id.dailyRemindersSwitch)
        deleteAccountButton = findViewById(R.id.deleteAccountButton)

        // Removed views that no longer exist in the layout:
        // - notificationSoundsSwitch
        // - vibrationSwitch
        // - themeSpinner
        // - languageSpinner
        // - cloudBackupSwitch
    }

    private fun setupClickListeners() {
        // Back button to return to Home
        backButton.setOnClickListener {
            finish()
        }

        // Switch listeners - only dailyRemindersSwitch remains
        dailyRemindersSwitch.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(this, "Daily reminders ${if (isChecked) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
        }

        // Removed switch listeners for:
        // - notificationSoundsSwitch
        // - vibrationSwitch
        // - cloudBackupSwitch

        // Button listeners
        exportDataButton.setOnClickListener {
            Toast.makeText(this, "Exporting data...", Toast.LENGTH_SHORT).show()
            // Add your export data logic here
        }

        deleteAccountButton.setOnClickListener {
            showDeleteAccountConfirmation()
        }
    }

    // Removed setupSpinners() method since spinners no longer exist in the layout

    private fun showDeleteAccountConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
            .setPositiveButton("Delete") { dialog, which ->
                // Add your delete account logic here
                Toast.makeText(this, "Account deletion requested", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}