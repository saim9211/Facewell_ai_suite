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
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class VendorAddProductFragment : Fragment() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private lateinit var etTitle: EditText
    private lateinit var etDesc: EditText
    private lateinit var tvDescCounter: TextView
    private lateinit var etCategory: MaterialAutoCompleteTextView
    private lateinit var etRecommended: MaterialAutoCompleteTextView

    private lateinit var etLink: EditText
    private lateinit var etPrice: EditText
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
        etDesc = view.findViewById(R.id.etDescription)
        tvDescCounter = view.findViewById(R.id.tvDescCounter)

        etCategory = view.findViewById(R.id.etCategory)
        etRecommended = view.findViewById(R.id.etRecommended)

        etLink = view.findViewById(R.id.etLink)
        etPrice = view.findViewById(R.id.etPrice)
        btnList = view.findViewById(R.id.btnListProduct)
        progress = view.findViewById(R.id.progress)

        setupCategoryDropdown()
        setupDescriptionCounter()

        btnList.setOnClickListener { createProduct() }
    }

    private fun setupCategoryDropdown() {
        val labels = listOf(
            "Ophthalmology (Eye)",
            "Dermatology (Skin)",
            "Mental Health (Mood)",
            "Other"
        )
        val categories = listOf("eye", "skin", "mood", "other")

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, labels)
        etCategory.setAdapter(adapter)

        etCategory.setOnItemClickListener { _, _, pos, _ ->
            loadRecommendedDiseases(categories[pos])
        }
    }

    private fun loadRecommendedDiseases(category: String) {
        val list = when (category) {
            "eye" -> DiseaseConstants.EYE_DISEASES
            "skin" -> DiseaseConstants.SKIN_DISEASES
            "mood" -> DiseaseConstants.MOOD_CLASSES
            else -> emptyList()
        }

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, list)
        etRecommended.setAdapter(adapter)
    }

    private fun setupDescriptionCounter() {
        etDesc.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val words = s?.trim()?.split("\\s+".toRegex())?.filter { it.isNotBlank() } ?: emptyList()
                val count = words.size

                tvDescCounter.text = "$count / $MAX_WORDS"
                tvDescCounter.setTextColor(
                    if (count > MAX_WORDS)
                        resources.getColor(android.R.color.holo_red_dark)
                    else
                        resources.getColor(android.R.color.darker_gray)
                )

                btnList.isEnabled = count <= MAX_WORDS
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun createProduct() {
        val user = auth.currentUser ?: return

        val title = etTitle.text.toString().trim()
        val desc = etDesc.text.toString().trim()
        val link = etLink.text.toString().trim()
        val price = etPrice.text.toString().toDoubleOrNull()

        val category = when {
            etCategory.text.toString().contains("eye", true) -> "eye"
            etCategory.text.toString().contains("skin", true) -> "skin"
            etCategory.text.toString().contains("mood", true) -> "mood"
            else -> "other"
        }

        val recommended = etRecommended.text.toString().trim()

        // VALIDATION
        if (title.isEmpty()) { etTitle.error = "Required"; etTitle.requestFocus(); return }
        if (desc.split("\\s+".toRegex()).size > MAX_WORDS) {
            Snackbar.make(requireView(), "Description must be ≤ 30 words", Snackbar.LENGTH_LONG).show()
            return
        }
        if (!Patterns.WEB_URL.matcher(link).matches()) {
            etLink.error = "Enter a valid URL"
            etLink.requestFocus()
            return
        }
        if (price == null) {
            etPrice.error = "Enter price"
            etPrice.requestFocus()
            return
        }

        btnList.isEnabled = false
        progress.visibility = View.VISIBLE

        val now = System.currentTimeMillis()

        val data = hashMapOf<String, Any?>(
            "vendorId" to user.uid,
            "title" to title,
            "description" to desc,
            "category" to category,
            "recommendedFor" to listOf(recommended),

            "link" to link,
            "price" to price,
            "currency" to "PKR",
            "images" to listOf<String>(),

            // Stats
            "clicks" to 0L,
            "views" to 0L,

            // ⭐ NEW — DEFAULT RATING FIELDS
            "avgRating" to 0.0,
            "ratingsCount" to 0,

            // Flags
            "isActive" to true,
            "isApproved" to false,
            "visibility" to "public",

            // Timestamps
            "createdAt" to now,
            "updatedAt" to now
        )


        db.collection("products").add(data)
            .addOnSuccessListener { ref ->
                ref.update("id", ref.id)

                db.collection("users").document(user.uid)
                    .update("vendorProducts", FieldValue.arrayUnion(ref.id))

                clearFields()
                moveToListings()
            }
            .addOnFailureListener {
                btnList.isEnabled = true
                progress.visibility = View.GONE
                Snackbar.make(requireView(), "Failed to list product", Snackbar.LENGTH_LONG).show()
            }
    }

    private fun clearFields() {
        etTitle.setText("")
        etDesc.setText("")
        etLink.setText("")
        etPrice.setText("")
        etCategory.setText("", false)
        etRecommended.setText("", false)
        tvDescCounter.text = "0 / $MAX_WORDS"
    }

    private fun moveToListings() {
        progress.visibility = View.GONE
        btnList.isEnabled = true

        val act = activity ?: return
        val bottomNav = act.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavVendor)

        if (bottomNav != null) {
            bottomNav.selectedItemId = R.id.nav_vendor_listings
            return
        }

        val vp = act.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPagerVendor)
        vp?.setCurrentItem(
            VendorPagerAdapter.Page.values().indexOf(VendorPagerAdapter.Page.LISTINGS),
            false
        )
    }
}
