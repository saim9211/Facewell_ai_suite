package com.example.fyp.models

import java.io.Serializable

data class RegisteredClinic(
    val clinicName: String? = null,
    val clinicAddress: String? = null,
    val clinicPhone: String? = null,
    val clinicWebsite: String? = null,

    val clinicServices: List<String> = emptyList(),

    val clinicAvgRating: Double? = 0.0,
    val clinicRatingsCount: Int = 0,

    val clinicType: String? = null,
    val clinicSpecialties: List<String> = emptyList(),

    // Location (both schema formats supported)
    val lat: Double? = null,
    val lon: Double? = null,
    val location: Map<String, Any>? = null
) : Serializable
