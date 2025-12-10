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

    // USER FIELDS
    val firstName: String? = null,
    val lastName: String? = null,
    val gender: String? = null,
    val dob: String? = null,
    val avatarUrl: String? = null,     // Only user uses avatar

    // COMMON LOCATION FIELDS
    val locationEnabled: Boolean = false,
    val city: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,

    // VENDOR FIELDS
    val vendorName: String? = null,
    val vendorDescription: String? = null,
    val vendorWebsite: String? = null,

    // CLINIC FIELDS
    val clinicName: String? = null,
    val clinicAddress: String? = null,
    val clinicPhone: String? = null,
    val clinicWebsite: String? = null
): Serializable
