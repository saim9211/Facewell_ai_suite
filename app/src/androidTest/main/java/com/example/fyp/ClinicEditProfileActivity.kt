package com.example.fyp

import android.os.Bundle
import android.text.InputFilter
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import android.view.View

class ClinicEditProfileActivity : AppCompatActivity() {

    private lateinit var tilClinicName: TextInputLayout
    private lateinit var tilClinicPhone: TextInputLayout
    private lateinit var tilCity: TextInputLayout
    private lateinit var tilAddress: TextInputLayout
    private lateinit var tilWebsite: TextInputLayout

    private lateinit var etClinicName: TextInputEditText
    private lateinit var etClinicPhone: TextInputEditText
    private lateinit var etCity: MaterialAutoCompleteTextView
    private lateinit var etAddress: TextInputEditText
    private lateinit var etWebsite: TextInputEditText

    private lateinit var btnSave: MaterialButton
    private lateinit var overlay: View
    private lateinit var progress: CircularProgressIndicator

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_clinic_edit_profile)
        bindViews()
        setupCityAutocomplete()
        loadExisting()

        btnSave.setOnClickListener { onSaveClick() }
    }

    private fun bindViews() {
        // Match IDs exactly to the layout you provided
        tilClinicName = findViewById(R.id.tilClinicName)
        // layout uses tilPhone (not tilClinicPhone)
        tilClinicPhone = findViewById(R.id.tilPhone)
        tilCity = findViewById(R.id.tilCity)
        tilAddress = findViewById(R.id.tilAddress)
        tilWebsite = findViewById(R.id.tilWebsite)

        etClinicName = findViewById(R.id.etClinicName)
        // layout uses etPhone
        etClinicPhone = findViewById(R.id.etPhone)
        etCity = findViewById(R.id.etCity)
        etAddress = findViewById(R.id.etAddress)
        etWebsite = findViewById(R.id.etWebsite)

        btnSave = findViewById(R.id.btnSave)
        overlay = findViewById(R.id.loadingOverlay)
        progress = findViewById(R.id.progress)
    }

    private fun setupCityAutocomplete() {
        val cities = try {
            resources.getStringArray(R.array.pk_cities)
        } catch (e: Exception) {
            arrayOf<String>()
        }
        if (cities.isNotEmpty()) {
            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, cities)
            // etCity is MaterialAutoCompleteTextView — set adapter directly
            etCity.setAdapter(adapter)
            etCity.threshold = 1
            etCity.setOnClickListener { etCity.showDropDown() }
        }
    }

    private fun loadExisting() {
        val uid = auth.currentUser?.uid ?: return
        showLoading(true)
        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                etClinicName.setText(doc.getString("clinicName") ?: "")
                etClinicPhone.setText(doc.getString("clinicPhone") ?: doc.getString("phone") ?: "")
                etCity.setText(doc.getString("city") ?: "")
                etAddress.setText(doc.getString("clinicAddress") ?: "")
                etWebsite.setText(doc.getString("clinicWebsite") ?: "")
                showLoading(false)
            }
            .addOnFailureListener {
                showLoading(false)
            }
    }

    private fun onSaveClick() {
        clearErrors()

        val name = etClinicName.text?.toString()?.trim().orEmpty()
        val phoneRaw = etClinicPhone.text?.toString()?.trim().orEmpty()
        val city = etCity.text?.toString()?.trim().orEmpty()
        val address = etAddress.text?.toString()?.trim().orEmpty()
        val website = etWebsite.text?.toString()?.trim().orEmpty()

        var ok = true
        if (name.isEmpty()) {
            tilClinicName.error = "Clinic name is required"
            ok = false
        } else {
            if (!name.matches(Regex(".*[A-Za-z].*"))) {
                tilClinicName.error = "Enter a valid name"
                ok = false
            }
        }

        if (phoneRaw.isEmpty()) {
            tilClinicPhone.error = "Phone is required"
            ok = false
        } else {
            val phoneOk = phoneRaw.matches(Regex("^0\\d{3}-?\\d{7}\$")) || phoneRaw.matches(Regex("^\\+?\\d{7,15}\$"))
            if (!phoneOk) {
                tilClinicPhone.error = "Phone must start with 0 and be 11 digits (e.g., 0321-2345754)"
                ok = false
            }
        }

        if (city.isEmpty()) {
            tilCity.error = "City is required"
            ok = false
        }

        if (address.isEmpty()) {
            tilAddress.error = "Address is required"
            ok = false
        }

        if (!ok) return

        val uid = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "Not authenticated.", Toast.LENGTH_LONG).show()
            return
        }

        showLoading(true)

        val phoneDigits = phoneRaw.filter { it.isDigit() }

        val payload = hashMapOf<String, Any>(
            "userType" to "clinic",
            "clinicName" to name,
            "clinicPhone" to phoneDigits,
            "city" to city,
            "clinicAddress" to address,
            "clinicWebsite" to website,
            "stage" to 2,
            "locationEnabled" to false,
            "profileUpdatedAt" to System.currentTimeMillis()
        )

        db.collection("users").document(uid)
            .set(payload, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, "Profile updated.", Toast.LENGTH_SHORT).show()
                // finish so fragment's onResume will pick up new data
                finish()
            }
            .addOnFailureListener {
                showLoading(false)
                Toast.makeText(this, "Failed to save. Try again.", Toast.LENGTH_LONG).show()
            }
    }

    private fun clearErrors() {
        tilClinicName.error = null
        tilClinicPhone.error = null
        tilCity.error = null
        tilAddress.error = null
        tilWebsite.error = null
    }

    private fun showLoading(loading: Boolean) {
        overlay.visibility = if (loading) View.VISIBLE else View.GONE
        btnSave.isEnabled = !loading
        progress.isIndeterminate = loading
    }
}
