package com.example.rentbridgesub.data

data class ChatMessage(
    val senderId: String = "",
    val receiverId: String = "",
    val message: String = "",
    val imageUrl: String = "",
    val timestamp: Long = 0,
    val type: String = "text"  // "text", "image", "contract"
)

