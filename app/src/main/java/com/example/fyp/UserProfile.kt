package com.example.fyp

import java.io.Serializable

data class UserProfile(
    val uid: String = "",
    val email: String? = null,
    val phone: String? = null,

    val userType: String? = null,
    val stage: Int = 0,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val isVerified: Boolean = false,
    val status: String? = "active",

    val displayName: String? = null,
    val avatarUrl: String? = null,
    val coverPhotoUrl: String? = null,

    // USER FIELDS
    val firstName: String? = null,
    val lastName: String? = null,
    val gender: String? = null,
    val dob: String? = null,

    // LOCATION
    val locationEnabled: Boolean = false,
    val city: String? = null,
    val addressLine: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,

    // ⭐ PRODUCT RATING SYSTEM
    val ratedProducts: List<String> = emptyList(),

    // -----------------------------
    // ⭐ VENDOR ANALYTICS FIELDS
    // -----------------------------
    val vendorName: String? = null,
    val vendorDescription: String? = null,
    val vendorWebsite: String? = null,

    val vendorProducts: List<String> = emptyList(),
    val vendorLogoUrl: String? = null,
    val vendorCoverUrl: String? = null,

    val avgRating: Double? = null,       // vendor-wide average rating
    val ratingsCount: Int = 0,          // vendor rating count
    val totalClicks: Long = 0L,         // vendor clicks (product-based)
    val totalSales: Double = 0.0,
    val currency: String? = "PKR",

    val businessHours: Map<String, String>? = null,
    val paymentProviderId: String? = null,

    // -----------------------------
    // ⭐ CLINIC ANALYTICS FIELDS
    // -----------------------------
    val clinicName: String? = null,
    val clinicAddress: String? = null,
    val clinicPhone: String? = null,
    val clinicWebsite: String? = null,

    val clinicServices: List<String> = emptyList(),

    val clinicAvgRating: Double? = null,     // still valid
    val clinicRatingsCount: Int = 0,         // rating count (already present)

    val clinicTotalClicks: Long = 0L,        // NEW → CTR / total clicks from users
    val clinicContactClicks: Long = 0L,      // NEW → when user taps "Call / Contact"

    val appointmentsCount: Int = 0,          // unused but kept
    val clinicType: String? = null,
    val clinicSpecialties: List<String> = emptyList(),

    val tags: List<String> = emptyList()
) : Serializable
