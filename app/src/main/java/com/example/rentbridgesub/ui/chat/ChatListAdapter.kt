package com.example.rentbridgesub.ui.chat

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.rentbridgesub.data.ChatListItem
import com.example.rentbridgesub.databinding.ItemChatListBinding

class ChatListAdapter(
    private val chatRooms: List<ChatListItem>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<ChatListAdapter.ChatListViewHolder>() {

    inner class ChatListViewHolder(val binding: ItemChatListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                onItemClick(chatRooms[adapterPosition].userId) // 👈 여기
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatListViewHolder {
        val binding = ItemChatListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatListViewHolder, position: Int) {
        val item = chatRooms[position]
        holder.binding.tvUserId.text = item.userName

        // 마지막 메시지 텍스트 설정
        val displayMessage = when {
            item.lastMessage.isNotBlank() -> item.lastMessage
            item.fileName.isNotBlank()    -> "📎 파일: ${item.fileName}"
            item.imageUrl.isNotBlank()    -> "📷 사진을 보냈습니다."
            else                          -> "(알 수 없음)"
        }
        holder.binding.tvLastMessage.text = displayMessage

        // 시간 포맷
        holder.binding.tvTimestamp.text = formatTimestampHumanFriendly(item.timestamp)

        Log.d("UnreadCheck", "User: ${item.userName}, UnreadCount: ${item.unreadCount}")


        if (item.unreadCount > 0) {
            holder.binding.tvUnreadBadge.text = item.unreadCount.toString()
            holder.binding.tvUnreadBadge.visibility = View.VISIBLE
        } else {
            holder.binding.tvUnreadBadge.visibility = View.GONE
        }
    }

    private fun formatTimestampHumanFriendly(timestamp: Long): String {
        val now = System.currentTimeMillis()

        val calNow = java.util.Calendar.getInstance()
        calNow.timeInMillis = now

        val calMsg = java.util.Calendar.getInstance()
        calMsg.timeInMillis = timestamp

        val isSameDay = calNow.get(java.util.Calendar.YEAR) == calMsg.get(java.util.Calendar.YEAR)
                && calNow.get(java.util.Calendar.DAY_OF_YEAR) == calMsg.get(java.util.Calendar.DAY_OF_YEAR)

        val isYesterday = calNow.get(java.util.Calendar.YEAR) == calMsg.get(java.util.Calendar.YEAR)
                && calNow.get(java.util.Calendar.DAY_OF_YEAR) - calMsg.get(java.util.Calendar.DAY_OF_YEAR) == 1

        return when {
            isSameDay -> android.text.format.DateFormat.format("aa h:mm", timestamp).toString()
            isYesterday -> "어제"
            calNow.get(java.util.Calendar.YEAR) == calMsg.get(java.util.Calendar.YEAR) ->
                "${calMsg.get(java.util.Calendar.MONTH) + 1}/${calMsg.get(java.util.Calendar.DAY_OF_MONTH)}"
            else -> "${calMsg.get(java.util.Calendar.YEAR)}/${calMsg.get(java.util.Calendar.MONTH) + 1}/${calMsg.get(java.util.Calendar.DAY_OF_MONTH)}"
        }
    }

    override fun getItemCount(): Int = chatRooms.size
}
