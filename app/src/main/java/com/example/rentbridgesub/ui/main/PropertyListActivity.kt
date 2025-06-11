package com.example.rentbridgesub.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rentbridgesub.data.Property
import com.example.rentbridgesub.databinding.ActivityPropertyListBinding
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore

class PropertyListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPropertyListBinding
    private val db = FirebaseFirestore.getInstance()
    private val propertyList = mutableListOf<Property>()
    private lateinit var adapter: PropertyAdapter
    private var userLatitude: Double = 0.0
    private var userLongitude: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPropertyListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = PropertyAdapter(propertyList) { property ->
            val intent = Intent(this, PropertyDetailActivity::class.java)
            intent.putExtra("property", property)
            intent.putExtra("isRecommended", property.isRecommended)
            startActivity(intent)
        }

        binding.recyclerViewAllProperties.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewAllProperties.adapter = adapter

        getCurrentLocation()
    }

    private fun getCurrentLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    userLatitude = location.latitude
                    userLongitude = location.longitude
                    loadAllProperties()
                } else {
                    Toast.makeText(this, "위치 정보를 가져올 수 없습니다", Toast.LENGTH_SHORT).show()
                    loadAllProperties()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "위치 요청 실패", Toast.LENGTH_SHORT).show()
                loadAllProperties()
            }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val result = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, result)
        return result[0] // meter 단위
    }

    private fun loadAllProperties() {
        db.collection("Properties")
            .whereEqualTo("status", "available")
            .get()
            .addOnSuccessListener { result ->
                val tempList = mutableListOf<Pair<Property, Float>>()  // 거리와 함께 저장

                for (doc in result) {
                    val property = doc.toObject(Property::class.java)
                    val distance = calculateDistance(userLatitude, userLongitude, property.latitude, property.longitude)
                    tempList.add(Pair(property, distance))
                }

                // 거리 순 정렬 후 상위 3개 추천으로 표시
                val sorted = tempList.sortedBy { it.second }
                propertyList.clear()

                sorted.forEachIndexed { index, pair ->
                    val property = pair.first
                    if (index < 3) {
                        property.isRecommended = true
                    }
                    propertyList.add(property)
                }

                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "매물 불러오기 실패: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
