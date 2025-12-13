package com.example.fyp

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore

class EditProductActivity : AppCompatActivity() {

    private val db by lazy { FirebaseFirestore.getInstance() }

    private lateinit var etTitle: TextInputEditText
    private lateinit var etDesc: TextInputEditText
    private lateinit var etLink: TextInputEditText
    private lateinit var etPrice: TextInputEditText
    private lateinit var etCategory: MaterialAutoCompleteTextView
    private lateinit var etRecommended: MaterialAutoCompleteTextView
    private lateinit var btnSave: MaterialButton
    private lateinit var progress: ProgressBar

    private lateinit var product: Product

    private val categoryLabels = listOf(
        "Ophthalmology Clinic (eye)",
        "Dermatology & Aesthetics (skin)",
        "Mental Health & Wellness (mood)",
        "Other"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_product)

        product = intent.getSerializableExtra("product") as Product

        etTitle = findViewById(R.id.etTitle)
        etDesc = findViewById(R.id.etDesc)
        etLink = findViewById(R.id.etLink)
        etPrice = findViewById(R.id.etPrice)
        etCategory = findViewById(R.id.etCategory)
        etRecommended = findViewById(R.id.etRecommended)
        btnSave = findViewById(R.id.btnSave)
        progress = findViewById(R.id.progressEdit)

        setupCategoryDropdown()
        populateFields()

        btnSave.setOnClickListener { saveChanges() }
    }

    private fun populateFields() {
        etTitle.setText(product.title)
        etDesc.setText(product.description)
        etLink.setText(product.link ?: "")
        etPrice.setText((product.price ?: 0.0).toString())

        val categoryLabel = when (product.category) {
            "eye" -> categoryLabels[0]
            "skin" -> categoryLabels[1]
            "mood" -> categoryLabels[2]
            else -> categoryLabels[3]
        }
        etCategory.setText(categoryLabel, false)

        loadRecommendedDiseases(product.category)
        etRecommended.setText(product.recommendedFor.firstOrNull() ?: "", false)
    }

    private fun setupCategoryDropdown() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, categoryLabels)
        etCategory.setAdapter(adapter)

        etCategory.setOnItemClickListener { _, _, pos, _ ->
            val category = when (pos) {
                0 -> "eye"
                1 -> "skin"
                2 -> "mood"
                else -> "other"
            }
            loadRecommendedDiseases(category)
        }
    }

    private fun loadRecommendedDiseases(category: String) {
        val list = when (category) {
            "eye" -> DiseaseConstants.EYE_DISEASES
            "skin" -> DiseaseConstants.SKIN_DISEASES
            "mood" -> DiseaseConstants.MOOD_CLASSES
            else -> emptyList()
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, list)
        etRecommended.setAdapter(adapter)
    }

    private fun saveChanges() {

        // Disable button + show loader
        btnSave.isEnabled = false
        progress.visibility = View.VISIBLE

        val updatedCategory = when {
            etCategory.text.toString().contains("eye", true) -> "eye"
            etCategory.text.toString().contains("skin", true) -> "skin"
            etCategory.text.toString().contains("mood", true) -> "mood"
            else -> "other"
        }

        val map = mapOf(
            "title" to etTitle.text.toString().trim(),
            "description" to etDesc.text.toString().trim(),
            "link" to etLink.text.toString().trim(),
            "price" to etPrice.text.toString().toDoubleOrNull(),
            "category" to updatedCategory,
            "recommendedFor" to listOf(etRecommended.text.toString().trim()),
            "updatedAt" to System.currentTimeMillis()
        )

        db.collection("products")
            .document(product.id)
            .update(map)
            .addOnSuccessListener {
                finish()   // Return to listings
            }
            .addOnFailureListener {
                it.printStackTrace()
                Toast.makeText(this, "Failed to update", Toast.LENGTH_SHORT).show()

                // Re-enable button + hide loader
                btnSave.isEnabled = true
                progress.visibility = View.GONE
            }
    }
}
