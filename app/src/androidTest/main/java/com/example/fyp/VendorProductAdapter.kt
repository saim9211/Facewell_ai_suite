package com.example.fyp

import android.app.AlertDialog
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class VendorProductAdapter(private val items: MutableList<Product>) :
    RecyclerView.Adapter<VendorProductAdapter.VH>() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val img = itemView.findViewById<ImageView>(R.id.imgProduct)
        val title = itemView.findViewById<TextView>(R.id.tvTitle)
        val desc = itemView.findViewById<TextView>(R.id.tvDesc)
        val category = itemView.findViewById<TextView>(R.id.tvCategory)
        val price = itemView.findViewById<TextView>(R.id.tvPrice)

        // ⭐ RATING
        val rating = itemView.findViewById<TextView>(R.id.tvRating)
        val ratingCount = itemView.findViewById<TextView>(R.id.tvRatingCount)

        val btnEdit = itemView.findViewById<Button>(R.id.btnEditProduct)
        val btnDelete = itemView.findViewById<ImageView>(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_vendor_product_card, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = items[position]

        holder.title.text =
            if (p.title.length > 25) p.title.substring(0, 25) + "..." else p.title

        holder.desc.text =
            if (p.description.length > 25) p.description.substring(0, 25) + "..." else p.description

        holder.category.text = p.category
        holder.price.text = "PKR ${p.price ?: 0}"

        // ⭐ RATING DISPLAY
        holder.rating.text = String.format("%.1f", p.avgRating)
        holder.ratingCount.text = "(${p.ratingsCount})"

        holder.img.setImageResource(R.drawable.img_product_placeholder)

        holder.btnEdit.setOnClickListener {
            val intent = Intent(holder.itemView.context, EditProductActivity::class.java)
            intent.putExtra("product", p)
            holder.itemView.context.startActivity(intent)
        }

        holder.btnDelete.setOnClickListener {
            showDeleteDialog(holder, p, position)
        }
    }

    override fun getItemCount(): Int = items.size

    private fun showDeleteDialog(holder: VH, product: Product, position: Int) {
        val ctx = holder.itemView.context
        val view = LayoutInflater.from(ctx).inflate(R.layout.dialog_delete_product, null)

        val dialog = AlertDialog.Builder(ctx)
            .setView(view)
            .setCancelable(true)
            .create()

        val btnDelete = view.findViewById<Button>(R.id.btnDeleteConfirm)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnDelete.setOnClickListener {
            dialog.dismiss()
            deleteProduct(product, position)
        }

        dialog.show()
    }

    private fun deleteProduct(product: Product, position: Int) {
        val uid = auth.currentUser?.uid ?: return

        // 1) Delete product document
        db.collection("products").document(product.id)
            .delete()
            .addOnSuccessListener {
                // 2) Remove from vendor's product array
                db.collection("users").document(uid)
                    .update("products", com.google.firebase.firestore.FieldValue.arrayRemove(product.id))

                // 3) Remove from list + notify
                items.removeAt(position)
                notifyItemRemoved(position)
            }
            .addOnFailureListener { it.printStackTrace() }
    }

    fun replaceAll(newItems: MutableList<Product>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
