package com.example.mintyminutes

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileViewActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private lateinit var fullNameView: TextView
    private lateinit var emailView: TextView
    private lateinit var phoneView: TextView
    private lateinit var memberSinceView: TextView
    private lateinit var editProfileButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_view)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://minty-minutes-cloud-default-rtdb.asia-southeast1.firebasedatabase.app/")

        initViews()
        setupListeners()
        loadUserData()
    }

    private fun initViews() {
        fullNameView = findViewById(R.id.fullNameView)
        emailView = findViewById(R.id.emailView)
        phoneView = findViewById(R.id.phoneView)
        editProfileButton = findViewById(R.id.editProfileButton)
    }

    private fun setupListeners() {
        editProfileButton.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)

            // PASS CURRENT DATA TO PROFILE ACTIVITY
            // Get current data from TextViews (already loaded from Firebase)
            intent.putExtra("current_name", fullNameView.text.toString())
            intent.putExtra("current_email", emailView.text.toString())
            intent.putExtra("current_phone", phoneView.text.toString())

            startActivity(intent)
        }
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid

            // Get user data from Firebase Realtime Database
            database.getReference("users").child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            // Get data from database
                            val name = snapshot.child("name").getValue(String::class.java) ?: "Not set"
                            val email = snapshot.child("email").getValue(String::class.java) ?: currentUser.email ?: "Not set"
                            val phone = snapshot.child("phone").getValue(String::class.java) ?: "Not set"
                            val createdAt = snapshot.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()

                            // Update UI with real data
                            fullNameView.text = name
                            emailView.text = email
                            phoneView.text = phone
                            memberSinceView.text = formatDate(createdAt)
                        } else {
                            // Fallback to Firebase Auth data
                            fullNameView.text = currentUser.displayName ?: "User"
                            emailView.text = currentUser.email ?: "Not set"
                            phoneView.text = "Not set"
                            memberSinceView.text = "Recently"
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@ProfileViewActivity, "Failed to load profile: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        } else {
            // User not logged in
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun formatDate(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }
}