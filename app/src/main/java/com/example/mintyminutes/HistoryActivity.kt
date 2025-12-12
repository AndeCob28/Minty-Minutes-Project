package com.example.mintyminutes

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.widget.ImageView
import android.widget.TextView
import android.widget.LinearLayout
import java.text.SimpleDateFormat
import java.util.*


class HistoryActivity : AppCompatActivity() {

    private lateinit var titleText: TextView
    private lateinit var backButton: ImageView
    private lateinit var weeklySummaryButton: LinearLayout
    private lateinit var totalTimeTextView: TextView
    private lateinit var sessionCountTextView: TextView
    private lateinit var firebaseManager: FirebaseManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        initializeViews()
        setupUI()
        setupClickListeners()
        fetchTodayData()
    }

    private fun initializeViews() {
        titleText = findViewById(R.id.titleText)
        backButton = findViewById(R.id.backButton)
        weeklySummaryButton = findViewById(R.id.weeklySummaryButton)

        // Initialize the TextViews for displaying data
        // We'll need to add IDs to the white_shape LinearLayouts in XML
        // For now, let's create them programmatically or update XML
        totalTimeTextView = TextView(this)
        sessionCountTextView = TextView(this)

        firebaseManager = FirebaseManager()
    }

    private fun setupUI() {
        titleText.text = "BRUSHING HISTORY"

        // We'll update this in fetchTodayData()
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        weeklySummaryButton.setOnClickListener {
            val intent = Intent(this, WeeklyStatisticsActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun fetchTodayData() {
        // Fetch today's brushing sessions
        fetchTodaySessions()

        // Fetch today's total brushing time
        fetchTodayTotalTime()
    }

    private fun fetchTodaySessions() {
        firebaseManager.getTodaySessions { sessionCount ->
            runOnUiThread {
                // Update session count display
                // For now, let's just log it and we'll update the view later
                println("DEBUG: Today's sessions: $sessionCount")

                // Find the session TextView and update it
                // First, we need to add proper views in XML or find them programmatically
                val sessionContainer = findViewById<LinearLayout>(R.id.sessionContainer)
                if (sessionContainer != null) {
                    // Find or create TextView in the white_shape layout
                    val sessionText = TextView(this).apply {
                        text = "$sessionCount\nSessions"
                        textSize = 18f
                        textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                        setTextColor(resources.getColor(android.R.color.black, null))
                    }
                    sessionContainer.removeAllViews()
                    sessionContainer.addView(sessionText)
                }

                // Alternatively, if you have a TextView in XML with id sessionTextView
                findViewById<TextView>(R.id.sessionTextView)?.text = "$sessionCount Sessions"
            }
        }
    }

    private fun fetchTodayTotalTime() {
        firebaseManager.getTodayTotalTime { totalSeconds ->
            runOnUiThread {
                updateTotalTimeDisplay(totalSeconds)
            }
        }
    }

    private fun updateTotalTimeDisplay(totalSeconds: Int) {
        // Convert seconds to minutes and seconds
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60

        val timeText = if (totalSeconds > 0) {
            if (minutes > 0) {
                "$minutes min\n$seconds sec"
            } else {
                "$seconds seconds"
            }
        } else {
            "No brushing\ntoday"
        }

        // Find the total time TextView and update it
        val totalTimeContainer = findViewById<LinearLayout>(R.id.totalTimeContainer)
        if (totalTimeContainer != null) {
            val timeTextView = TextView(this).apply {
                text = timeText
                textSize = 18f
                textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                setTextColor(resources.getColor(android.R.color.black, null))
            }
            totalTimeContainer.removeAllViews()
            totalTimeContainer.addView(timeTextView)
        }

        // Alternatively, if you have a TextView in XML with id totalTimeTextView
        findViewById<TextView>(R.id.totalTimeTextView)?.text = timeText
    }

    private fun showDefaultTime() {
        runOnUiThread {
            findViewById<TextView>(R.id.totalTimeTextView)?.text = "0 min\n0 sec"
            findViewById<TextView>(R.id.sessionTextView)?.text = "0 Sessions"
        }
    }

    private fun getTodayDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun fetchSessionHistory() {
        firebaseManager.getSessionHistory(limit = 10) { sessions ->
            runOnUiThread {
                // Update a RecyclerView or ListView with session history
                println("DEBUG: Loaded ${sessions.size} sessions")

                // You could create a simple list display like this:
                sessions.forEach { session ->
                    val date = session["date"] as? String ?: ""
                    val duration = session["duration"] as? Int ?: 0
                    val score = session["score"] as? Int ?: 0

                    println("Session: $date - ${duration}sec - Score: $score")
                }
            }
        }
    }
}