package com.example.rentbridgesub.data

import java.io.Serializable

data class Property(
    val propertyId: String = "",
    val ownerId: String = "",
    val address: String = "",
    val price: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val imageUrl: String = ""
) : Serializable
