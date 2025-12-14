package com.example.fyp

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.fyp.models.RegisteredClinic
import com.example.fyp.providers.ClinicCategory
import com.example.fyp.providers.OsmClinicProvider
import com.example.fyp.ui.NearbyClinicAdapter
import com.example.fyp.ui.RegisteredClinicAdapter
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ClinicsFragment : Fragment(R.layout.activity_clinics_fragment) {

    private lateinit var rvRegistered: RecyclerView
    private lateinit var rvNearby: RecyclerView
    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var empty: TextView

    private lateinit var btnRegistered: MaterialButton
    private lateinit var btnNearby: MaterialButton

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var registeredAdapter: RegisteredClinicAdapter
    private lateinit var nearbyAdapter: NearbyClinicAdapter

    private val osmProvider = OsmClinicProvider()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvRegistered = view.findViewById(R.id.rvRegistered)
        rvNearby = view.findViewById(R.id.rvNearby)
        swipe = view.findViewById(R.id.swipeRefresh)
        empty = view.findViewById(R.id.tvEmpty)

        btnRegistered = view.findViewById(R.id.btnRegistered)
        btnNearby = view.findViewById(R.id.btnNearby)

        rvRegistered.layoutManager = LinearLayoutManager(requireContext())
        rvNearby.layoutManager = LinearLayoutManager(requireContext())

        registeredAdapter = RegisteredClinicAdapter(requireContext(), listOf())
        nearbyAdapter = NearbyClinicAdapter(requireContext(), listOf())

        rvRegistered.adapter = registeredAdapter
        rvNearby.adapter = nearbyAdapter

        swipe.setOnRefreshListener {
            if (btnRegistered.isChecked) loadRegisteredClinics()
            else loadNearby()
        }

        btnRegistered.setOnClickListener {
            btnRegistered.isChecked = true
            btnNearby.isChecked = false
            updateTabUI("registered")
            showRegisteredUI()
            loadRegisteredClinics()
        }

        btnNearby.setOnClickListener {
            btnNearby.isChecked = true
            btnRegistered.isChecked = false
            updateTabUI("nearby")
            showNearbyUI()
            loadNearby()
        }

        // DEFAULT TAB
        btnRegistered.isChecked = true
        btnNearby.isChecked = false
        updateTabUI("registered")
        showRegisteredUI()
        loadRegisteredClinics()

    }

    // -------------------------- UI SWITCHING -----------------------------

    private fun showRegisteredUI() {
        rvRegistered.visibility = View.VISIBLE
        rvNearby.visibility = View.GONE
        empty.visibility = View.GONE
    }

    private fun showNearbyUI() {
        rvRegistered.visibility = View.GONE
        rvNearby.visibility = View.VISIBLE
        empty.visibility = View.GONE
    }

    private fun updateTabUI(active: String) {

        val teal = requireContext().getColor(R.color.teal_light)
        val white = requireContext().getColor(R.color.white)
        val black = requireContext().getColor(R.color.black)
        val tealText = requireContext().getColor(R.color.teal_bg)

        if (active == "registered") {
            btnRegistered.setBackgroundColor(teal)
            btnRegistered.setTextColor(tealText)

            btnNearby.setBackgroundColor(white)
            btnNearby.setTextColor(black)

            rvRegistered.visibility = View.VISIBLE
            rvNearby.visibility = View.GONE

        } else {
            btnNearby.setBackgroundColor(teal)
            btnNearby.setTextColor(tealText)

            btnRegistered.setBackgroundColor(white)
            btnRegistered.setTextColor(black)

            rvRegistered.visibility = View.GONE
            rvNearby.visibility = View.VISIBLE
        }
    }



    // ----------------------- LOAD REGISTERED CLINICS ---------------------

    private fun loadRegisteredClinics() {
        swipe.isRefreshing = true

        db.collection("users")
            .whereEqualTo("userType", "clinic")
            .get()
            .addOnSuccessListener { snap ->
                val clinics = mutableListOf<RegisteredClinic>()

                for (doc in snap.documents) {
                    try {
                        clinics.add(
                            RegisteredClinic(
                                clinicName = doc.getString("clinicName"),
                                clinicAddress = doc.getString("clinicAddress"),
                                clinicPhone = doc.getString("clinicPhone"),
                                clinicWebsite = doc.getString("clinicWebsite"),
                                clinicServices = doc.get("clinicServices") as? List<String> ?: emptyList(),
                                clinicAvgRating = doc.getDouble("clinicAvgRating") ?: 0.0,
                                clinicRatingsCount = (doc.getLong("clinicRatingsCount") ?: 0).toInt(),
                                lat = doc.getDouble("lat"),
                                lon = doc.getDouble("lon"),
                                location = doc.get("location") as? Map<String, Any>
                            )
                        )
                    } catch (e: Exception) {
                        Log.e("REGISTERED_CLINICS", "Mapping error: $e")
                    }
                }

                swipe.isRefreshing = false

                if (clinics.isEmpty()) {
                    empty.visibility = View.VISIBLE
                    empty.text = "No clinics registered yet."
                } else {
                    empty.visibility = View.GONE
                }

                registeredAdapter.update(clinics)
            }
            .addOnFailureListener {
                swipe.isRefreshing = false
                empty.visibility = View.VISIBLE
                empty.text = "Failed to load clinics."
            }
    }

    // ----------------------- LOAD NEARBY CLINICS -------------------------

    private fun loadNearby() {
        swipe.isRefreshing = true

        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val lat =
                    doc.getDouble("lat")
                        ?: (doc.get("location") as? Map<*, *>)?.get("lat") as? Double
                val lon =
                    doc.getDouble("lon")
                        ?: (doc.get("location") as? Map<*, *>)?.get("lng") as? Double

                if (lat == null || lon == null) {
                    swipe.isRefreshing = false
                    empty.visibility = View.VISIBLE
                    empty.text = "Enable location to find nearby clinics."
                    return@addOnSuccessListener
                }

                osmProvider.searchClinics(lat, lon, 20000, ClinicCategory.EYE) { list ->
                    requireActivity().runOnUiThread {
                        swipe.isRefreshing = false

                        if (list.isNullOrEmpty()) {
                            empty.visibility = View.VISIBLE
                            empty.text = "No clinics found near you."
                            nearbyAdapter.update(listOf())
                        } else {
                            empty.visibility = View.GONE
                            nearbyAdapter.update(list)
                        }
                    }
                }
            }
            .addOnFailureListener {
                swipe.isRefreshing = false
                empty.visibility = View.VISIBLE
                empty.text = "Failed to load location."
            }
    }
}
