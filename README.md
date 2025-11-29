GRADLE CHANGES

Updated Kotlin from 1.9.0 to 2.1.0 for compatibility

Added Firebase BoM, Auth, and Database dependencies

Used KTX extensions for Kotlin support

NEW USER MODEL
Created User.kt data class with fields:

id, name, email

dailyGoal (default: 3 brushing sessions)

streak, totalSessions, createdAt

lastBrushingDate

REGISTER MODEL UPDATES

Added Firebase Auth and Database imports

Implemented real Firebase authentication instead of simulation

Added database instance with your Asia-Southeast1 URL

Enhanced registerUser() to:

Create user with email/password via Firebase Auth

Update user profile with display name

Save user data to Realtime Database

DATABASE INTEGRATION

Created saveUserToDatabase() method

Stores user profiles under "users/{userId}" path

Sets default values for new users (3 daily goals, 0 streak)

Uses coroutines for async operations

FIREBASE CONSOLE SETUP

Enabled Email/Password authentication

Configured Realtime Database in Asia-Southeast1 region

Set security rules for user data protection

Database URL: minty-minutes-cloud-default-rtdb.asia-southeast1.firebasedatabase.app

REGISTRATION FLOW

User completes form validation

Firebase Auth creates account

User profile updated with name

User data saved to Realtime Database

Success navigation to login
