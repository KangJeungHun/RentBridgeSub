package com.example.rentbridgesub.data

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val userType: String = "", // sublessor or sublessee
    val favorites: List<String> = emptyList()
)