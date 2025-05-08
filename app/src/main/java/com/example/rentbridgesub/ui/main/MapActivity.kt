package com.example.rentbridgesub.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.rentbridgesub.R
import com.example.rentbridgesub.data.Property
import com.example.rentbridgesub.ui.property.PropertyBottomSheet
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private var isFirstLocation = true
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        findViewById<LinearLayout>(R.id.navMyPage).setOnClickListener {
            val intent = Intent(this, MyPageActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
            startLocationUpdates()
//            loadPropertyMarkers()

            loadMockMarkers()
        } else {
            requestLocationPermission()
        }
    }

    private fun loadMockMarkers() {
        val mockProperties = listOf(
            Property(
                id = "1",
                ownerId = "abc123",
                title = "홍대 원룸",
                description = "00빌라 1층",
                address = "서울 마포구 신촌로 100",
                price = "500/50",
                startDate = "2025-07-01",
                endDate = "2025-08-31",
                imageUrl = "",
                latitude = 37.559819,
                longitude = 126.942308
            ),
            Property(
                id = "2",
                ownerId = "def456",
                address = "서울 마포구 양화로 120",
                title = "홍대 부근 원룸",
                description = "",
                price = "1000/70",
                startDate = "2025-07-01",
                endDate = "2025-08-01",
                imageUrl = "",
                latitude = 37.556081,
                longitude = 126.922425
            )
        )

        for (property in mockProperties) {
            val position = LatLng(property.latitude, property.longitude)
            mMap.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(property.title)
                    .snippet("주소: ${property.address}\n" +
                            "가격: ${property.price}\n" +
                            "기간:${property.startDate} ~ ${property.endDate}")
            )
        }

        mMap.setOnMarkerClickListener { marker ->
            val matchedProperty = mockProperties.find {
                it.latitude == marker.position.latitude && it.longitude == marker.position.longitude
            }
            if (matchedProperty != null) {
                val bottomSheet = PropertyBottomSheet(matchedProperty)
                bottomSheet.show(supportFragmentManager, bottomSheet.tag)
            }
            true
        }

    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startLocationUpdates()
//            loadPropertyMarkers()
        } else {
            Toast.makeText(this, "위치 권한이 필요합니다", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 5000L
        ).apply {
            setMinUpdateIntervalMillis(2000L)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                val currentLatLng = LatLng(location.latitude, location.longitude)

                // ✅ 처음 위치 수신 시, 지도 중심 이동
                if (isFirstLocation) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f))
                    isFirstLocation = false
                }

                // 위치 마커 추가하거나 저장할 경우는 여기
            }
        }

        if (
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                1001
            )
            return
        }
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun loadPropertyMarkers() {
        db.collection("Properties")
            .get()
            .addOnSuccessListener { result ->
                for (doc in result) {
                    val property = doc.toObject(Property::class.java)
                    if (property.latitude != 0.0 && property.longitude != 0.0) {
                        val position = LatLng(property.latitude, property.longitude)
                        mMap.addMarker(
                            MarkerOptions()
                                .position(position)
                                .title(property.address)
                                .snippet("보증금: ${property.price}")
                        )
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "매물 정보를 불러올 수 없습니다", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onStop() {
        super.onStop()
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    override fun onBackPressed() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("앱 종료")
            .setMessage("앱을 종료하시겠습니까?")
            .setPositiveButton("예") { _, _ ->
                finishAffinity() // 전체 액티비티 종료
            }
            .setNegativeButton("아니요", null)
            .show()
    }

}
