package com.example.fyp.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fyp.R
import com.example.fyp.models.RegisteredClinic
import com.google.android.material.button.MaterialButton

class RegisteredClinicAdapter(
    private val context: Context,
    private var list: List<RegisteredClinic>
) : RecyclerView.Adapter<RegisteredClinicAdapter.Holder>() {

    inner class Holder(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.tvClinicName)
        val rating: TextView = v.findViewById(R.id.tvRating)
        val address: TextView = v.findViewById(R.id.tvAddress)
        val phone: TextView = v.findViewById(R.id.tvPhone)
        val website: TextView = v.findViewById(R.id.tvWebsite)
        val services: TextView = v.findViewById(R.id.tvServices)
        val btnMaps: MaterialButton = v.findViewById(R.id.btnOpenMaps)

        // 🔥 NEW CALL ICON
        val callIcon: ImageView = v.findViewById(R.id.ivCall)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_registered_clinic, parent, false)
        return Holder(v)
    }

    override fun onBindViewHolder(h: Holder, pos: Int) {
        val c = list[pos]

        // Clinic Name + Rating
        h.name.text = c.clinicName ?: "Clinic"
        h.rating.text = "⭐ ${c.clinicAvgRating ?: 0.0} (${c.clinicRatingsCount})"

        // Address (Optional)
        if (!c.clinicAddress.isNullOrBlank()) {
            h.address.text = c.clinicAddress
            h.address.visibility = View.VISIBLE
        } else h.address.visibility = View.GONE

        // Phone + Call Icon
        if (!c.clinicPhone.isNullOrBlank()) {
            h.phone.text = c.clinicPhone
            h.phone.visibility = View.VISIBLE
            h.callIcon.visibility = View.VISIBLE

            val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${c.clinicPhone}"))

            // Click on number
            h.phone.setOnClickListener {
                context.startActivity(callIntent)
            }

            // Click on call icon
            h.callIcon.setOnClickListener {
                context.startActivity(callIntent)
            }
        } else {
            h.phone.visibility = View.GONE
            h.callIcon.visibility = View.GONE
        }

        // Website (Optional)
        if (!c.clinicWebsite.isNullOrBlank()) {
            h.website.text = c.clinicWebsite
            h.website.visibility = View.VISIBLE

            h.website.setOnClickListener {
                val browser = Intent(Intent.ACTION_VIEW, Uri.parse(c.clinicWebsite))
                context.startActivity(browser)
            }
        } else h.website.visibility = View.GONE

        // Services (Optional)
        if (!c.clinicServices.isNullOrEmpty()) {
            h.services.text = c.clinicServices.joinToString(", ")
            h.services.visibility = View.VISIBLE
        } else h.services.visibility = View.GONE

        // MAPS BUTTON — SUPPORT BOTH LOCATION STRUCTURES
        val lat = c.lat
            ?: (c.location?.get("lat") as? Double)
            ?: (c.location?.get("latitude") as? Double)

        val lon = c.lon
            ?: (c.location?.get("lon") as? Double)
            ?: (c.location?.get("lng") as? Double)

        if (lat != null && lon != null) {
            h.btnMaps.visibility = View.VISIBLE
            h.btnMaps.setOnClickListener {
                val uri = Uri.parse("geo:$lat,$lon?q=$lat,$lon(${c.clinicName})")
                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
            }
        } else {
            h.btnMaps.visibility = View.GONE
        }
    }

    override fun getItemCount() = list.size

    fun update(newList: List<RegisteredClinic>) {
        list = newList
        notifyDataSetChanged()
    }
}
