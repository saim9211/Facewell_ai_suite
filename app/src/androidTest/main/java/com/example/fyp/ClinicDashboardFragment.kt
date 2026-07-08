package com.example.fyp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ClinicDashboardFragment : Fragment() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private lateinit var swipeRefresh: SwipeRefreshLayout

    private var tvGreeting: TextView? = null
    private var tvLocationPhone: TextView? = null

    private var tvTotalClicksValue: TextView? = null
    private var tvAvgRatingValue: TextView? = null
    private var tvRatingCountValue: TextView? = null
    private var tvContactClicksValue: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.activity_clinic_dashboard_fragment, container, false)

        swipeRefresh = v.findViewById(R.id.swipeClinic)

        tvGreeting = v.findViewById(R.id.tvClinicGreeting)
        tvLocationPhone = v.findViewById(R.id.tvClinicLocationPhone)

        tvTotalClicksValue = v.findViewById(R.id.tvTotalClicksValue)
        tvAvgRatingValue = v.findViewById(R.id.tvAvgRatingValue)
        tvRatingCountValue = v.findViewById(R.id.tvRatingCountValue)
        tvContactClicksValue = v.findViewById(R.id.tvContactClicksValue)

        swipeRefresh.setOnRefreshListener { loadDashboard() }

        return v
    }

    override fun onResume() {
        super.onResume()
        loadDashboard()
    }

    private fun loadDashboard() {
        val uid = auth.currentUser?.uid ?: return
        swipeRefresh.isRefreshing = true

        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->

                val name = doc.getString("clinicName") ?: "Clinic"
                tvGreeting?.text = name

                val city = doc.getString("city") ?: ""
                val phone = doc.getString("clinicPhone") ?: doc.getString("phone") ?: ""

                tvLocationPhone?.text = listOf(city, phone)
                    .filter { it.isNotBlank() }
                    .joinToString(" • ")

                val clicks = doc.getLong("clinicClicks") ?: 0L
                val avgRating = doc.getDouble("clinicAvgRating") ?: 0.0
                val ratingCount = doc.getLong("clinicRatingsCount") ?: 0L
                val contactClicks = doc.getLong("clinicContactClicks") ?: 0L

                tvTotalClicksValue?.text = clicks.toString()
                tvAvgRatingValue?.text = String.format("%.1f", avgRating)
                tvRatingCountValue?.text = ratingCount.toString()
                tvContactClicksValue?.text = contactClicks.toString()

                swipeRefresh.isRefreshing = false
            }
            .addOnFailureListener {
                swipeRefresh.isRefreshing = false
            }
    }
}
