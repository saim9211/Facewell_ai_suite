package com.example.fyp

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class VendorAddProductFragment : Fragment() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private lateinit var etTitle: EditText
    private lateinit var etDescription: EditText
    private lateinit var tvDescCounter: TextView
    private lateinit var spinnerCategory: Spinner
    private lateinit var etLink: EditText
    private lateinit var btnList: Button
    private lateinit var progress: ProgressBar

    private val MAX_WORDS = 30

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_vendor_add_product_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        etTitle = view.findViewById(R.id.etTitle)
        etDescription = view.findViewById(R.id.etDescription)
        tvDescCounter = view.findViewById(R.id.tvDescCounter)
        spinnerCategory = view.findViewById(R.id.spinnerCategory)
        etLink = view.findViewById(R.id.etLink)
        btnList = view.findViewById(R.id.btnListProduct)
        progress = view.findViewById(R.id.progress)

        // setup category spinner (labels user sees)
        val categoryLabels = listOf(
            "Ophthalmology Clinic (eye)",
            "Dermatology & Aesthetics (skin)",
            "Mental Health & Wellness (mood)",
            "Other"
        )
        spinnerCategory.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categoryLabels).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        // description word counter
        etDescription.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val words = s?.trim()?.split("\\s+".toRegex())?.filter { it.isNotBlank() } ?: emptyList()
                val count = words.size
                tvDescCounter.text = "$count / $MAX_WORDS"
                if (count > MAX_WORDS) {
                    tvDescCounter.setTextColor(resources.getColor(android.R.color.holo_red_dark))
                    btnList.isEnabled = false
                } else {
                    tvDescCounter.setTextColor(resources.getColor(android.R.color.darker_gray))
                    btnList.isEnabled = true
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        btnList.setOnClickListener { createProduct() }
    }

    private fun createProduct() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "Not authenticated", Toast.LENGTH_SHORT).show()
            return
        }
        val title = etTitle.text.toString().trim()
        val description = etDescription.text.toString().trim()
        val link = etLink.text.toString().trim()
        val categoryIndex = spinnerCategory.selectedItemPosition
        val category = when (categoryIndex) {
            0 -> "eye"
            1 -> "skin"
            2 -> "mood"
            else -> "other"
        }

        // VALIDATION
        if (title.isEmpty()) {
            etTitle.error = "Required"
            etTitle.requestFocus()
            return
        }
        val words = description.split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (words.size > MAX_WORDS) {
            Snackbar.make(requireView(), "Description must be ≤ $MAX_WORDS words", Snackbar.LENGTH_LONG).show()
            return
        }
        if (link.isEmpty()) {
            etLink.error = "Product link is required"
            etLink.requestFocus()
            return
        }
        if (!Patterns.WEB_URL.matcher(link).matches()) {
            etLink.error = "Enter a valid URL (including https://)"
            etLink.requestFocus()
            return
        }

        // lock UI
        btnList.isEnabled = false
        progress.visibility = View.VISIBLE

        val now = System.currentTimeMillis()
        val productMap = hashMapOf<String, Any?>(
            "vendorId" to user.uid,
            "title" to title,
            "description" to description,
            "category" to category,
            "link" to link,
            "images" to listOf<String>(),
            "clicks" to 0L,
            "views" to 0L,
            "price" to null,
            "currency" to "PKR",
            "isActive" to true,
            "isApproved" to false,
            "visibility" to "public",
            "createdAt" to now,
            "updatedAt" to now
        )

        db.collection("products").add(productMap)
            .addOnSuccessListener { ref ->
                val newId = ref.id
                ref.update("id", newId)
                // add product id to vendorProducts array (non-blocking)
                db.collection("users").document(user.uid)
                    .update("vendorProducts", FieldValue.arrayUnion(newId))
                    .addOnCompleteListener {
                        // ignore errors but proceed to move to listings
                        moveToListings()
                    }
            }
            .addOnFailureListener { e ->
                btnList.isEnabled = true
                progress.visibility = View.GONE
                Snackbar.make(requireView(), "Failed: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
    }

    private fun moveToListings() {
        progress.visibility = View.GONE
        btnList.isEnabled = true

        val act = activity
        if (act != null) {
            val bottomNav = act.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavVendor)
            if (bottomNav != null) {
                bottomNav.selectedItemId = R.id.nav_vendor_listings
                return
            }
            val vp = act.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPagerVendor)
            if (vp != null) {
                val idx = VendorPagerAdapter.Page.values().indexOf(VendorPagerAdapter.Page.LISTINGS)
                vp.setCurrentItem(idx, false)
            }
        }
    }
}
