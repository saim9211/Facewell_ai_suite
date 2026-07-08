package com.example.fyp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class SearchProductAdapter(
    private var list: List<Product>,
    private val onProductClick: (Product) -> Unit
) : RecyclerView.Adapter<SearchProductAdapter.Holder>() {

    inner class Holder(v: View) : RecyclerView.ViewHolder(v) {
        val img: ImageView = v.findViewById(R.id.imgProduct)
        val title: TextView = v.findViewById(R.id.tvTitle)
        val desc: TextView = v.findViewById(R.id.tvDesc)
        val cat: TextView = v.findViewById(R.id.tvCategory)
        val price: TextView = v.findViewById(R.id.tvPrice)
        val rating: TextView = v.findViewById(R.id.tvRating)
        val ratingCount: TextView = v.findViewById(R.id.tvRatingCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_product, parent, false)
        return Holder(v)
    }

    override fun onBindViewHolder(h: Holder, position: Int) {
        val p = list[position]

        h.title.text = p.title.take(25) + if (p.title.length > 25) "…" else ""
        h.desc.text = p.description.take(25) + if (p.description.length > 25) "…" else ""

        h.cat.text = p.category
        h.price.text = "PKR ${p.price ?: 0}"

        h.rating.text = String.format("%.1f", p.avgRating)
        h.ratingCount.text = "(${p.ratingsCount})"

        val imageUrl = p.images.firstOrNull()

        Glide.with(h.itemView.context)
            .load(imageUrl)
            .placeholder(R.drawable.img_product_placeholder)
            .into(h.img)

        h.itemView.setOnClickListener { onProductClick(p) }
    }

    override fun getItemCount(): Int = list.size

    fun update(newList: List<Product>) {
        list = newList
        notifyDataSetChanged()
    }
}
