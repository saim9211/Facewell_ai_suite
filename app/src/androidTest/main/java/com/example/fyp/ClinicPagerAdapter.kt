package com.example.fyp

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ClinicPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {

    enum class Page { DASHBOARD, PROFILE }

    override fun getItemCount() = Page.values().size

    override fun createFragment(position: Int): Fragment = when (Page.values()[position]) {
        Page.DASHBOARD -> ClinicDashboardFragment()
        Page.PROFILE -> ClinicProfileFragment()
    }
}
