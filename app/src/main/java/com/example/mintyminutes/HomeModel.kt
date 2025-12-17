package com.example.mintyminutes

import android.content.Context
import com.example.mintyminutes.UserSessionManager

class HomeModel(private val context: Context) {

    private val sessionManager = UserSessionManager(context)

    fun getUserName(): String {
        return sessionManager.getUserName()
    }

    fun clearSession() {
        sessionManager.clearSession()
    }
}
