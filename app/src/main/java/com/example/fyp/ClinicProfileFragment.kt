package com.example.fyp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.example.fyp.utils.SessionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ClinicProfileFragment : Fragment() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val sessionManager by lazy { SessionManager(requireContext()) }
    private val googleClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(requireActivity(), gso)
    }

    private var tvName: TextView? = null
    private var tvEmail: TextView? = null
    private var tvPhone: TextView? = null
    private var tvCity: TextView? = null
    private var tvAddress: TextView? = null
    private var tvWebsite: TextView? = null

    private var btnEdit: MaterialButton? = null
    private var rowLogout: View? = null

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

        btnEdit = v.findViewById(R.id.btnEditClinicProfile)
        rowLogout = v.findViewById(R.id.rowLogoutClinic)

        btnEdit?.setOnClickListener {
            // Open edit activity
            startActivity(Intent(requireContext(), ClinicEditProfileActivity::class.java))
        }

        rowLogout?.setOnClickListener {
            showLogoutDialog()
        }

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
                tvName?.text = doc.getString("clinicName") ?: "-"
                tvEmail?.text = doc.getString("email") ?: "-"
                tvPhone?.text = doc.getString("clinicPhone") ?: doc.getString("phone") ?: "-"
                tvCity?.text = doc.getString("city") ?: "-"
                tvAddress?.text = doc.getString("clinicAddress") ?: "-"
                tvWebsite?.text = doc.getString("clinicWebsite") ?: "-"
            }
            .addOnFailureListener {
                // keep previous values / show nothing
            }
    }

//    private fun showLogoutConfirm() {
//        // Use Material AlertDialog for consistent app theme
//        val ctx = requireContext()
//        MaterialAlertDialogBuilder(ctx)
//            .setTitle("Confirm logout")
//            .setMessage("Are you sure you want to logout?")
//            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
//            .setPositiveButton("Logout") { _, _ ->
//                doLogoutConfirmed()
//            }
//            .show()
//    }

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
        // 1. Clear session
        sessionManager.clearSession()

        // 2. Auth signOut
        auth.signOut()

        // 3. Google signOut + revoke
        googleClient.signOut().addOnCompleteListener {
            googleClient.revokeAccess().addOnCompleteListener {
                navigateToLogin()
            }
        }.addOnFailureListener {
            navigateToLogin()
        }
    }

    private fun navigateToLogin() {
        if (!isAdded) return
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
        requireActivity().finish()
    }


}
