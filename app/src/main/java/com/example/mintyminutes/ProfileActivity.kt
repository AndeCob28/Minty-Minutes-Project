package com.example.mintyminutes

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ProfileActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var menuIcon: ImageView
    private lateinit var profileImage: ImageView
    private lateinit var fullNameEdit: EditText
    private lateinit var emailEdit: EditText
    private lateinit var phoneEdit: EditText
    private lateinit var passwordEdit: EditText
    private lateinit var saveChangesBtn: Button

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance(
        "https://minty-minutes-cloud-default-rtdb.asia-southeast1.firebasedatabase.app/"
    )
    private val storage = FirebaseStorage.getInstance()

    private var selectedImageUri: Uri? = null
    private var currentUser: User? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            profileImage.setImageURI(it)
            Toast.makeText(this, "Profile picture selected", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        initializeViews()
        setupClickListeners()
        setupSidebarMenu()
        loadUserData()
    }

    private fun initializeViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        menuIcon = findViewById(R.id.menuIcon)
        profileImage = findViewById(R.id.profileImage)
        fullNameEdit = findViewById(R.id.fullNameEdit)
        emailEdit = findViewById(R.id.emailEdit)
        phoneEdit = findViewById(R.id.phoneEdit)
        passwordEdit = findViewById(R.id.passwordEdit)
        saveChangesBtn = findViewById(R.id.saveChangesBtn)
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return

        saveChangesBtn.isEnabled = false
        saveChangesBtn.text = "Loading..."

        database.getReference("users").child(userId).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                currentUser = snapshot.getValue(User::class.java)
                currentUser?.let { user ->
                    fullNameEdit.setText(user.name)
                    emailEdit.setText(user.email)
                    // Phone number can be added to User data class if needed
                }
                println("DEBUG ProfileActivity: User data loaded - ${currentUser?.name}")
            }
            saveChangesBtn.isEnabled = true
            saveChangesBtn.text = "SAVE CHANGES"
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed to load profile: ${e.message}", Toast.LENGTH_SHORT).show()
            saveChangesBtn.isEnabled = true
            saveChangesBtn.text = "SAVE CHANGES"
        }
    }

    private fun setupClickListeners() {
        menuIcon.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        profileImage.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        saveChangesBtn.setOnClickListener {
            saveProfileChanges()
        }
    }

    private fun saveProfileChanges() {
        val fullName = fullNameEdit.text.toString().trim()
        val email = emailEdit.text.toString().trim()
        val phone = phoneEdit.text.toString().trim()
        val password = passwordEdit.text.toString()

        // Validation
        if (fullName.isEmpty()) {
            fullNameEdit.error = "Name is required"
            return
        }

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEdit.error = "Valid email is required"
            return
        }

        if (phone.isNotEmpty() && phone.length < 10) {
            phoneEdit.error = "Valid phone number required"
            return
        }

        if (password.isNotEmpty() && password.length < 6) {
            passwordEdit.error = "Password must be at least 6 characters"
            return
        }

        // Show loading
        saveChangesBtn.isEnabled = false
        saveChangesBtn.text = "Saving..."

        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Update profile picture if selected
                if (selectedImageUri != null) {
                    uploadProfilePicture()
                }

                // Update name in database
                updateUserName(fullName)

                // Update email if changed
                if (email != currentUser?.email) {
                    updateEmail(email)
                }

                // Update password if provided
                if (password.isNotEmpty()) {
                    updatePassword(password)
                }

                // Update session manager
                val sessionManager = UserSessionManager(this@ProfileActivity)
                sessionManager.saveUserSession(fullName, email)

                Toast.makeText(this@ProfileActivity, "Profile updated successfully!", Toast.LENGTH_LONG).show()
                passwordEdit.setText("") // Clear password field

            } catch (e: Exception) {
                Toast.makeText(this@ProfileActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                println("DEBUG ProfileActivity: Error updating profile - ${e.message}")
            } finally {
                saveChangesBtn.isEnabled = true
                saveChangesBtn.text = "SAVE CHANGES"
            }
        }
    }

    private suspend fun uploadProfilePicture() = withContext(Dispatchers.IO) {
        try {
            val userId = auth.currentUser?.uid ?: return@withContext
            val imageRef = storage.reference.child("profile_pictures/$userId.jpg")

            selectedImageUri?.let { uri ->
                imageRef.putFile(uri).await()
                val downloadUrl = imageRef.downloadUrl.await()

                // Save URL to database
                database.getReference("users")
                    .child(userId)
                    .child("profilePictureUrl")
                    .setValue(downloadUrl.toString())
                    .await()

                println("DEBUG ProfileActivity: Profile picture uploaded")
            }
        } catch (e: Exception) {
            println("DEBUG ProfileActivity: Failed to upload picture - ${e.message}")
            throw e
        }
    }

    private suspend fun updateUserName(newName: String) = withContext(Dispatchers.IO) {
        try {
            val userId = auth.currentUser?.uid ?: return@withContext
            val user = auth.currentUser

            // Update in Firebase Auth
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(newName)
                .build()
            user?.updateProfile(profileUpdates)?.await()

            // Update in Database
            database.getReference("users")
                .child(userId)
                .child("name")
                .setValue(newName)
                .await()

            println("DEBUG ProfileActivity: Name updated to $newName")
        } catch (e: Exception) {
            println("DEBUG ProfileActivity: Failed to update name - ${e.message}")
            throw e
        }
    }

    private suspend fun updateEmail(newEmail: String) = withContext(Dispatchers.IO) {
        try {
            val user = auth.currentUser ?: return@withContext
            val userId = user.uid

            // Need to re-authenticate before email change
            val credential = EmailAuthProvider.getCredential(
                user.email ?: return@withContext,
                passwordEdit.text.toString()
            )

            if (passwordEdit.text.toString().isEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ProfileActivity,
                        "Please enter your current password to change email",
                        Toast.LENGTH_LONG
                    ).show()
                }
                return@withContext
            }

            user.reauthenticate(credential).await()
            user.verifyBeforeUpdateEmail(newEmail).await()

            // Update in Database
            database.getReference("users")
                .child(userId)
                .child("email")
                .setValue(newEmail)
                .await()

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@ProfileActivity,
                    "Verification email sent to $newEmail",
                    Toast.LENGTH_LONG
                ).show()
            }

            println("DEBUG ProfileActivity: Email update initiated")
        } catch (e: Exception) {
            println("DEBUG ProfileActivity: Failed to update email - ${e.message}")
            throw e
        }
    }

    private suspend fun updatePassword(newPassword: String) = withContext(Dispatchers.IO) {
        try {
            val user = auth.currentUser ?: return@withContext
            user.updatePassword(newPassword).await()

            println("DEBUG ProfileActivity: Password updated")
        } catch (e: Exception) {
            println("DEBUG ProfileActivity: Failed to update password - ${e.message}")
            throw e
        }
    }

    private fun setupSidebarMenu() {
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_settings -> {
                    Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show()
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_profile -> {
                    Toast.makeText(this, "Already in Profile", Toast.LENGTH_SHORT).show()
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_logout -> {
                    val sessionManager = UserSessionManager(this)
                    sessionManager.clearSession()

                    auth.signOut()

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

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}