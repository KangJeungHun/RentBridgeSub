package com.example.rentbridgesub.data

import java.io.Serializable

data class Property(
    val id: String = "",
    val ownerId: String = "",
    val address: String = "",  // 매물 주소
    val price: String = "",    // 가격
    val startDate: String = "", // 계약 시작일
    val endDate: String = "",   // 계약 종료일
    val imageUrl: String = ""   // 이미지 URL
) : Serializable
