package com.example.fyp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

import com.example.fyp.utils.SessionManager

class SplashActivity : AppCompatActivity() {

    private val phase1Ms = 600L   // white
    private val phase2Ms = 1500L  // logo
    private val phase3Ms = 600L   // white

    private lateinit var auth: FirebaseAuth
    private lateinit var sessionManager: SessionManager
    private val db by lazy { FirebaseFirestore.getInstance() }

    private var splashDone = false
    private var authCheckDone = false
    private var routed = false

    private var routingIntent: Intent? = null

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        auth = FirebaseAuth.getInstance()
        sessionManager = SessionManager(this)

        // start auth + firestore check immediately
        checkLoginAndPrepareRoute()

        val logo = findViewById<View>(R.id.logo)

        // Phase 1
        handler.postDelayed({
            // Phase 2: show logo
            logo.visibility = View.VISIBLE

            handler.postDelayed({
                // Phase 3: hide logo
                logo.visibility = View.GONE

                handler.postDelayed({
                    splashDone = true
                    tryRoute()
                }, phase3Ms)

            }, phase2Ms)

        }, phase1Ms)
    }

    private fun tryRoute() {
        if (!routed && splashDone && authCheckDone && routingIntent != null) {
            startAndFinish(routingIntent!!)
        } else if (!routed && splashDone && !authCheckDone) {
            // Wait a bit longer if auth check is still pending
            handler.postDelayed({ tryRoute() }, 500L)
        }
    }

    /**
     * Auth + Firestore routing logic (NEW FLOW)
     */
    private fun checkLoginAndPrepareRoute() {
        val user = auth.currentUser
        
        // 1. Check if user is logged in via Firebase AND session is valid (15 days)
        if (user == null || !sessionManager.isSessionValid()) {
            // If Firebase user exists but session invalid, sign out for safety
            if (user != null) {
                auth.signOut()
                sessionManager.clearSession()
            }
            routingIntent = Intent(this, LoginActivity::class.java)
            authCheckDone = true
            tryRoute()
            return
        }

        // 2. Validate token (Firebase session check)
        user.getIdToken(false)
            .addOnSuccessListener {
                goNextByStage()
            }
            .addOnFailureListener {
                // If token fails, user might need to re-auth
                routingIntent = Intent(this, LoginActivity::class.java)
                authCheckDone = true
                tryRoute()
            }
    }

    /**
     * ✅ EXACT ROUTING LOGIC YOU PROVIDED
     */
    private fun goNextByStage() {
        val uid = auth.currentUser?.uid ?: run {
            authCheckDone = true
            routingIntent = Intent(this, LoginActivity::class.java)
            tryRoute()
            return
        }

        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    routingIntent = Intent(this, LoginActivity::class.java)
                    authCheckDone = true
                    tryRoute()
                    return@addOnSuccessListener
                }

                val stage = (doc.getLong("stage") ?: 0L).toInt()
                val userType = doc.getString("userType") ?: ""
                val isApproved = doc.getBoolean("isApproved") ?: false

                routingIntent = when {
                    stage <= 0 ->
                        Intent(this, SelectUserTypeActivity::class.java)

                    stage == 1 -> when (userType) {
                        "user" -> Intent(this, CreateProfileActivity::class.java)
                        "vendor" -> Intent(this, VendorCreateProfileActivity::class.java)
                        "clinic" -> Intent(this, ClinicCreateProfileActivity::class.java)
                        else -> Intent(this, SelectUserTypeActivity::class.java)
                    }

                    stage == 2 -> when (userType) {
                        "user" ->
                            Intent(this, MainActivity::class.java)

                        "vendor", "clinic" ->
                            if (!isApproved)
                                Intent(this, WaitingApprovalActivity::class.java)
                            else
                                if (userType == "vendor")
                                    Intent(this, VendorMainActivity::class.java)
                                else
                                    Intent(this, ClinicMainActivity::class.java)

                        else -> Intent(this, MainActivity::class.java)
                    }

                    else -> Intent(this, SelectUserTypeActivity::class.java)
                }

                authCheckDone = true
                tryRoute()
            }
            .addOnFailureListener {
                // On failure, we might be offline or Firestore has an issue.
                // In some cases we might want to stay on current screen or retry.
                // For now, let's fallback to Login after a short wait if it keeps failing.
                routingIntent = Intent(this, LoginActivity::class.java)
                authCheckDone = true
                tryRoute()
            }
    }

    private fun startAndFinish(intent: Intent) {
        if (routed) return
        routed = true
        startActivity(
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
