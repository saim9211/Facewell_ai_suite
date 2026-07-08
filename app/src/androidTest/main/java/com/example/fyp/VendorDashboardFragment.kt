package com.example.fyp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class VendorDashboardFragment : Fragment() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private lateinit var swipeRefresh: SwipeRefreshLayout

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

        swipeRefresh = view.findViewById(R.id.swipeDashboard)

        tvGreeting = view.findViewById(R.id.tvVendorGreeting)
        tvLocationPhone = view.findViewById(R.id.tvVendorLocationPhone)
        tvTotalClicks = view.findViewById(R.id.tvVendorTotalClicksValue)
        tvAvgRating = view.findViewById(R.id.tvVendorAvgRatingValue)
        tvActiveProducts = view.findViewById(R.id.tvVendorActiveProductsValue)
        tvSales = view.findViewById(R.id.tvVendorSalesValue)
        cardActiveProducts = view.findViewById(R.id.cardVendorActiveProducts)

        // Default values while loading
        tvTotalClicks.text = "0"
        tvAvgRating.text = "0.0"
        tvActiveProducts.text = "0"
        tvSales.text = "0"

        swipeRefresh.setOnRefreshListener { loadVendorData() }

        loadVendorData()

        cardActiveProducts.setOnClickListener {
            val act = activity ?: return@setOnClickListener

            val bottomNav = act.findViewById<BottomNavigationView>(R.id.bottomNavVendor)
            if (bottomNav != null) {
                bottomNav.selectedItemId = R.id.nav_vendor_listings
                return@setOnClickListener
            }

            val vp = act.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPagerVendor)
            if (vp != null) {
                val index = VendorPagerAdapter.Page.values().indexOf(VendorPagerAdapter.Page.LISTINGS)
                vp.setCurrentItem(index, false)
            }
        }
    }


    private fun loadVendorData() {
        val user = auth.currentUser ?: run {
            swipeRefresh.isRefreshing = false
            return
        }

        val uid = user.uid
        swipeRefresh.isRefreshing = true

        // -----------------------------------
        // -----------------------------------
// 1️⃣ LOAD VENDOR PROFILE
// -----------------------------------
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {

                    val vendorName = doc.getString("vendorName") ?: ""   // ⭐ MAIN FIX
                    val firstName = doc.getString("firstName") ?: ""
                    val lastName = doc.getString("lastName") ?: ""
                    val city = doc.getString("city") ?: ""
                    val phone = doc.getString("phone") ?: ""

                    // Priority:
                    // 1) vendorName
                    // 2) firstName + lastName
                    // 3) "Vendor"
                    val displayName = when {
                        vendorName.isNotBlank() -> vendorName
                        firstName.isNotBlank() -> "$firstName $lastName"
                        else -> "Vendor"
                    }

                    tvGreeting.text = displayName

                    tvLocationPhone.text =
                        listOf(city, phone).filter { it.isNotBlank() }.joinToString(" • ")

                } else {
                    tvGreeting.text = user.displayName ?: "Vendor"
                    tvLocationPhone.text = "City • 03xx-xxxxxxx"
                }
            }
            .addOnFailureListener {
                tvGreeting.text = user.displayName ?: "Vendor"
                tvLocationPhone.text = "City • 03xx-xxxxxxx"
            }



        // -----------------------------------
        // 2️⃣ LOAD PRODUCTS → clicks + ratings + count
        // -----------------------------------
        db.collection("products")
            .whereEqualTo("vendorId", uid)
            .whereEqualTo("isActive", true)
            .get()
            .addOnSuccessListener { snap ->

                val products = snap.documents
                val totalProducts = products.size
                tvActiveProducts.text = totalProducts.toString()

                var totalClicks = 0L
                var totalRatingsSum = 0.0
                var ratedProductsCount = 0

                for (doc in products) {
                    // CLICK SUM
                    totalClicks += doc.getLong("clicks") ?: 0L

                    // RATING SUM
                    val avgR = doc.getDouble("avgRating") ?: 0.0
                    val rCount = doc.getLong("ratingsCount")?.toInt() ?: 0

                    if (rCount > 0) { // Only count rated products
                        totalRatingsSum += avgR
                        ratedProductsCount++
                    }
                }

                // UPDATE DASHBOARD CLICKS
                tvTotalClicks.text = totalClicks.toString()

                // UPDATE DASHBOARD AVG RATING
                val vendorAvgRating =
                    if (ratedProductsCount > 0) totalRatingsSum / ratedProductsCount else 0.0

                tvAvgRating.text = String.format("%.1f", vendorAvgRating)

                // DONE
                swipeRefresh.isRefreshing = false
            }
            .addOnFailureListener {
                tvActiveProducts.text = "0"
                tvTotalClicks.text = "0"
                tvAvgRating.text = "0.0"
                swipeRefresh.isRefreshing = false
            }
    }
}
