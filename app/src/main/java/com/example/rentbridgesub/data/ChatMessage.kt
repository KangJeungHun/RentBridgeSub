package com.example.rentbridgesub.data

data class ChatMessage(
    val senderId: String = "",
    val receiverId: String = "",
    val message: String = "",     // 텍스트 메시지
    val imageUrl: String = "",    // 이미지 메시지 URL
    val timestamp: Long = 0L
)
