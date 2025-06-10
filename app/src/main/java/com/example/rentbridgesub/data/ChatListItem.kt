package com.example.rentbridgesub.data

data class ChatListItem(
    val userId: String,
    val userName: String,
    var lastMessage: String = "",
    var fileName: String = "",
    var imageUrl: String = "",
    var timestamp: Long = 0L,
    var read: Boolean = true,
    var unreadCount: Int = 0
)
