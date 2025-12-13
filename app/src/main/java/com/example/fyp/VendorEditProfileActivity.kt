package com.example.fyp

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class VendorEditProfileActivity : AppCompatActivity() {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private lateinit var tilVendorName: TextInputLayout
    private lateinit var tilCity: TextInputLayout
    private lateinit var tilPhone: TextInputLayout
    private lateinit var tilWebsite: TextInputLayout

    private lateinit var etVendorName: TextInputEditText
    private lateinit var etCity: MaterialAutoCompleteTextView
    private lateinit var etPhone: TextInputEditText
    private lateinit var etWebsite: TextInputEditText

    private lateinit var btnSave: MaterialButton
    private lateinit var overlay: View
    private lateinit var progress: CircularProgressIndicator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vendor_edit_profile)

        bindViews()
        setupCityAutocomplete()
        loadExisting()

        btnSave.setOnClickListener { saveProfile() }
    }

    private fun bindViews() {
        tilVendorName = findViewById(R.id.tilVendorName)
        tilCity = findViewById(R.id.tilCity)
        tilPhone = findViewById(R.id.tilPhone)
        tilWebsite = findViewById(R.id.tilWebsite)

        etVendorName = findViewById(R.id.etVendorName)
        etCity = findViewById(R.id.etCity)
        etPhone = findViewById(R.id.etPhone)
        etWebsite = findViewById(R.id.etWebsite)

        btnSave = findViewById(R.id.btnSave)

        overlay = findViewById(R.id.loadingOverlay)
        progress = findViewById(R.id.progress)
    }

    private fun setupCityAutocomplete() {
        val cities = resources.getStringArray(R.array.pk_cities)
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, cities)
        etCity.setAdapter(adapter)
        etCity.setOnClickListener { etCity.showDropDown() }
    }

    private fun loadExisting() {
        val uid = auth.currentUser?.uid ?: return
        showLoading(true)

        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                etVendorName.setText(doc.getString("vendorName") ?: "")
                etPhone.setText(doc.getString("phone") ?: "")
                etCity.setText(doc.getString("city") ?: "")
                etWebsite.setText(doc.getString("vendorWebsite") ?: "")
                showLoading(false)
            }
            .addOnFailureListener { showLoading(false) }
    }

    private fun saveProfile() {
        clearErrors()

        val name = etVendorName.text.toString().trim()
        val city = etCity.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val website = etWebsite.text.toString().trim()

        var ok = true

        if (name.isEmpty()) {
            tilVendorName.error = "Required"
            ok = false
        }
        if (city.isEmpty()) {
            tilCity.error = "Required"
            ok = false
        }
        if (phone.isEmpty()) {
            tilPhone.error = "Required"
            ok = false
        }

        if (!ok) return

        val uid = auth.currentUser?.uid ?: return

        showLoading(true)

        val payload = hashMapOf(
            "vendorName" to name,
            "phone" to phone,
            "city" to city,
            "vendorWebsite" to website,
            "userType" to "vendor",
            "stage" to 2,
            "updatedAt" to System.currentTimeMillis()
        )

        db.collection("users").document(uid)
            .set(payload, SetOptions.merge())
            .addOnSuccessListener {
                showLoading(false)
                finish()
            }
            .addOnFailureListener {
                showLoading(false)
                Toast.makeText(this, "Failed to save. Try again.", Toast.LENGTH_LONG).show()
            }
    }

    private fun clearErrors() {
        tilVendorName.error = null
        tilCity.error = null
        tilPhone.error = null
        tilWebsite.error = null
    }

    private fun showLoading(show: Boolean) {
        overlay.visibility = if (show) View.VISIBLE else View.GONE
        btnSave.isEnabled = !show
    }
}
