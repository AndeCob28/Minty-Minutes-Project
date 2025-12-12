package com.example.mintyminutes

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*

class WeeklyStatisticsActivity : AppCompatActivity() {

    private lateinit var titleText: TextView
    private lateinit var backButton: ImageView
    private lateinit var firebaseManager: FirebaseManager

    // Array to hold the time TextViews
    private val timeTextViews = arrayOfNulls<TextView>(7)

    // Days of the week in order (Monday to Sunday)
    private val daysOfWeek = listOf(
        "Monday", "Tuesday", "Wednesday", "Thursday",
        "Friday", "Saturday", "Sunday"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weeklystatistics)

        initializeViews()
        setupUI()
        setupClickListeners()
        fetchWeeklyData()
    }

    private fun initializeViews() {
        titleText = findViewById(R.id.titleText)
        backButton = findViewById(R.id.backButton)
        firebaseManager = FirebaseManager()

        // Initialize time TextViews
        timeTextViews[0] = findViewById(R.id.monday_time)
        timeTextViews[1] = findViewById(R.id.tuesday_time)
        timeTextViews[2] = findViewById(R.id.wednesday_time)
        timeTextViews[3] = findViewById(R.id.thursday_time)
        timeTextViews[4] = findViewById(R.id.friday_time)
        timeTextViews[5] = findViewById(R.id.saturday_time)
        timeTextViews[6] = findViewById(R.id.sunday_time)
    }

    private fun setupUI() {
        titleText.text = "WEEKLY SUMMARY"
    }

    private fun fetchWeeklyData() {
        firebaseManager.getWeeklyData { weeklyData ->
            runOnUiThread {
                updateWeeklyTable(weeklyData)
            }
        }
    }

    private fun updateWeeklyTable(weeklyData: Map<String, Int>) {
        for (i in 0 until 7) {
            val day = daysOfWeek[i]
            val totalSeconds = weeklyData[day] ?: 0

            // Format the time display
            val displayText = formatTimeForDisplay(totalSeconds)
            timeTextViews[i]?.text = displayText

            // Optional: Add color coding based on brushing time
            updateTimeTextColor(timeTextViews[i], totalSeconds)
        }
    }

    private fun formatTimeForDisplay(totalSeconds: Int): String {
        return when {
            totalSeconds == 0 -> "No brushing"
            totalSeconds < 60 -> "${totalSeconds} sec"
            else -> {
                val minutes = totalSeconds / 60
                val seconds = totalSeconds % 60
                if (seconds > 0) {
                    "${minutes}m ${seconds}s"
                } else {
                    "${minutes} min"
                }
            }
        }
    }

    private fun updateTimeTextColor(textView: TextView?, totalSeconds: Int) {
        textView?.setTextColor(when {
            totalSeconds >= 120 -> getColor(R.color.good_brushing) // 2+ minutes - good
            totalSeconds >= 60 -> getColor(R.color.average_brushing) // 1-2 minutes - average
            totalSeconds > 0 -> getColor(R.color.poor_brushing) // less than 1 minute - poor
            else -> getColor(R.color.no_brushing) // no brushing
        })
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}