package com.example.fyp

import java.io.Serializable

data class Product(
    val id: String = "",
    val vendorId: String = "",

    val title: String = "",
    val description: String = "",
    val category: String = "other",   // eye | skin | mood | other

    val recommendedFor: List<String> = emptyList(),
    val tags: List<String> = emptyList(),

    val link: String? = null,
    val images: List<String> = emptyList(),

    val clicks: Long = 0L,
    val views: Long = 0L,

    val price: Double? = null,
    val currency: String? = "PKR",

    // ⭐ RATING SYSTEM
    val avgRating: Double = 0.0,      // average of all user ratings
    val ratingsCount: Int = 0,        // number of users who rated

    val isActive: Boolean = true,
    val isApproved: Boolean = false,
    val visibility: String? = "public",

    val createdAt: Long? = null,
    val updatedAt: Long? = null
) : Serializable
