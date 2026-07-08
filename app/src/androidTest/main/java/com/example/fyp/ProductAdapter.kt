package com.example.fyp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ProductAdapter(
    private val ctx: Context,
    private var list: List<Product>,
    private val onClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.Holder>() {

    inner class Holder(v: View) : RecyclerView.ViewHolder(v) {
        val img: ImageView = v.findViewById(R.id.imgProduct)
        val title: TextView = v.findViewById(R.id.tvTitle)
        val desc: TextView = v.findViewById(R.id.tvDesc)
        val rating: TextView = v.findViewById(R.id.tvRating)
        val ratingCount: TextView = v.findViewById(R.id.tvRatingCount)
        val category: TextView = v.findViewById(R.id.tvCategory)
        val price: TextView = v.findViewById(R.id.tvPrice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_eye_report_product, parent, false)
        return Holder(v)
    }

    override fun onBindViewHolder(h: Holder, pos: Int) {
        val p = list[pos]

        h.title.text = p.title
        h.desc.text = p.description.take(60) + if (p.description.length > 60) "..." else ""

        h.rating.text = String.format("%.1f", p.avgRating)
        h.ratingCount.text = "(${p.ratingsCount})"

        h.category.text = p.category

        h.price.text =
            if (p.price != null) "${p.currency ?: "PKR"} ${p.price}" else "Price N/A"

        Glide.with(ctx)
            .load(p.images.firstOrNull())
            .placeholder(R.drawable.img_product_placeholder)
            .into(h.img)

        h.itemView.setOnClickListener { onClick(p) }
    }

    override fun getItemCount() = list.size

    fun update(newList: List<Product>) {
        list = newList
        notifyDataSetChanged()
    }
}
