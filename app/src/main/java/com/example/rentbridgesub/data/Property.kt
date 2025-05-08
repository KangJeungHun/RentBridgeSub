package com.example.rentbridgesub.data

import java.io.Serializable

data class Property(
    val id: String = "",
    val ownerId: String = "",
    val title: String = "", // 제목
    val description: String = "", // 설명
    val address: String = "",  // 매물 주소
    val price: String = "",    // 가격
    val startDate: String = "", // 계약 시작일
    val endDate: String = "",   // 계약 종료일
    val imageUrl: String = "",   // 이미지 URL

    val latitude: Double = 0.0,   // 위도
    val longitude: Double = 0.0   // 경도
) : Serializable
