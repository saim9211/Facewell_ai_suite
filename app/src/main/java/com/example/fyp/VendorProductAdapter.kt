package com.example.fyp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class VendorProductAdapter(private val items: MutableList<Product>) : RecyclerView.Adapter<VendorProductAdapter.VH>() {

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val img = itemView.findViewById<ImageView>(R.id.imgProduct)
        val title = itemView.findViewById<TextView>(R.id.tvTitle)
        val desc = itemView.findViewById<TextView>(R.id.tvDesc)
        val category = itemView.findViewById<TextView>(R.id.tvCategory)
        val clicks = itemView.findViewById<TextView>(R.id.tvClicks)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_product_card, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = items[position]
        holder.title.text = p.title
        holder.desc.text = p.description
        holder.category.text = p.category
        holder.clicks.text = "Clicks: ${p.clicks}"

        // placeholder image for now
        holder.img.setImageResource(R.drawable.img_product_placeholder)

        // TODO: handle card clicks (edit / preview) later
        holder.itemView.setOnClickListener {
            // future: open edit or preview
        }
    }

    override fun getItemCount(): Int = items.size

    fun replaceAll(newItems: MutableList<Product>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
