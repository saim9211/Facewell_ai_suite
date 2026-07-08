package com.example.fyp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class ProductDetailBottomSheet : BottomSheetDialogFragment() {

    private lateinit var product: Product
    private lateinit var ratingBar: RatingBar
    private lateinit var btnSubmit: Button

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        product = arguments?.getSerializable("product") as Product
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.activity_product_detail_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val img = view.findViewById<ImageView>(R.id.imgProduct)
        val title = view.findViewById<TextView>(R.id.tvTitle)
        val cat = view.findViewById<TextView>(R.id.tvCategory)
        val desc = view.findViewById<TextView>(R.id.tvDesc)
        val btnBuy = view.findViewById<Button>(R.id.btnBuyNow)

        ratingBar = view.findViewById(R.id.ratingBar)
        btnSubmit = view.findViewById(R.id.btnSubmitRating)

        Glide.with(requireContext())
            .load(product.images.firstOrNull())
            .placeholder(R.drawable.img_product_placeholder)
            .into(img)

        title.text = product.title
        cat.text = product.category
        desc.text = product.description

        btnBuy.setOnClickListener {
            val link = product.link
            if (link.isNullOrBlank()) {
                Toast.makeText(requireContext(), "No link available", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
        }

        setupRatingUI()
    }

    private fun setupRatingUI() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).get()
            .addOnSuccessListener { snap ->
                val ratedList = snap.get("ratedProducts") as? List<String> ?: emptyList()

                if (ratedList.contains(product.id)) {
                    val avg = (product.avgRating ?: 0.0).toFloat()
                    ratingBar.rating = avg
                    ratingBar.setIsIndicator(true)
                    btnSubmit.visibility = View.GONE

                } else {
                    ratingBar.setIsIndicator(false)
                    btnSubmit.visibility = View.VISIBLE

                    btnSubmit.setOnClickListener {
                        val stars = ratingBar.rating.toInt()

                        if (stars == 0) {
                            Toast.makeText(requireContext(), "Please select a rating", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }

                        submitRating(stars)
                    }
                }
            }
    }

    private fun submitRating(stars: Int) {
        val uid = auth.currentUser?.uid ?: return

        val productRef = db.collection("products").document(product.id)
        val userRef = db.collection("users").document(uid)

        btnSubmit.isEnabled = false
        btnSubmit.text = "Submitting..."

        db.runTransaction { tx ->
            val snap = tx.get(productRef)

            val oldAvg = snap.getDouble("avgRating") ?: 0.0
            val oldCount = snap.getLong("ratingsCount") ?: 0L

            val newAvg = ((oldAvg * oldCount) + stars) / (oldCount + 1)
            val newCount = oldCount + 1

            tx.update(productRef, mapOf(
                "avgRating" to newAvg,
                "ratingsCount" to newCount
            ))

            tx.update(userRef, "ratedProducts", FieldValue.arrayUnion(product.id))

            newAvg

        }.addOnSuccessListener { newAvg ->
            Log.d("RATING", "SUCCESS: newAvg=$newAvg")

            Toast.makeText(requireContext(), "Thanks for rating!", Toast.LENGTH_SHORT).show()

            ratingBar.rating = (newAvg as Double).toFloat()
            ratingBar.setIsIndicator(true)

            btnSubmit.visibility = View.GONE
            dismiss()

        }.addOnFailureListener { e ->
            Log.e("RATING_ERROR", "Rating failed", e)

            btnSubmit.isEnabled = true
            btnSubmit.text = "Submit Rating"
            Toast.makeText(requireContext(), "Failed to rate product", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        fun newInstance(product: Product): ProductDetailBottomSheet {
            val f = ProductDetailBottomSheet()
            f.arguments = Bundle().apply {
                putSerializable("product", product)
            }
            return f
        }
    }
}
