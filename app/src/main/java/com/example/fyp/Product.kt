package com.example.fyp

import java.io.Serializable

data class Product(
    val id: String = "",                 // Firestore doc ID
    val vendorId: String = "",           // Owner (uid)

    val title: String = "",              // Required
    val description: String = "",        // Max 30 words (validated in UI)
    val category: String = "other",      // "eye" | "skin" | "mood" | "other"
    val tags: List<String> = emptyList(),// Optional search filters

    val link: String? = null,            // Optional product URL
    val images: List<String> = emptyList(), // Storage URLs (1–3 images)

    val clicks: Long = 0L,               // Increment on every fetch/open
    val views: Long = 0L,                // Optional page views metric

    val price: Double? = null,           // Optional
    val currency: String? = "PKR",       // Default PKR

    val isActive: Boolean = true,        // Soft delete
    val isApproved: Boolean = false,     // Admin approval flag
    val visibility: String? = "public",  // "public" | "private" | "unlisted"

    val createdAt: Long? = null,         // serverTimestamp
    val updatedAt: Long? = null          // serverTimestamp
) : Serializable
