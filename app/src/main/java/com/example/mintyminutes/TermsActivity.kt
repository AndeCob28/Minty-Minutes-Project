package com.example.mintyminutes

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView

class TermsActivity : AppCompatActivity() {

    // Add this companion object with the constant
    companion object {
        const val EXTRA_TERMS_ACCEPTED = "terms_accepted"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_terms)

        val backButton: ImageView = findViewById(R.id.backButton)
        val acceptButton: Button = findViewById(R.id.acceptButton)

        // Back button - returns to RegisterActivity without accepting
        backButton.setOnClickListener {
            // Return without accepting
            setResult(RESULT_CANCELED)
            finish()
        }

        // Accept button - returns with acceptance result
        acceptButton.setOnClickListener {
            val resultIntent = Intent().apply {
                putExtra(EXTRA_TERMS_ACCEPTED, true)
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }

    override fun finish() {
        super.finish()
        // Optional: Add a slide animation
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}