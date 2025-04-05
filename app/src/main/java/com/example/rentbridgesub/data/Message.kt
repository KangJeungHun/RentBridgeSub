package com.example.rentbridgesub.data

data class Message(
    val senderId: String = "",
    val receiverId: String = "",
    val content: String = "",
    val timestamp: Long = 0L
)
