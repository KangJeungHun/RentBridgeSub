package com.example.rentbridgesub.data

import java.io.Serializable

data class Property(
    var id: String = "",
    val ownerId: String = "",
    val title: String = "",         // 제목
    val description: String = "",   // 설명
    val addressMain: String = "",   // 도로명 주소
    val addressDetail: String = "", // 상세 주소
    val price: String = "",         // 가격
    val startDate: String = "",     // 계약 시작일
    val endDate: String = "",       // 계약 종료일
    val imageUrl: String = "",      // 이미지 URL
    val latitude: Double = 0.0,     // 위도
    val longitude: Double = 0.0,     // 경도
    val landlordPhone: String = "",  //임대인 전화번호
    var isRecommended: Boolean = false, //  추천 여부 표시용 (로컬 처리용)
    val status: String = "available"
) : Serializable
