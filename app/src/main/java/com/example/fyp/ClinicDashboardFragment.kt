package com.example.fyp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.fyp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ClinicDashboardFragment : Fragment() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private var tvGreeting: TextView? = null
    private var tvTotalClicks: TextView? = null
    private var tvAvgRating: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.activity_clinic_dashboard_fragment, container, false)
        tvGreeting = v.findViewById(R.id.tvClinicGreeting)
        tvTotalClicks = v.findViewById(R.id.tvTotalClicks)
        tvAvgRating = v.findViewById(R.id.tvAvgRating)
        return v
    }

    override fun onResume() {
        super.onResume()
        loadHeader()
    }

    private fun loadHeader() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("clinicName") ?: "Clinic"
                tvGreeting?.text = "Hello, $name"

                // placeholders; you can plug your real stats later
                val clicks = doc.getLong("stats_totalClicks") ?: 0L
                val rating = doc.getDouble("stats_avgRating") ?: 0.0

                tvTotalClicks?.text = "Total clicks: $clicks"
                tvAvgRating?.text = "Average rating: ${"%.1f".format(rating)}"
            }
    }
}
