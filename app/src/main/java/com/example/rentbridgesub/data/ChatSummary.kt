package com.example.rentbridgesub.data

data class ChatSummary(
    val chatRoomId: String,
    val otherUserId: String,
    var otherUserName: String = "",
    var avatarUrl: String = "",
    var lastMessage: String = "",
    var timestamp: Long = 0L
)
