package com.example.rentbridgesub.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.rentbridgesub.data.Property
import com.example.rentbridgesub.databinding.ItemPropertyBinding
import com.squareup.picasso.Picasso

class PropertyAdapter(
    private val items: List<Property>,
    private val onItemClick: (Property) -> Unit
) : RecyclerView.Adapter<PropertyAdapter.PropertyViewHolder>() {

    inner class PropertyViewHolder(val binding: ItemPropertyBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                onItemClick(items[adapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PropertyViewHolder {
        val binding = ItemPropertyBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PropertyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PropertyViewHolder, position: Int) {
        val item = items[position]
        holder.binding.tvAddress.text = item.address
        holder.binding.tvPeriod.text = "${item.startDate} ~ ${item.endDate}"
        holder.binding.tvPrice.text = "${item.price}원"
        Picasso.get().load(item.imageUrl).into(holder.binding.ivThumbnail)
    }

    override fun getItemCount(): Int = items.size
}
