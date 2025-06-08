package com.example.rentbridgesub.ui.chat

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.rentbridgesub.R
import com.example.rentbridgesub.data.ChatMessage
import com.example.rentbridgesub.databinding.ItemChatMessageBinding
import com.squareup.picasso.Picasso

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
        val isSender = message.senderId == currentUserId

        // 초기화: 모든 뷰 감추기
        holder.binding.tvMessage.visibility = View.GONE
        holder.binding.ivImage.visibility = View.GONE

        // 1) 파일 메시지 처리
        if (message.fileUrl.isNotBlank()) {
            holder.binding.tvMessage.visibility = View.VISIBLE
            val fileName = Uri.parse(message.fileUrl).lastPathSegment ?: "파일"
            holder.binding.tvMessage.text = "📄 ${message.fileName}"
            holder.binding.tvMessage.setOnClickListener {
                val uri = Uri.parse(message.fileUrl)
                val ext = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
                val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mime)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                try {
                    holder.itemView.context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(holder.itemView.context, "파일을 열 수 있는 앱이 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        // 2) 이미지 메시지 처리
        else if (message.imageUrl.isNotBlank()) {
            holder.binding.ivImage.visibility = View.VISIBLE
            Picasso.get().load(message.imageUrl).into(holder.binding.ivImage)
            holder.binding.ivImage.setOnClickListener {
                val intent = Intent(holder.itemView.context, FullImageActivity::class.java).apply {
                    putExtra("imageUrl", message.imageUrl)
                }
                holder.itemView.context.startActivity(intent)
            }
        }
        // 3) 일반 텍스트 메시지 처리
        else if (message.message.isNotBlank()) {
            holder.binding.tvMessage.visibility = View.VISIBLE
            holder.binding.tvMessage.text = message.message
            holder.binding.tvMessage.setOnClickListener(null)
        }

        // 정렬 설정
        val containerParams = holder.binding.messageContainer.layoutParams as FrameLayout.LayoutParams
        containerParams.gravity = if (isSender) Gravity.END else Gravity.START
        holder.binding.messageContainer.layoutParams = containerParams

        // 배경 및 글자 색 설정
        if (isSender) {
            holder.binding.tvMessage.setBackgroundResource(R.drawable.bg_message_sent)
            holder.binding.tvMessage.setTextColor(ContextCompat.getColor(holder.itemView.context, android.R.color.white))
        } else {
            holder.binding.tvMessage.setBackgroundResource(R.drawable.bg_message_received)
            holder.binding.tvMessage.setTextColor(ContextCompat.getColor(holder.itemView.context, android.R.color.black))
        }

//        holder.binding.tvMessage.setOnClickListener {
//            val url = message.imageUrl
//            val extension = MimeTypeMap.getFileExtensionFromUrl(url)
//
//            Log.d("Url", "PDFurl: $url")
//
//            if (url.isNotEmpty()) {
//                if (extension == "pdf") {
//                    val intent = Intent(Intent.ACTION_VIEW).apply {
//                        setDataAndType(Uri.parse(url), "application/pdf")
//                        flags = Intent.FLAG_ACTIVITY_NO_HISTORY or
//                                Intent.FLAG_GRANT_READ_URI_PERMISSION or
//                                Intent.FLAG_ACTIVITY_NEW_TASK
//                    }
//                    try {
//                        holder.itemView.context.startActivity(intent)
//                    } catch (e: Exception) {
//                        Toast.makeText(holder.itemView.context, "PDF를 열 수 있는 앱이 없습니다.", Toast.LENGTH_SHORT).show()
//                    }
//                } else {
//                    Toast.makeText(holder.itemView.context, "이미지 클릭 처리 미구현", Toast.LENGTH_SHORT).show()
//                }
//            }
//        }
//
//        holder.binding.ivImage.setOnClickListener {
//            val intent = Intent(holder.itemView.context, FullImageActivity::class.java)
//            intent.putExtra("imageUrl", message.imageUrl)
//            holder.itemView.context.startActivity(intent)
//        }
    }
    override fun getItemCount(): Int = messageList.size
}
