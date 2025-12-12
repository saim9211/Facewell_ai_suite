package com.example.fyp

import java.io.Serializable

data class UserProfile(
    val uid: String = "",
    val email: String? = null,
    val phone: String? = null,

    // App-wide fields
    val userType: String? = null,      // "user" | "vendor" | "clinic"
    val stage: Int = 0,                // onboarding stage
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val isVerified: Boolean = false,
    val status: String? = "active",    // "active" | "suspended" | "banned"

    // DISPLAY
    val displayName: String? = null,   // fallback name for greeting (vendorName/clinicName or first+last)
    val avatarUrl: String? = null,     // user/vendor/clinic avatar/logo
    val coverPhotoUrl: String? = null,

    // USER FIELDS
    val firstName: String? = null,
    val lastName: String? = null,
    val gender: String? = null,
    val dob: String? = null,

    // COMMON LOCATION FIELDS
    val locationEnabled: Boolean = false,
    val city: String? = null,
    val addressLine: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,

    // VENDOR FIELDS
    val vendorName: String? = null,
    val vendorDescription: String? = null,
    val vendorWebsite: String? = null,
    val vendorProducts: List<String> = emptyList(), // list of product IDs (denormalized)
    val vendorLogoUrl: String? = null,
    val vendorCoverUrl: String? = null,
    val avgRating: Double? = null,
    val ratingsCount: Int = 0,
    val totalClicks: Long = 0L,
    val totalSales: Double = 0.0,
    val currency: String? = "PKR",
    val businessHours: Map<String, String>? = null, // e.g. {"mon":"09:00-17:00", ...}
    val paymentProviderId: String? = null, // token/ref to vendor payment config (non-sensitive)

    // CLINIC FIELDS
    val clinicName: String? = null,
    val clinicAddress: String? = null,
    val clinicPhone: String? = null,
    val clinicWebsite: String? = null,
    val clinicServices: List<String> = emptyList(), // e.g. ["eye exam","consultation"]
    val clinicAvgRating: Double? = null,
    val clinicRatingsCount: Int = 0,
    val appointmentsCount: Int = 0,
    val clinicType: String? = null,             // use suggested professional names
    val clinicSpecialties: List<String> = emptyList(), // optional finer-grained specialties/tags

    // META / TAGS
    val tags: List<String> = emptyList()
) : Serializable
