package com.example.fyp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class SelectUserTypeActivity : AppCompatActivity() {

    private lateinit var cardUser: MaterialCardView
    private lateinit var cardVendor: MaterialCardView
    private lateinit var cardClinic: MaterialCardView
    private lateinit var btnNext: MaterialButton

    private lateinit var icUser: ImageView
    private lateinit var icVendor: ImageView
    private lateinit var icClinic: ImageView

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var selectedType: String? = null // "user" | "vendor" | "clinic"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_user_type)

        bindViews()
        setupClicks()
    }

    private fun bindViews() {
        cardUser   = findViewById(R.id.cardUser)
        cardVendor = findViewById(R.id.cardVendor)
        cardClinic = findViewById(R.id.cardClinic)
        btnNext    = findViewById(R.id.btnNextType)

        icUser   = findViewById(R.id.icUser)
        icVendor = findViewById(R.id.icVendor)
        icClinic = findViewById(R.id.icClinic)
    }

    private fun setupClicks() {
        cardUser.setOnClickListener { select("user") }
        cardVendor.setOnClickListener { select("vendor") }
        cardClinic.setOnClickListener { select("clinic") }

        btnNext.setOnClickListener { saveAndProceed() }
    }

    private fun select(type: String) {
        selectedType = type

        // reset visuals
        resetCard(cardUser, icUser)
        resetCard(cardVendor, icVendor)
        resetCard(cardClinic, icClinic)

        // highlight selected
        when (type) {
            "user" -> highlight(cardUser, icUser, R.color.teal_mid)
            "vendor" -> highlight(cardVendor, icVendor, R.color.red)
            "clinic" -> highlight(cardClinic, icClinic, R.color.scin_blue)
        }
    }

    private fun resetCard(card: MaterialCardView, icon: ImageView) {
        card.strokeWidth = 0
        icon.setColorFilter(getColor(R.color.text_muted))
    }

    private fun highlight(card: MaterialCardView, icon: ImageView, color: Int) {
        card.strokeWidth = 4
        card.strokeColor = getColor(color)
        icon.setColorFilter(getColor(color))
    }

    private fun saveAndProceed() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedType == null) {
            Toast.makeText(this, "Please select an account type.", Toast.LENGTH_SHORT).show()
            return
        }

        val payload = mapOf(
            "userType" to selectedType,
            "stage" to 1 // next stage = profile creation for that type
        )

        db.collection("users").document(uid)
            .set(payload, SetOptions.merge())
            .addOnSuccessListener {
                when (selectedType) {
                    "user" -> startActivity(Intent(this, CreateProfileActivity::class.java))
                    "vendor" -> startActivity(Intent(this, VendorCreateProfileActivity::class.java))
                    "clinic" -> startActivity(Intent(this, ClinicCreateProfileActivity::class.java))
                }
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save.", Toast.LENGTH_SHORT).show()
            }
    }
}
