package com.example.fyp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.fyp.LoginActivity
import com.example.fyp.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ClinicProfileFragment : Fragment() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private var tvName: TextView? = null
    private var tvEmail: TextView? = null
    private var tvPhone: TextView? = null
    private var tvCity: TextView? = null
    private var tvAddress: TextView? = null
    private var tvWebsite: TextView? = null
    private var btnLogout: MaterialButton? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.activity_clinic_profile_fragment, container, false)

        tvName = v.findViewById(R.id.tvClinicName)
        tvEmail = v.findViewById(R.id.tvClinicEmail)
        tvPhone = v.findViewById(R.id.tvClinicPhone)
        tvCity = v.findViewById(R.id.tvClinicCity)
        tvAddress = v.findViewById(R.id.tvClinicAddress)
        tvWebsite = v.findViewById(R.id.tvClinicWebsite)
        btnLogout = v.findViewById(R.id.btnLogoutClinic)

        btnLogout?.setOnClickListener { doLogout() }

        return v
    }

    override fun onResume() {
        super.onResume()
        loadProfile()
    }

    private fun loadProfile() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("clinicName") ?: "Clinic"
                val email = doc.getString("email") ?: "-"
                val phone = doc.getString("clinicPhone") ?: doc.getString("phone") ?: "-"
                val city = doc.getString("city") ?: "-"
                val address = doc.getString("clinicAddress") ?: "-"
                val website = doc.getString("clinicWebsite") ?: "-"

                tvName?.text = "Name: $name"
                tvEmail?.text = "Email: $email"
                tvPhone?.text = "Phone: $phone"
                tvCity?.text = "City: $city"
                tvAddress?.text = "Address: $address"
                tvWebsite?.text = "Website: $website"
            }
    }

    private fun doLogout() {
        auth.signOut()
        val ctx = requireContext()
        val i = Intent(ctx, LoginActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(i)
        requireActivity().finish()
    }
}
