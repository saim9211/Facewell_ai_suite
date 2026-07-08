package com.example.fyp.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "fyp_session_pref"
        private const val KEY_LAST_LOGIN = "last_login_timestamp"
        private const val SESSION_DURATION_MS = 15L * 24 * 60 * 60 * 1000 // 15 days
    }

    /**
     * Call this after a successful LoginActivity.
     */
    fun saveLoginSession() {
        prefs.edit().putLong(KEY_LAST_LOGIN, System.currentTimeMillis()).apply()
    }

    /**
     * Check if 15 days have passed since the last login.
     */
    fun isSessionValid(): Boolean {
        val lastLogin = prefs.getLong(KEY_LAST_LOGIN, 0L)
        if (lastLogin == 0L) return false
        
        val currentTime = System.currentTimeMillis()
        return (currentTime - lastLogin) < SESSION_DURATION_MS
    }

    /**
     * Clear session on manual logout.
     */
    fun clearSession() {
        prefs.edit().remove(KEY_LAST_LOGIN).apply()
    }
}
