package com.example.mintyminutes

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var emailInput: TextInputEditText
    private lateinit var emailLayout: TextInputLayout
    private lateinit var resetButton: MaterialButton
    private lateinit var backToLogin: TextView
    private lateinit var progressIndicator: LinearProgressIndicator
    private lateinit var instructionText: TextView

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        // Initialize Firebase Auth
        auth = Firebase.auth

        initViews()
        setupListeners()
    }

    private fun initViews() {
        emailInput = findViewById(R.id.emailInput)
        emailLayout = findViewById(R.id.emailLayout)
        resetButton = findViewById(R.id.resetButton)
        backToLogin = findViewById(R.id.backToLogin)
        progressIndicator = findViewById(R.id.progressIndicator)
        instructionText = findViewById(R.id.instructionText)

        // Make sure progress indicator is initially hidden
        progressIndicator.visibility = View.GONE
    }

    private fun setupListeners() {
        resetButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            if (validateEmail(email)) {
                resetPassword(email)
            }
        }

        backToLogin.setOnClickListener {
            finish() // Go back to LoginActivity
        }
    }

    private fun validateEmail(email: String): Boolean {
        return when {
            email.isEmpty() -> {
                emailLayout.error = "Email is required"
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                emailLayout.error = "Please enter a valid email address"
                false
            }
            else -> {
                emailLayout.error = null
                true
            }
        }
    }

    private fun resetPassword(email: String) {
        // Show loading state
        showLoading(true)

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                showLoading(false)

                if (task.isSuccessful) {
                    // Password reset email sent successfully
                    showSuccessMessage(email)
                } else {
                    // Handle errors
                    val errorMessage = task.exception?.message ?: "Failed to send reset email"

                    when {
                        errorMessage.contains("user-not-found", ignoreCase = true) -> {
                            emailLayout.error = "No account found with this email"
                        }
                        errorMessage.contains("invalid-email", ignoreCase = true) -> {
                            emailLayout.error = "Invalid email address"
                        }
                        errorMessage.contains("network-request-failed", ignoreCase = true) -> {
                            Toast.makeText(this, "Network error. Check your connection.", Toast.LENGTH_LONG).show()
                        }
                        else -> {
                            Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
    }

    private fun showLoading(show: Boolean) {
        if (show) {
            progressIndicator.visibility = View.VISIBLE
            resetButton.isEnabled = false
            resetButton.text = "Sending..."
            instructionText.text = "Sending reset link to your email..."
        } else {
            progressIndicator.visibility = View.GONE
            resetButton.isEnabled = true
            resetButton.text = "Send Reset Link"
            instructionText.text = "Enter your email address and we'll send you a link to reset your password."
        }
    }

    private fun showSuccessMessage(email: String) {
        // Create a success message with instructions
        val successMessage = """
            ✅ Reset link sent to:
            $email
            
            Please check your email and follow the instructions to reset your password.
            
            The link will expire in 1 hour.
        """.trimIndent()

        // You could show this in a dialog or a success screen
        // For now, let's show a Toast and navigate back to login
        Toast.makeText(
            this,
            "Password reset email sent! Check your inbox.",
            Toast.LENGTH_LONG
        ).show()

        // Optional: Navigate back to login after a delay
        resetButton.postDelayed({
            finish()
        }, 2000)
    }
}