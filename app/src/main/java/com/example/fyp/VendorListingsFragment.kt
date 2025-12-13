package com.example.fyp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class VendorListingsFragment : Fragment() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progress: ProgressBar
    private lateinit var emptyContainer: View
    private lateinit var btnGoList: Button
    private lateinit var rv: RecyclerView
    private lateinit var tvHeading: TextView

    private val adapter by lazy { VendorProductAdapter(mutableListOf()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_vendor_listings_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        progress = view.findViewById(R.id.progressListings)
        emptyContainer = view.findViewById(R.id.emptyContainer)
        btnGoList = view.findViewById(R.id.btnGoListProduct)
        rv = view.findViewById(R.id.rvProducts)
        tvHeading = view.findViewById(R.id.tvHeading)

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        btnGoList.setOnClickListener { navigateToAdd() }

        swipeRefresh.setOnRefreshListener {
            loadProducts()
        }

        loadProducts()
    }

    private fun loadProducts() {
        val user = auth.currentUser ?: run {
            showEmpty("Not authenticated")
            swipeRefresh.isRefreshing = false
            return
        }

        // START LOADING UI
        if (!swipeRefresh.isRefreshing) {
            progress.visibility = View.VISIBLE
        }
        emptyContainer.visibility = View.GONE
        rv.visibility = View.GONE
        tvHeading.visibility = View.GONE

        db.collection("products")
            .whereEqualTo("vendorId", user.uid)
            .whereEqualTo("isActive", true)
            .get()
            .addOnSuccessListener { snap ->
                progress.visibility = View.GONE
                swipeRefresh.isRefreshing = false

                val docs = snap.documents
                if (docs.isEmpty()) {
                    showEmpty(null)
                    return@addOnSuccessListener
                }

                val items = docs.map { doc ->
                    Product(
                        id = doc.id,
                        vendorId = doc.getString("vendorId") ?: "",
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        category = doc.getString("category") ?: "other",
                        recommendedFor = (doc.get("recommendedFor") as? List<String>) ?: emptyList(),
                        link = doc.getString("link"),
                        images = (doc.get("images") as? List<String>) ?: emptyList(),
                        clicks = doc.getLong("clicks") ?: 0L,
                        views = doc.getLong("views") ?: 0L,
                        price = doc.getDouble("price"),          // 🔥 FIXED
                        currency = doc.getString("currency") ?: "PKR",
                        isActive = doc.getBoolean("isActive") ?: true,
                        isApproved = doc.getBoolean("isApproved") ?: false,
                        createdAt = doc.getLong("createdAt"),
                        updatedAt = doc.getLong("updatedAt")
                    )
                }


                tvHeading.visibility = View.VISIBLE
                adapter.replaceAll(items.toMutableList())
                rv.visibility = View.VISIBLE
            }
            .addOnFailureListener { e ->
                progress.visibility = View.GONE
                swipeRefresh.isRefreshing = false
                showEmpty("Failed to load: ${e.message}")
            }
    }

    private fun showEmpty(message: String?) {
        progress.visibility = View.GONE
        rv.visibility = View.GONE
        emptyContainer.visibility = View.VISIBLE
        tvHeading.visibility = View.GONE

        val tvSub = emptyContainer.findViewById<TextView>(R.id.tvEmptySub)
        tvSub.text = message ?: "You don't have any products live right now. Create a product to show in reports and let users buy from external links."

        swipeRefresh.isRefreshing = false
    }

    private fun navigateToAdd() {
        val act = activity ?: return

        val bottomNav = act.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavVendor)
        if (bottomNav != null) {
            bottomNav.selectedItemId = R.id.nav_vendor_add
            return
        }

        val vp = act.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPagerVendor)
        if (vp != null) {
            val idx = VendorPagerAdapter.Page.values().indexOf(VendorPagerAdapter.Page.ADD)
            vp.setCurrentItem(idx, false)
        }
    }
}
