package com.example.fyp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ClinicDashboardFragment : Fragment() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private var tvGreeting: TextView? = null
    private var tvSub: TextView? = null
    private var tvLocationPhone: TextView? = null

    private var tvTotalClicksValue: TextView? = null
    private var tvAvgRatingValue: TextView? = null
    private var tvActiveProductsValue: TextView? = null
    private var tvUpcomingApptsValue: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.activity_clinic_dashboard_fragment, container, false)

        tvGreeting = v.findViewById(R.id.tvClinicGreeting)
        tvSub = v.findViewById(R.id.tvClinicSub)
        tvLocationPhone = v.findViewById(R.id.tvClinicLocationPhone)

        tvTotalClicksValue = v.findViewById(R.id.tvTotalClicksValue)
        tvAvgRatingValue = v.findViewById(R.id.tvAvgRatingValue)
        tvActiveProductsValue = v.findViewById(R.id.tvActiveProductsValue)
        tvUpcomingApptsValue = v.findViewById(R.id.tvUpcomingApptsValue)

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
                // Clinic display name (fallback)
                val name = doc.getString("clinicName") ?: doc.getString("vendorName") ?: "Clinic"
                tvGreeting?.text = "$name"

                // city + phone
                val city = doc.getString("city") ?: ""
                val phone = doc.getString("clinicPhone") ?: doc.getString("phone") ?: ""
                val locPhone = if (city.isNotBlank() && phone.isNotBlank()) "$city  •  $phone"
                else if (city.isNotBlank()) city
                else if (phone.isNotBlank()) phone
                else ""
                tvLocationPhone?.text = locPhone

                // Stats (placeholders; replace with your real fields if present)
                val clicks = (doc.getLong("stats_totalClicks") ?: 0L)
                val rating = (doc.getDouble("stats_avgRating") ?: 0.0)
                val activeProducts = (doc.getLong("stats_activeProducts") ?: 0L)
                val upcoming = (doc.getLong("stats_upcomingAppointments") ?: 0L)

                tvTotalClicksValue?.text = clicks.toString()
                tvAvgRatingValue?.text = "%.1f".format(rating)
                tvActiveProductsValue?.text = activeProducts.toString()
                tvUpcomingApptsValue?.text = upcoming.toString()
            }
            .addOnFailureListener {
                // keep defaults if read failed
            }
    }
}
