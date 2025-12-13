package com.example.fyp

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class VendorProfileFragment : Fragment() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private var tvName: TextView? = null
    private var tvEmail: TextView? = null
    private var tvPhone: TextView? = null
    private var tvCity: TextView? = null
    private var tvWebsite: TextView? = null

    private var btnEdit: MaterialButton? = null
    private var rowLogout: View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.activity_vendor_profile_fragment, container, false)

        tvName = v.findViewById(R.id.tvVendorName)
        tvEmail = v.findViewById(R.id.tvVendorEmail)
        tvPhone = v.findViewById(R.id.tvVendorPhone)
        tvCity = v.findViewById(R.id.tvVendorCity)
        tvWebsite = v.findViewById(R.id.tvVendorWebsite)

        btnEdit = v.findViewById(R.id.btnEditVendorProfile)
        rowLogout = v.findViewById(R.id.rowLogoutVendor)

        btnEdit?.setOnClickListener {
            startActivity(Intent(requireContext(), VendorEditProfileActivity::class.java))
        }

        rowLogout?.setOnClickListener { showLogoutDialog() }

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
                tvName?.text = doc.getString("vendorName") ?: "-"
                tvEmail?.text = doc.getString("email") ?: "-"
                tvPhone?.text = doc.getString("phone") ?: "-"
                tvCity?.text = doc.getString("city") ?: "-"
                tvWebsite?.text = doc.getString("vendorWebsite") ?: "-"
            }
    }

    private fun showLogoutDialog() {
        val dialog = android.app.Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_logout)
        dialog.setCancelable(true)

        dialog.findViewById<View>(R.id.btnCancel)?.setOnClickListener { dialog.dismiss() }
        dialog.findViewById<View>(R.id.btnLogoutConfirm)?.setOnClickListener {
            dialog.dismiss()
            doLogout()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun doLogout() {
        auth.signOut()

        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)

        requireActivity().finish()
    }
}
