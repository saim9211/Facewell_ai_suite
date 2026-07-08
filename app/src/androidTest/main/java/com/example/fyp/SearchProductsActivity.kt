package com.example.fyp

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore

class SearchProductsActivity : AppCompatActivity() {

    private lateinit var etSearch: TextInputEditText
    private lateinit var btnGeneral: MaterialButton
    private lateinit var btnEye: MaterialButton
    private lateinit var btnSkin: MaterialButton
    private lateinit var btnStress: MaterialButton
    private lateinit var btnBack: ImageView

    private lateinit var rv: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var swipeRefresh: SwipeRefreshLayout

    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: SearchProductAdapter

    private val allProducts = mutableListOf<Product>()
    private var currentCategory = "general"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_products)

        btnBack = findViewById(R.id.btnBack)
        etSearch = findViewById(R.id.etSearch)
        btnGeneral = findViewById(R.id.btnGeneral)
        btnEye = findViewById(R.id.btnEye)
        btnSkin = findViewById(R.id.btnSkin)
        btnStress = findViewById(R.id.btnStress)
        rv = findViewById(R.id.rvProducts)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        emptyText = findViewById(R.id.tvEmptyState)

        rv.layoutManager = LinearLayoutManager(this)

        adapter = SearchProductAdapter(listOf()) { product ->
            ProductDetailBottomSheet.newInstance(product)
                .show(supportFragmentManager, "detailDialog")
        }
        rv.adapter = adapter

        btnBack.setOnClickListener { finish() }

        // SWIPE TO REFRESH
        swipeRefresh.setOnRefreshListener {
            fetchProducts(true)
        }

        fetchProducts()
        setupSearch()
        setupFilters()
    }

    /** Fetch products from Firestore */
    private fun fetchProducts(isRefreshing: Boolean = false) {
        if (!isRefreshing) swipeRefresh.isRefreshing = true

        db.collection("products")
            .whereEqualTo("isActive", true)   // ✔ ONLY filter
            .get()
            .addOnSuccessListener { snap ->
                allProducts.clear()

                for (d in snap.documents) {
                    val p = d.toObject(Product::class.java) ?: continue
                    allProducts.add(p)   // add ALL active products
                }

                applyFilters()
                swipeRefresh.isRefreshing = false
            }
            .addOnFailureListener {
                swipeRefresh.isRefreshing = false
            }

    }

    /** Search bar listener */
    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { applyFilters() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    /** Filter selection buttons */
    private fun setupFilters() {

        fun resetButtons() {
            btnGeneral.setBackgroundColor(getColor(R.color.white))
            btnEye.setBackgroundColor(getColor(R.color.white))
            btnSkin.setBackgroundColor(getColor(R.color.white))
            btnStress.setBackgroundColor(getColor(R.color.white))

            btnGeneral.setTextColor(getColor(R.color.black))
            btnEye.setTextColor(getColor(R.color.black))
            btnSkin.setTextColor(getColor(R.color.black))
            btnStress.setTextColor(getColor(R.color.black))
        }

        btnGeneral.setOnClickListener {
            resetButtons()
            btnGeneral.setBackgroundColor(getColor(R.color.teal_light))
            currentCategory = "general"
            applyFilters()
        }

        btnEye.setOnClickListener {
            resetButtons()
            btnEye.setBackgroundColor(getColor(R.color.teal_light))
            currentCategory = "eye"
            applyFilters()
        }

        btnSkin.setOnClickListener {
            resetButtons()
            btnSkin.setBackgroundColor(getColor(R.color.teal_light))
            currentCategory = "skin"
            applyFilters()
        }

        btnStress.setOnClickListener {
            resetButtons()
            btnStress.setBackgroundColor(getColor(R.color.teal_light))
            currentCategory = "stress"
            applyFilters()
        }
    }

    /** Apply search + category filter */
    private fun applyFilters() {
        val query = etSearch.text?.toString()?.lowercase()?.trim() ?: ""

        val filtered = allProducts.filter { p ->

            val textMatch = p.title.lowercase().contains(query)

            val categoryMatch =
                currentCategory == "general" ||
                        p.category.equals(currentCategory, ignoreCase = true)

            textMatch && categoryMatch
        }

        adapter.update(filtered)

        // Show "No products" message
        emptyText.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }
}
