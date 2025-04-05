package com.example.rentbridgesub.ui.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.rentbridgesub.databinding.ItemChatListBinding

class ChatListAdapter(
    private val users: List<Pair<String, String>>, // (userId, userName)
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<ChatListAdapter.ChatListViewHolder>() {

    inner class ChatListViewHolder(val binding: ItemChatListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                onItemClick(users[adapterPosition].first)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatListViewHolder {
        val binding = ItemChatListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatListViewHolder, position: Int) {
        holder.binding.tvUserId.text = users[position].second // 이름 표시
    }

    override fun getItemCount(): Int = users.size
}
