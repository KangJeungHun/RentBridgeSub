package com.example.rentbridgesub.ui.main

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.rentbridgesub.data.Property
import com.example.rentbridgesub.databinding.ItemPropertyBinding
import com.example.rentbridgesub.ui.chat.ChatActivity
import com.google.firebase.auth.FirebaseAuth

class PropertyAdapter(
    private val properties: List<Property>,
    private val onItemClick: (Property) -> Unit
) : RecyclerView.Adapter<PropertyAdapter.PropertyViewHolder>() {

    inner class PropertyViewHolder(val binding: ItemPropertyBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PropertyViewHolder {
        val binding = ItemPropertyBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PropertyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PropertyViewHolder, position: Int) {
        val property = properties[position]
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        holder.binding.tvTitle.text = property.title
        holder.binding.tvPrice.text = property.price
        holder.binding.tvDescription.text = property.description

        if (property.ownerId == currentUserId) {
            holder.binding.btnChat.visibility = View.GONE
        } else {
            holder.binding.btnChat.visibility = View.VISIBLE
        }

        holder.binding.btnChat.setOnClickListener {
            val intent = Intent(holder.itemView.context, ChatActivity::class.java)
            intent.putExtra("propertyId", property.id)
            intent.putExtra("receiverId", property.ownerId)
            holder.itemView.context.startActivity(intent)
        }

        holder.itemView.setOnClickListener {
            onItemClick(property)
        }
    }

    override fun getItemCount(): Int = properties.size
}
