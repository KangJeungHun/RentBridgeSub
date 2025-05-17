package com.example.rentbridgesub.ui.manageproperty

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.rentbridgesub.data.Property
import com.example.rentbridgesub.databinding.ItemPropertyManageBinding

class ManagePropertyAdapter(
    private val items: List<Property>,
    private val onClick: (Property) -> Unit
) : RecyclerView.Adapter<ManagePropertyAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemPropertyManageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(property: Property) {
            binding.tvTitle.text = property.title
            binding.tvPrice.text = property.price
            binding.root.setOnClickListener { onClick(property) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPropertyManageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
