package com.example.mintyminutes

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView

class WeeklyStatisticsActivity : AppCompatActivity() {

    private lateinit var titleText: TextView
    private lateinit var backButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weeklystatistics)

        initializeViews()
        setupUI()
        setupClickListeners()
    }

    private fun initializeViews() {
        titleText = findViewById(R.id.titleText)
        backButton = findViewById(R.id.backButton)
        // Initialize other views from your weekly statistics layout as needed
    }

    private fun setupUI() {
        titleText.text = "BRUSHING HISTORY"
        // Setup your weekly statistics data here
    }

    private fun setupClickListeners() {
        // Back button click listener - navigate back to History
        backButton.setOnClickListener {
            finish() // This will close WeeklyStatisticsActivity and return to HistoryActivity
        }
    }

    override fun onBackPressed() {
        // Handle the back button press (same as clicking the back arrow)
        super.onBackPressed()
        // Optional: Add a smooth transition
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}