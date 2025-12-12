package com.example.fyp

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class VendorPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {

    enum class Page { DASHBOARD, ADD, LISTINGS, PROFILE }

    override fun getItemCount() = Page.values().size

    override fun createFragment(position: Int): Fragment = when (Page.values()[position]) {
        Page.DASHBOARD -> VendorDashboardFragment()
        Page.ADD -> VendorAddProductFragment()
        Page.LISTINGS -> VendorListingsFragment()
        Page.PROFILE -> VendorProfileFragment()
    }
}
