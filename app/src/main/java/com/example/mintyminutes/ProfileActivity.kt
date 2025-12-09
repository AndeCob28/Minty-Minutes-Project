package com.example.mintyminutes

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase

class ProfileActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var menuIcon: ImageView
    private lateinit var fullNameEdit: EditText
    private lateinit var emailEdit: EditText
    private lateinit var phoneEdit: EditText
    private lateinit var passwordEdit: EditText
    private lateinit var saveChangesBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        initializeViews()
        setupClickListeners()
        setupSidebarMenu()
        populateCurrentData() // ADD THIS LINE
    }

    private fun initializeViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        menuIcon = findViewById(R.id.menuIcon)
        fullNameEdit = findViewById(R.id.fullNameEdit)
        emailEdit = findViewById(R.id.emailEdit)
        phoneEdit = findViewById(R.id.phoneEdit)
        passwordEdit = findViewById(R.id.passwordEdit)
        saveChangesBtn = findViewById(R.id.saveChangesBtn)
    }

    // ADD THIS METHOD TO POPULATE CURRENT USER DATA
    private fun populateCurrentData() {
        // Get data passed from ProfileViewActivity
        val currentName = intent.getStringExtra("current_name")
        val currentEmail = intent.getStringExtra("current_email")
        val currentPhone = intent.getStringExtra("current_phone")

        // Pre-fill the EditText fields with current data
        if (currentName != null && currentName != "Loading...") {
            fullNameEdit.setText(currentName)
        }

        if (currentEmail != null && currentEmail != "Loading...") {
            emailEdit.setText(currentEmail)
        }

        if (currentPhone != null && currentPhone != "Loading...") {
            phoneEdit.setText(currentPhone)
        }
    }

    private fun setupClickListeners() {
        // Menu icon click listener
        menuIcon.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Save changes button - UPDATE THIS
        saveChangesBtn.setOnClickListener {
            val fullName = fullNameEdit.text.toString().trim()
            val email = emailEdit.text.toString().trim()
            val phone = phoneEdit.text.toString().trim()
            val password = passwordEdit.text.toString().trim()

            if (fullName.isNotEmpty() && email.isNotEmpty() && phone.isNotEmpty()) {
                // Save to Firebase Database
                saveProfileToFirebase(fullName, email, phone, password)
            } else {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupSidebarMenu() {
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    // Navigate back to HomeActivity
                    finish() // This closes ProfileActivity and goes back to Home
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_settings -> {
                    Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show()
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_profile -> {
                    // Already in Profile, just close drawer
                    Toast.makeText(this, "Already in Profile", Toast.LENGTH_SHORT).show()
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_logout -> {
                    // Logout and go back to LoginActivity
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

    // ADD THIS METHOD TO SAVE TO FIREBASE
    private fun saveProfileToFirebase(name: String, email: String, phone: String, password: String) {
        // Show loading
        saveChangesBtn.isEnabled = false
        saveChangesBtn.text = "Saving..."

        // Get current user
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser != null) {
            val userId = currentUser.uid
            val database = FirebaseDatabase.getInstance("https://minty-minutes-cloud-default-rtdb.asia-southeast1.firebasedatabase.app/")

            // Update user data in Firebase Database
            val userUpdates = hashMapOf<String, Any>(
                "name" to name,
                "email" to email,
                "phone" to phone
                // Note: Password cannot be updated directly - use Firebase Auth for password updates
            )

            database.getReference("users").child(userId)
                .updateChildren(userUpdates)
                .addOnSuccessListener {
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()

                    // Update Firebase Auth profile if needed
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()

                    currentUser.updateProfile(profileUpdates)
                        .addOnCompleteListener { profileTask ->
                            if (profileTask.isSuccessful) {
                                // Navigate back to Profile View
                                val intent = Intent(this, ProfileViewActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                startActivity(intent)
                                finish()
                            }
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
                    saveChangesBtn.isEnabled = true
                    saveChangesBtn.text = "SAVE CHANGES"
                }
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            saveChangesBtn.isEnabled = true
            saveChangesBtn.text = "SAVE CHANGES"
        }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}