package com.example.rentbridgesub.ui.chat

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.rentbridgesub.R
import com.example.rentbridgesub.data.ChatMessage
import com.example.rentbridgesub.databinding.ItemChatMessageBinding

class ChatAdapter(
    private val messages: List<ChatMessage>,
    private val currentUserId: String
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    inner class ChatViewHolder(val binding: ItemChatMessageBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messages[position]

        holder.binding.tvMessage.text = message.message

        if (message.senderId == currentUserId) {
            // 내가 보낸 메시지
            holder.binding.tvMessage.textAlignment = View.TEXT_ALIGNMENT_VIEW_END
            holder.binding.tvMessage.background = holder.itemView.context.getDrawable(R.drawable.bg_message_sent)
            holder.binding.tvMessage.setTextColor(Color.WHITE)  // ✨ 내 메시지는 흰색

            val params = holder.binding.tvMessage.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            params.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
            params.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
            holder.binding.tvMessage.layoutParams = params

        } else {
            // 상대방이 보낸 메시지
            holder.binding.tvMessage.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            holder.binding.tvMessage.background = holder.itemView.context.getDrawable(R.drawable.bg_message_received)
            holder.binding.tvMessage.setTextColor(Color.BLACK)  // ✨ 상대 메시지는 검정색

            val params = holder.binding.tvMessage.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            params.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
            params.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
            holder.binding.tvMessage.layoutParams = params
        }
    }

    override fun getItemCount(): Int = messages.size
}
