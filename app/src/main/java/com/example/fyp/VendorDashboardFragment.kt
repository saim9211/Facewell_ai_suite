package com.example.fyp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class VendorDashboardFragment : Fragment() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private lateinit var tvGreeting: TextView
    private lateinit var tvLocationPhone: TextView
    private lateinit var tvTotalClicks: TextView
    private lateinit var tvAvgRating: TextView
    private lateinit var tvActiveProducts: TextView
    private lateinit var tvSales: TextView
    private lateinit var cardActiveProducts: MaterialCardView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_vendor_dashboard_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        tvGreeting = view.findViewById(R.id.tvVendorGreeting)
        tvLocationPhone = view.findViewById(R.id.tvVendorLocationPhone)
        tvTotalClicks = view.findViewById(R.id.tvVendorTotalClicksValue)
        tvAvgRating = view.findViewById(R.id.tvVendorAvgRatingValue)
        tvActiveProducts = view.findViewById(R.id.tvVendorActiveProductsValue)
        tvSales = view.findViewById(R.id.tvVendorSalesValue)
        cardActiveProducts = view.findViewById(R.id.cardVendorActiveProducts)

        // Defaults while loading
        tvTotalClicks.text = "0"
        tvAvgRating.text = "0.0"
        tvActiveProducts.text = "0"
        tvSales.text = "0"

        val user = auth.currentUser
        if (user == null) {
            // No user — keep defaults (the activity already listens to auth state)
            tvGreeting.text = "Vendor"
            tvLocationPhone.text = "City • 03xx-xxxxxxx"
            return
        }

        val uid = user.uid

        // 1) Load user profile (collection: "users", doc: uid)
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    val firstName = doc.getString("firstName") ?: doc.getString("name") ?: ""
                    val lastName = doc.getString("lastName") ?: ""
                    val city = doc.getString("city") ?: ""
                    val phone = doc.getString("phone") ?: user.phoneNumber ?: ""

                    val displayName = when {
                        firstName.isNotBlank() && lastName.isNotBlank() -> "$firstName $lastName"
                        firstName.isNotBlank() -> firstName
                        else -> user.displayName ?: "Vendor"
                    }

                    tvGreeting.text = displayName
                    tvLocationPhone.text = listOfNotNull(city.ifBlank { null }, phone.ifBlank { null })
                        .joinToString(" • ")
                        .ifBlank { "City • 03xx-xxxxxxx" }
                } else {
                    // fallback
                    tvGreeting.text = user.displayName ?: "Vendor"
                    tvLocationPhone.text = "City • 03xx-xxxxxxx"
                }
            }
            .addOnFailureListener {
                // keep defaults
                tvGreeting.text = user.displayName ?: "Vendor"
                tvLocationPhone.text = "City • 03xx-xxxxxxx"
            }

        // 2) Count active products for this vendor
        // Assumes products collection has field "vendorId"
        db.collection("products")
            .whereEqualTo("vendorId", uid)
            .get()
            .addOnSuccessListener { snap ->
                val count = snap.size()
                tvActiveProducts.text = count.toString()
            }
            .addOnFailureListener {
                tvActiveProducts.text = "0"
            }

        // 3) Active-products card click -> switch to Listings tab in VendorMainActivity
        cardActiveProducts.setOnClickListener {
            // Try to set bottom nav selected item (works because BottomNavigationView lives in the activity layout)
            val activity = activity
            if (activity != null) {
                val bottomNav = activity.findViewById<BottomNavigationView>(R.id.bottomNavVendor)
                if (bottomNav != null) {
                    bottomNav.selectedItemId = R.id.nav_vendor_listings
                    return@setOnClickListener
                }

                // fallback: try to find viewpager and set index directly (index of LISTINGS in VendorPagerAdapter)
                val vp = activity.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPagerVendor)
                if (vp != null) {
                    val index = VendorPagerAdapter.Page.values().indexOf(VendorPagerAdapter.Page.LISTINGS)
                    vp.setCurrentItem(index, false)
                }
            }
        }
    }
}
