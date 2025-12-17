package com.example.mintyminutes

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import kotlinx.coroutines.*
class DeleteAccountActivity : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var titleText: TextView
    private lateinit var warningText: TextView
    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var deleteButton: Button
    private lateinit var cancelButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView

    private val firebaseManager = FirebaseManager()

    companion object {
        private const val TAG = "DeleteAccountActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_delete_account)
        Log.d(TAG, "onCreate")

        initializeViews()
        setupClickListeners()
    }

    private fun initializeViews() {
        backButton = findViewById(R.id.backButton)
        titleText = findViewById(R.id.titleText)
        warningText = findViewById(R.id.warningText)
        passwordInput = findViewById(R.id.passwordInput)
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput)
        deleteButton = findViewById(R.id.deleteButton)
        cancelButton = findViewById(R.id.cancelButton)
        progressBar = findViewById(R.id.progressBar)
        statusText = findViewById(R.id.statusText)

        // Set user email in warning text if available
        val userEmail = firebaseManager.getUserEmail()
        Log.d(TAG, "User email retrieved: $userEmail")

        if (userEmail != null) {
            warningText.text = "You are about to permanently delete the account: $userEmail\n\nThis action will delete ALL your data and cannot be undone!"
        } else {
            warningText.text = "You are about to permanently delete your account.\n\nThis action will delete ALL your data and cannot be undone!"
            Log.w(TAG, "User email is null")
        }
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            Log.d(TAG, "Back button clicked")
            finish()
        }

        deleteButton.setOnClickListener {
            Log.d(TAG, "Delete button clicked")
            if (validateInputs()) {
                showFinalConfirmationDialog()
            }
        }

        cancelButton.setOnClickListener {
            Log.d(TAG, "Cancel button clicked")
            finish()
        }

        // Auto-focus password field
        passwordInput.requestFocus()
    }

    private fun validateInputs(): Boolean {
        val password = passwordInput.text.toString()
        val confirmPassword = confirmPasswordInput.text.toString()

        if (password.isEmpty()) {
            passwordInput.error = "Password is required"
            return false
        }

        if (confirmPassword.isEmpty()) {
            confirmPasswordInput.error = "Please confirm your password"
            return false
        }

        if (password != confirmPassword) {
            confirmPasswordInput.error = "Passwords do not match"
            return false
        }

        return true
    }

    private fun showFinalConfirmationDialog() {
        Log.d(TAG, "Showing final confirmation dialog")

        // Create the custom EditText for confirmation
        val confirmationEditText = EditText(this).apply {
            hint = "Type DELETE here"
            setPadding(32, 16, 32, 16)
        }

        AlertDialog.Builder(this)
            .setTitle("⚠️ FINAL CONFIRMATION")
            .setMessage(
                """
                ARE YOU ABSOLUTELY SURE?
                
                This will PERMANENTLY delete:
                • Your account login
                • All brushing history
                • Schedules and reminders
                • Progress data and statistics
                • Device connection data
                
                This action CANNOT BE UNDONE!
                
                Type "DELETE" to confirm:
                """.trimIndent()
            )
            .setView(confirmationEditText)
            .setPositiveButton("DELETE ACCOUNT") { dialog, _ ->
                val confirmationText = confirmationEditText.text.toString().trim()
                Log.d(TAG, "Confirmation text entered: $confirmationText")

                if (confirmationText == "DELETE") {
                    val password = passwordInput.text.toString()
                    Log.d(TAG, "Starting account deletion process")
                    processAccountDeletion(password)
                } else {
                    Toast.makeText(
                        this@DeleteAccountActivity,
                        "You must type 'DELETE' to confirm",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                Log.d(TAG, "Deletion cancelled by user")
                dialog.dismiss()
            }
            .show()
    }

    private fun processAccountDeletion(password: String) {
        Log.d(TAG, "processAccountDeletion started")
        showProgress(true)

        // Use a coroutine scope with proper cancellation
        val deletionScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        deletionScope.launch {
            try {
                Log.d(TAG, "Calling FirebaseManager.deleteCurrentUserAccountImmediately")
                val result = firebaseManager.deleteCurrentUserAccountImmediately(password)
                Log.d(TAG, "Deletion result received: ${result::class.simpleName}")

                withContext(Dispatchers.Main) {
                    showProgress(false)

                    // Use FULL QUALIFIED NAME
                    when (result) {
                        is FirebaseManager.DeletionResult.Success -> {
                            Log.d(TAG, "Deletion SUCCESS: ${result.message}")
                            showDeletionSuccess(result.message)
                        }
                        is FirebaseManager.DeletionResult.Error -> {
                            Log.e(TAG, "Deletion ERROR: ${result.message}")
                            showDeletionError(result.message)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during deletion process: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    showProgress(false)
                    showDeletionError("An unexpected error occurred: ${e.message ?: "Unknown error"}")
                }
            } finally {
                deletionScope.cancel() // Clean up the scope
            }
        }
    }

    private fun showProgress(show: Boolean) {
        runOnUiThread {
            Log.d(TAG, "showProgress: $show")
            progressBar.isVisible = show
            statusText.isVisible = show

            passwordInput.isEnabled = !show
            confirmPasswordInput.isEnabled = !show
            deleteButton.isEnabled = !show
            cancelButton.isEnabled = !show

            if (show) {
                statusText.text = "Deleting account and all data..."
            }
        }
    }

    private fun showDeletionSuccess(message: String) {
        AlertDialog.Builder(this)
            .setTitle("✅ Account Deleted")
            .setMessage("$message\n\nYou will now be redirected to the login screen.")
            .setCancelable(false)
            .setPositiveButton("OK") { _, _ ->
                Log.d(TAG, "Navigating to login after successful deletion")
                navigateToLogin()
            }
            .show()
    }

    private fun showDeletionError(errorMessage: String) {
        AlertDialog.Builder(this)
            .setTitle("❌ Deletion Failed")
            .setMessage(errorMessage)
            .setPositiveButton("OK", null)
            .setNegativeButton("Try Again") { _, _ ->
                Log.d(TAG, "User clicked Try Again")
                // Clear password fields for retry
                passwordInput.text.clear()
                confirmPasswordInput.text.clear()
                passwordInput.requestFocus()
            }
            .show()
    }

    private fun navigateToLogin() {
        Log.d(TAG, "Navigating to login screen")

        // Clear any local app data
        clearLocalData()

        // Navigate to login screen
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun clearLocalData() {
        try {
            Log.d(TAG, "Clearing local data")

            // Clear session using UserSessionManager
            val sessionManager = UserSessionManager(this)
            sessionManager.clearSession()

            // Clear shared preferences
            getSharedPreferences("MintyMinutesSession", MODE_PRIVATE)
                .edit()
                .clear()
                .apply()

            // Clear any cached files
            cacheDir.deleteRecursively()

        } catch (e: Exception) {
            Log.e(TAG, "Error clearing local data: ${e.message}")
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
    }
}