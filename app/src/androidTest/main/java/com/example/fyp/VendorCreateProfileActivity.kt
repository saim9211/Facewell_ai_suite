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

class VendorCreateProfileActivity : AppCompatActivity() {

    private lateinit var tilVendorName: TextInputLayout
    private lateinit var tilVendorPhone: TextInputLayout
    private lateinit var tilVendorCity: TextInputLayout
    private lateinit var tilVendorAddress: TextInputLayout
    private lateinit var tilVendorWebsite: TextInputLayout

    private lateinit var etVendorName: TextInputEditText
    private lateinit var etVendorPhone: TextInputEditText
    private lateinit var etVendorCity: TextInputEditText
    private lateinit var etVendorAddress: TextInputEditText
    private lateinit var etVendorWebsite: TextInputEditText

    private lateinit var btnSave: MaterialButton
    private lateinit var overlay: View
    private lateinit var progress: CircularProgressIndicator

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private var phoneFormatting = false

    // minimal city list
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
        setContentView(R.layout.activity_vendor_create_profile)

        findViewById<ImageButton>(R.id.btnBack)?.setOnClickListener {
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
        tilVendorName = findViewById(R.id.tilVendorName)
        tilVendorPhone = findViewById(R.id.tilVendorPhone)
        tilVendorCity = findViewById(R.id.tilVendorCity)
        tilVendorAddress = findViewById(R.id.tilVendorAddress)
        tilVendorWebsite = findViewById(R.id.tilVendorWebsite)

        etVendorName = findViewById(R.id.etVendorName)
        etVendorPhone = findViewById(R.id.etVendorPhone)
        etVendorCity = findViewById(R.id.etVendorCity)
        etVendorAddress = findViewById(R.id.etVendorAddress)
        etVendorWebsite = findViewById(R.id.etVendorWebsite)

        btnSave = findViewById(R.id.btnSave)
        overlay = findViewById(R.id.loadingOverlay)
        progress = findViewById(R.id.progress)
    }

    private fun setupInputConstraints() {
        etVendorPhone.filters = arrayOf(InputFilter.LengthFilter(12)) // allow 4-7 with hyphen
    }

    private fun setupLiveValidation() {
        etVendorName.addTextChangedListener(simpleWatcher { tilVendorName.error = null })

        etVendorPhone.addTextChangedListener(object : TextWatcher {
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
                        etVendorPhone.setText(formatted)
                        val newPos = formatted.length.coerceIn(0, formatted.length)
                        Selection.setSelection(etVendorPhone.text, newPos)
                    }
                } catch (_: Exception) {
                } finally {
                    tilVendorPhone.error = null
                    phoneFormatting = false
                }
            }
        })

        etVendorCity.addTextChangedListener(simpleWatcher { tilVendorCity.error = null })
        etVendorAddress.addTextChangedListener(simpleWatcher { tilVendorAddress.error = null })
        etVendorWebsite.addTextChangedListener(simpleWatcher { tilVendorWebsite.error = null })
    }

    private fun simpleWatcher(after: () -> Unit) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, afterChange: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) { after() }
    }

    private fun setupCityPicker() {
        etVendorCity.isFocusable = false
        etVendorCity.isClickable = true

        val showDialog = {
            AlertDialog.Builder(this)
                .setTitle("Select city")
                .setItems(cities) { _, which ->
                    etVendorCity.setText(cities[which])
                    tilVendorCity.error = null
                }
                .show()
        }

        etVendorCity.setOnClickListener { showDialog() }
        etVendorCity.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) showDialog() }
    }

    private fun loadExisting() {
        val uid = auth.currentUser?.uid ?: return
        showLoading(true)
        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                etVendorName.setText(doc.getString("vendorName") ?: "")
                // try vendorPhone then phone
                etVendorPhone.setText(doc.getString("vendorPhone") ?: doc.getString("phone") ?: "")
                etVendorCity.setText(doc.getString("city") ?: "")
                etVendorAddress.setText(doc.getString("vendorAddress") ?: "")
                etVendorWebsite.setText(doc.getString("vendorWebsite") ?: "")
                showLoading(false)
            }
            .addOnFailureListener {
                showLoading(false)
            }
    }

    private fun onSaveClick() {
        clearErrors()

        val name = etVendorName.text?.toString()?.trim().orEmpty()
        val phoneRaw = etVendorPhone.text?.toString()?.trim().orEmpty()
        val city = etVendorCity.text?.toString()?.trim().orEmpty()
        val address = etVendorAddress.text?.toString()?.trim().orEmpty()
        val website = etVendorWebsite.text?.toString()?.trim().orEmpty()

        var ok = true
        if (name.isEmpty()) {
            tilVendorName.error = "Vendor name is required"
            ok = false
        } else if (!name.matches(Regex("^[A-Za-z ]+\$"))) {
            tilVendorName.error = "Only letters and spaces allowed"
            ok = false
        }

        if (phoneRaw.isEmpty()) {
            tilVendorPhone.error = "Phone is required"
            ok = false
        } else {
            val phoneOk = phoneRaw.matches(Regex("^0\\d{3}-?\\d{7}\$"))
            if (!phoneOk) {
                tilVendorPhone.error = "Phone must start with 0 and be 11 digits (e.g., 0321-2345754)"
                ok = false
            }
        }

        if (city.isEmpty()) {
            tilVendorCity.error = "City is required"
            ok = false
        }

        if (address.isEmpty()) {
            tilVendorAddress.error = "Address is required"
            ok = false
        }

        if (website.isNotEmpty() && !Patterns.WEB_URL.matcher(website).matches()) {
            tilVendorWebsite.error = "Enter a valid website URL"
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
            "userType" to "vendor",
            "vendorName" to name,
            "vendorPhone" to phoneDigits,
            "city" to city,
            "vendorAddress" to address,
            "stage" to 2,
            "locationEnabled" to false,
            "isApproved" to false,
            "profileUpdatedAt" to System.currentTimeMillis()
        )

        if (website.isNotEmpty()) payload["vendorWebsite"] = website

        db.collection("users").document(uid)
            .set(payload, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, "Vendor profile saved.", Toast.LENGTH_SHORT).show()
                val i = Intent(this, WaitingApprovalActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(i)
            }
            .addOnFailureListener {
                showLoading(false)
                Toast.makeText(this, "Failed to save. Try again.", Toast.LENGTH_LONG).show()
            }
    }

    private fun clearErrors() {
        tilVendorName.error = null
        tilVendorPhone.error = null
        tilVendorCity.error = null
        tilVendorAddress.error = null
        tilVendorWebsite.error = null
    }

    private fun showLoading(loading: Boolean) {
        overlay.visibility = if (loading) View.VISIBLE else View.GONE
        progress.visibility = if (loading) View.VISIBLE else View.GONE
        progress.isIndeterminate = loading
        btnSave.isEnabled = !loading
        etVendorName.isEnabled = !loading
        etVendorPhone.isEnabled = !loading
        etVendorCity.isEnabled = !loading
        etVendorAddress.isEnabled = !loading
        etVendorWebsite.isEnabled = !loading
    }
}
