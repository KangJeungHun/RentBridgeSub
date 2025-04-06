package com.example.rentbridgesub.ui.chat

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.rentbridgesub.databinding.ItemChatListBinding

class ChatListAdapter(
    private val chatRooms: List<Pair<String, String>>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<ChatListAdapter.ChatListViewHolder>() {

    inner class ChatListViewHolder(val binding: ItemChatListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                onItemClick(chatRooms[adapterPosition].first) // 👈 여기
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatListViewHolder {
        val binding = ItemChatListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatListViewHolder, position: Int) {
        holder.binding.tvUserId.text = chatRooms[position].second // 👈 상대방 이름 보여주기
    }

    override fun getItemCount(): Int = chatRooms.size
}
