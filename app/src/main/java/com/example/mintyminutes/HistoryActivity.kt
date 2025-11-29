package com.example.mintyminutes

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.widget.ImageView
import android.widget.TextView
import android.widget.LinearLayout

class HistoryActivity : AppCompatActivity() {

    private lateinit var titleText: TextView
    private lateinit var backButton: ImageView
    private lateinit var weeklySummaryButton: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        initializeViews()
        setupUI()
        setupClickListeners()
    }

    private fun initializeViews() {
        titleText = findViewById(R.id.titleText)
        backButton = findViewById(R.id.backButton)
        weeklySummaryButton = findViewById(R.id.weeklySummaryButton)
        // Removed statisticsButton since it's not in the layout
    }

    private fun setupUI() {
        titleText.text = "BRUSHING HISTORY"
        // Setup your history list and analytics here
    }

    private fun setupClickListeners() {
        // Back button click listener - navigate back to Home
        backButton.setOnClickListener {
            finish() // This will close HistoryActivity and return to HomeActivity
        }

        // Weekly Summary button click listener - navigate to WeeklyStatisticsActivity
        weeklySummaryButton.setOnClickListener {
            val intent = Intent(this, WeeklyStatisticsActivity::class.java)
            startActivity(intent)
            // Optional: Add transition animation
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        // Removed statisticsButton click listener since the button doesn't exist
    }

    override fun onBackPressed() {
        // Handle the back button press (same as clicking the back arrow)
        super.onBackPressed()
        // Optional: Add a smooth transition
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}