package com.example.mintyminutes

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var emailLayout: TextInputLayout
    private lateinit var emailInput: TextInputEditText
    private lateinit var sendResetButton: MaterialButton
    private lateinit var backToLoginText: TextView

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        initViews()
        setupListeners()
    }

    private fun initViews() {
        emailLayout = findViewById(R.id.emailLayout)
        emailInput = findViewById(R.id.emailInput)
        sendResetButton = findViewById(R.id.sendResetButton)
        backToLoginText = findViewById(R.id.backToLogin)
    }

    private fun setupListeners() {
        // Email validation
        emailInput.doOnTextChanged { text, _, _, _ ->
            validateEmail(text.toString())
        }

        // Send reset link button
        sendResetButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            if (validateEmail(email)) {
                sendPasswordResetEmail(email)
            }
        }

        // Back to login
        backToLoginText.setOnClickListener {
            finish() // Go back to LoginActivity
        }
    }

    private fun validateEmail(email: String): Boolean {
        return if (email.isEmpty()) {
            emailLayout.error = "Email is required"
            false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.error = "Invalid email format"
            false
        } else {
            emailLayout.error = null
            true
        }
    }

    private fun sendPasswordResetEmail(email: String) {
        showLoading(true)

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                showLoading(false)

                if (task.isSuccessful) {
                    // Password reset email sent successfully
                    Toast.makeText(
                        this,
                        "Password reset link sent to your email",
                        Toast.LENGTH_LONG
                    ).show()

                    // Optionally, navigate back to login after a delay
                    // Handler(Looper.getMainLooper()).postDelayed({
                    //     finish()
                    // }, 3000)
                } else {
                    // Handle errors
                    val errorMessage = when {
                        task.exception?.message?.contains("user-not-found") == true -> {
                            "No account found with this email address"
                        }
                        task.exception?.message?.contains("invalid-email") == true -> {
                            "Invalid email address"
                        }
                        task.exception?.message?.contains("network-request-failed") == true -> {
                            "Network error. Please check your internet connection"
                        }
                        else -> {
                            "Failed to send reset link: ${task.exception?.message}"
                        }
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                Toast.makeText(
                    this,
                    "Error: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun showLoading(show: Boolean) {
        sendResetButton.isEnabled = !show
        sendResetButton.text = if (show) "SENDING..." else "SEND RESET LINK"
    }

    // Optional: Pre-fill email if coming from login with email
    override fun onResume() {
        super.onResume()
        val loginEmail = intent.getStringExtra("email")
        loginEmail?.let {
            emailInput.setText(it)
        }
    }
}