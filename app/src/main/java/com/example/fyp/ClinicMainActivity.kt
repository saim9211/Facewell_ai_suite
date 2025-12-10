package com.example.fyp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewpager2.widget.ViewPager2
import com.example.fyp.LoginActivity
import com.example.fyp.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class ClinicMainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var overlayContainer: android.view.View

    private var overlayVisible = false
    private var overlayListener: FragmentManager.OnBackStackChangedListener? = null

    private val idToPage = mapOf(
        R.id.nav_clinic_dashboard to ClinicPagerAdapter.Page.DASHBOARD,
        R.id.nav_clinic_profile to ClinicPagerAdapter.Page.PROFILE
    )
    private val pageToId = idToPage.entries.associate { (id, page) -> page to id }

    private val firebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        val user = auth.currentUser
        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_clinic_main)

        viewPager = findViewById(R.id.viewPagerClinic)
        bottomNav = findViewById(R.id.bottomNavClinic)
        overlayContainer = findViewById(R.id.overlayContainerClinic)

        viewPager.adapter = ClinicPagerAdapter(this)
        viewPager.offscreenPageLimit = 1
        viewPager.isUserInputEnabled = true

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val page = ClinicPagerAdapter.Page.values()[position]
                pageToId[page]?.let { if (bottomNav.selectedItemId != it) bottomNav.selectedItemId = it }
            }
        })

        bottomNav.setOnItemSelectedListener { item ->
            val page = idToPage[item.itemId] ?: return@setOnItemSelectedListener false
            val index = ClinicPagerAdapter.Page.values().indexOf(page)
            if (overlayVisible) {
                closeOverlayThen { viewPager.setCurrentItem(index, false) }
            } else {
                viewPager.setCurrentItem(index, false)
            }
            true
        }

        bottomNav.setOnItemReselectedListener { /* optional scroll-to-top later */ }

        bottomNav.selectedItemId = R.id.nav_clinic_dashboard
        overlayContainer.visibility = android.view.View.GONE
    }

    override fun onStart() {
        super.onStart()
        firebaseAuth.addAuthStateListener(authStateListener)
    }

    override fun onStop() {
        super.onStop()
        firebaseAuth.removeAuthStateListener(authStateListener)
    }

    fun openOverlay(fragment: Fragment, tag: String) {
        overlayVisible = true
        overlayContainer.visibility = android.view.View.VISIBLE

        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .add(R.id.overlayContainerClinic, fragment, tag)
            .addToBackStack(tag)
            .commit()

        overlayListener?.let { supportFragmentManager.removeOnBackStackChangedListener(it) }
        overlayListener = object : FragmentManager.OnBackStackChangedListener {
            override fun onBackStackChanged() {
                if (supportFragmentManager.backStackEntryCount == 0) {
                    supportFragmentManager.removeOnBackStackChangedListener(this)
                    overlayVisible = false
                    overlayContainer.visibility = android.view.View.GONE
                    overlayListener = null
                }
            }
        }
        supportFragmentManager.addOnBackStackChangedListener(overlayListener!!)
    }

    private fun closeOverlayThen(afterClosed: () -> Unit) {
        val listener = object : FragmentManager.OnBackStackChangedListener {
            override fun onBackStackChanged() {
                if (supportFragmentManager.backStackEntryCount == 0) {
                    supportFragmentManager.removeOnBackStackChangedListener(this)
                    overlayVisible = false
                    overlayContainer.visibility = android.view.View.GONE
                    afterClosed()
                }
            }
        }
        supportFragmentManager.addOnBackStackChangedListener(listener)
        supportFragmentManager.popBackStack()
    }
}
