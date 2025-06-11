package com.example.rentbridgesub.ui.chat

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.rentbridgesub.R
import com.example.rentbridgesub.data.ChatSummary
import com.example.rentbridgesub.databinding.ItemRecentChatBinding
import java.util.Calendar

class ChatSummaryAdapter(
    private val items: List<ChatSummary>,
    private val onClick: (ChatSummary) -> Unit
) : RecyclerView.Adapter<ChatSummaryAdapter.VH>() {

    inner class VH(val binding: ItemRecentChatBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemRecentChatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        with(holder.binding) {
            // 프로필 이미지
            if (item.avatarUrl.isNotEmpty()) {
                Glide.with(root).load(item.avatarUrl).circleCrop().into(ivAvatar)
            } else {
                ivAvatar.setImageResource(R.drawable.ic_placeholder_avatar)
            }
            tvUserName.text = item.otherUserName
            tvLastMessage.text = item.lastMessage
            // 시간 포맷 함수
            tvTimestamp.text = formatTime(item.timestamp)

            root.setOnClickListener { onClick(item) }
        }
    }

    override fun getItemCount() = items.size

    private fun formatTime(ts: Long): String {
        // 방금, 오늘, 어제, 날짜 ...
        val now = System.currentTimeMillis()
        val calNow = Calendar.getInstance().apply { timeInMillis = now }
        val calMsg = Calendar.getInstance().apply { timeInMillis = ts }
        return when {
            calNow.get(Calendar.YEAR)==calMsg.get(Calendar.YEAR)
                    && calNow.get(Calendar.DAY_OF_YEAR)==calMsg.get(Calendar.DAY_OF_YEAR) ->
                DateFormat.format("aa h:mm", ts).toString()
            calNow.get(Calendar.YEAR)==calMsg.get(Calendar.YEAR)
                    && calNow.get(Calendar.DAY_OF_YEAR)-calMsg.get(Calendar.DAY_OF_YEAR)==1 ->
                "어제"
            calNow.get(Calendar.YEAR)==calMsg.get(Calendar.YEAR) ->
                "${calMsg.get(Calendar.MONTH)+1}/${calMsg.get(Calendar.DAY_OF_MONTH)}"
            else ->
                "${calMsg.get(Calendar.YEAR)}/${calMsg.get(Calendar.MONTH)+1}/${calMsg.get(Calendar.DAY_OF_MONTH)}"
        }
    }
}
