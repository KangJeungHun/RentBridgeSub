package com.example.rentbridgesub.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.rentbridgesub.R
import com.example.rentbridgesub.data.ChatMessage
import com.example.rentbridgesub.databinding.ItemChatMessageBinding

class ChatAdapter(
    private val messageList: List<ChatMessage>,
    private val currentUserId: String
) : RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

    inner class MessageViewHolder(val binding: ItemChatMessageBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemChatMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messageList[position]

        holder.binding.tvMessage.text = message.message   // 🔥 여기 수정 ("content" ❌ "message" ✅)

        val params = holder.binding.tvMessage.layoutParams as FrameLayout.LayoutParams
        if (message.senderId == currentUserId) {
            params.gravity = android.view.Gravity.END
            holder.binding.tvMessage.setBackgroundResource(R.drawable.bg_message_sent)
            holder.binding.tvMessage.setTextColor(android.graphics.Color.WHITE)
        } else {
            params.gravity = android.view.Gravity.START
            holder.binding.tvMessage.setBackgroundResource(R.drawable.bg_message_received)
            holder.binding.tvMessage.setTextColor(android.graphics.Color.BLACK)
        }
        holder.binding.tvMessage.layoutParams = params
    }

    override fun getItemCount(): Int = messageList.size
}
