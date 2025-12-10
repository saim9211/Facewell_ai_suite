package com.example.fyp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.Selection
import android.text.TextWatcher
import android.util.Patterns
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class ClinicCreateProfileActivity : AppCompatActivity() {

    private lateinit var tilClinicName: TextInputLayout
    private lateinit var tilClinicPhone: TextInputLayout
    private lateinit var tilCity: TextInputLayout
    private lateinit var tilAddress: TextInputLayout
    private lateinit var tilWebsite: TextInputLayout

    private lateinit var etClinicName: TextInputEditText
    private lateinit var etClinicPhone: TextInputEditText
    private lateinit var etCity: TextInputEditText
    private lateinit var etAddress: TextInputEditText
    private lateinit var etWebsite: TextInputEditText

    private lateinit var btnSave: MaterialButton
    private lateinit var overlay: View
    private lateinit var progress: CircularProgressIndicator

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    // for phone formatting (same style as Signup)
    private var phoneFormatting = false

    // simple city list (you can edit names)
    private val cities = arrayOf(
        "Lahore",
        "Karachi",
        "Islamabad",
        "Rawalpindi",
        "Faisalabad",
        "Multan",
        "Peshawar",
        "Quetta",
        "Sahiwal",
        "Dera Ghazi Khan"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_clinic_create_profile)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        bindViews()
        setupInputConstraints()
        setupLiveValidation()
        setupCityPicker()

        loadExisting()

        btnSave.setOnClickListener { onSaveClick() }
    }

    private fun bindViews() {
        tilClinicName = findViewById(R.id.tilClinicName)
        tilClinicPhone = findViewById(R.id.tilClinicPhone)
        tilCity = findViewById(R.id.tilCity)
        tilAddress = findViewById(R.id.tilAddress)
        tilWebsite = findViewById(R.id.tilWebsite)

        etClinicName = findViewById(R.id.etClinicName)
        etClinicPhone = findViewById(R.id.etClinicPhone)
        etCity = findViewById(R.id.etCity)
        etAddress = findViewById(R.id.etAddress)
        etWebsite = findViewById(R.id.etWebsite)

        btnSave = findViewById(R.id.btnSave)
        overlay = findViewById(R.id.loadingOverlay)
        progress = findViewById(R.id.progress)
    }

    private fun setupInputConstraints() {
        // phone same as signup: allow 11 digits + optional hyphen
        etClinicPhone.filters = arrayOf(InputFilter.LengthFilter(12))
    }

    private fun setupLiveValidation() {
        etClinicName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                tilClinicName.error = null
            }
        })

        // phone – live formatting like SignupActivity
        etClinicPhone.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (phoneFormatting) return
                phoneFormatting = true
                try {
                    val raw = s?.toString() ?: ""
                    val digits = raw.filter { it.isDigit() }

                    val formatted = when {
                        digits.length <= 4 -> digits
                        digits.length <= 11 -> {
                            val first = digits.substring(0, 4)
                            val rest = digits.substring(4)
                            "$first-$rest"
                        }
                        else -> {
                            val first = digits.substring(0, 4)
                            val rest = digits.substring(4, 11)
                            "$first-$rest"
                        }
                    }

                    if (formatted != raw) {
                        etClinicPhone.setText(formatted)
                        val newPos = formatted.length.coerceIn(0, formatted.length)
                        Selection.setSelection(etClinicPhone.text, newPos)
                    }
                } catch (_: Exception) {
                } finally {
                    tilClinicPhone.error = null
                    phoneFormatting = false
                }
            }
        })

        etCity.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                tilCity.error = null
            }
        })

        etAddress.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                tilAddress.error = null
            }
        })

        etWebsite.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                tilWebsite.error = null
            }
        })
    }

    private fun setupCityPicker() {
        // user will always pick from list; no manual typing
        etCity.isFocusable = false
        etCity.isClickable = true

        val showDialog = {
            AlertDialog.Builder(this)
                .setTitle("Select city")
                .setItems(cities) { _, which ->
                    etCity.setText(cities[which])
                    tilCity.error = null
                }
                .show()
        }

        etCity.setOnClickListener { showDialog() }
        etCity.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) showDialog()
        }
    }

    private fun loadExisting() {
        val uid = auth.currentUser?.uid ?: return
        showLoading(true)

        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                etClinicName.setText(doc.getString("clinicName") ?: "")
                etClinicPhone.setText(doc.getString("clinicPhone") ?: "")
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

        // required checks
        if (name.isEmpty()) {
            tilClinicName.error = "Clinic name is required"
            ok = false
        }
        if (phoneRaw.isEmpty()) {
            tilClinicPhone.error = "Clinic phone is required"
            ok = false
        }
        if (city.isEmpty()) {
            tilCity.error = "City is required"
            ok = false
        }
        if (address.isEmpty()) {
            tilAddress.error = "Clinic address is required"
            ok = false
        }

        // name: alphabets + spaces only
        if (name.isNotEmpty() && !name.matches(Regex("^[A-Za-z ]+\$"))) {
            tilClinicName.error = "Clinic name must contain letters only"
            ok = false
        }

        // phone: same rule as SignupActivity
        val phoneOk = phoneRaw.matches(Regex("^0\\d{3}-?\\d{7}\$"))
        if (phoneRaw.isNotEmpty() && !phoneOk) {
            tilClinicPhone.error =
                "Phone must start with 0 and be 11 digits (e.g., 0321-2345754 or 03212345754)"
            ok = false
        }

        // website optional but if filled then basic URL validation
        if (website.isNotEmpty() && !Patterns.WEB_URL.matcher(website).matches()) {
            tilWebsite.error = "Enter a valid website URL (e.g. https://example.com)"
            ok = false
        }

        if (!ok) return

        val uid = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "Not authenticated.", Toast.LENGTH_LONG).show()
            return
        }

        showLoading(true)

        // normalize phone digits only (same idea as signup)
        val phoneDigits = phoneRaw.filter { it.isDigit() }

        val payload = hashMapOf<String, Any>(
            "userType" to "clinic",
            "clinicName" to name,
            "clinicPhone" to phoneDigits,
            "city" to city,
            "clinicAddress" to address,
            "stage" to 2,
            "locationEnabled" to false
        )

        if (website.isNotEmpty()) {
            payload["clinicWebsite"] = website
        }

        db.collection("users").document(uid)
            .set(payload, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, "Clinic profile saved.", Toast.LENGTH_SHORT).show()
                val i = Intent(this, ClinicMainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(i)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save. Try again.", Toast.LENGTH_LONG).show()
                showLoading(false)
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
        progress.visibility = if (loading) View.VISIBLE else View.GONE
        progress.isIndeterminate = loading
        btnSave.isEnabled = !loading
        etClinicName.isEnabled = !loading
        etClinicPhone.isEnabled = !loading
        etCity.isEnabled = !loading
        etAddress.isEnabled = !loading
        etWebsite.isEnabled = !loading
    }
}
