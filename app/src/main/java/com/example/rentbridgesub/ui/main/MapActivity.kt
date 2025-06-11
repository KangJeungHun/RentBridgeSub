package com.example.rentbridgesub.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.rentbridgesub.R
import com.example.rentbridgesub.data.Property
import com.example.rentbridgesub.ui.chat.ChatListActivity
//import com.example.rentbridgesub.ui.property.PropertyBottomSheet
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
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale

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

        // 🔍 검색 버튼 처리
        val etSearch = findViewById<EditText>(R.id.etSearch)

        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val keyword = etSearch.text.toString()
                if (keyword.isNotEmpty()) {
                    searchLocation(keyword) { bounds, zoomLevel ->
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(bounds.center, zoomLevel))
                    }
                }
                true
            } else {
                false
            }
        }

        val btnSearch = findViewById<Button>(R.id.btnSearch)
        btnSearch.setOnClickListener {
            val keyword = etSearch.text.toString()
            if (keyword.isNotEmpty()) {
                searchLocation(keyword) { bounds, zoomLevel ->
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(bounds.center, zoomLevel))
                }
            } else {
                Toast.makeText(this, "검색어를 입력하세요", Toast.LENGTH_SHORT).show()
            }
        }

        val btnViewAll = findViewById<TextView>(R.id.btnViewAllProperties)
        btnViewAll.visibility = View.GONE
        btnViewAll.setOnClickListener {
            startActivity(Intent(this, PropertyListActivity::class.java))
        }

        // 사용자 타입에 따라 버튼 노출 제어
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("Users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                when (doc.getString("userType")) {
                    "sublessee" -> {
                        // 전차인일 때만 전체 매물 보기
                        btnViewAll.visibility = View.VISIBLE
                    }
                    else -> {
                        // 전대인은 숨김
                        btnViewAll.visibility = View.GONE
                    }
                }
            }

        val homeBtn = findViewById<LinearLayout>(R.id.navHome)
        FirebaseFirestore.getInstance().collection("Users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                when (doc.getString("userType")) {
                    "sublessee" -> {
                        // 전차인일 때 홈 버튼 숨기기
                        homeBtn.visibility = View.GONE
                    }
                    else -> {
                        // 전대인이나 기타일 땐 홈 버튼 보이기
                        homeBtn.visibility = View.VISIBLE
                    }
                }
            }

        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        findViewById<LinearLayout>(R.id.navChat).setOnClickListener {
            startActivity(Intent(this, ChatListActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.navMyPage).setOnClickListener {
            val intent = Intent(this, MyPageActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true

            // 1) 마지막 알려진 위치 가져와서 카메라 이동
            fusedLocationClient.lastLocation
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        val myLatLng = LatLng(loc.latitude, loc.longitude)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, 16f))
                    }
                    // 2) 그 다음부터 위치 업데이트 시작
                    startLocationUpdates()
                }
                .addOnFailureListener {
                    // 실패해도 위치 업데이트는 시도
                    startLocationUpdates()
                }

            loadPropertyMarkersFromFirestore()
        } else {
            requestLocationPermission()
        }
    }

    private fun searchLocation(keyword: String, onSuccess: (LatLngBounds, Float) -> Unit) {
        val url = "https://dapi.kakao.com/v2/local/search/keyword.json"
        val queue = Volley.newRequestQueue(this)

        val request = object : StringRequest(
            Request.Method.GET,
            "$url?query=${URLEncoder.encode(keyword, "UTF-8")}",
            { response ->
                val json = JSONObject(response)
                val documents = json.getJSONArray("documents")

                if (documents.length() > 0) {
                    val first = documents.getJSONObject(0)
                    val lat = first.getString("y").toDouble()
                    val lng = first.getString("x").toDouble()

                    val category = first.optString("category_group_code", "")

                    // 카테고리별로 오프셋(도)와 줌 레벨 결정
                    val (dLat, dLng, zoom) = when {
                        category.startsWith("AD") -> Triple(0.05, 0.05, 11f)  // 행정구역
                        category == "SW8"        -> Triple(0.02, 0.02, 15f)  // 지하철역
                        category == "BK9"        -> Triple(0.02, 0.02, 15f)  // 은행 등 공공기관
                        category == "MT1"        -> Triple(0.01, 0.01, 16f)  // 마트/슈퍼
                        category == "PO3"        -> Triple(0.005, 0.005, 17f) // 건물(오피스텔 등)
                        else                      -> Triple(0.01, 0.01, 14f)  // 일반 장소
                    }

                    // LatLngBounds 생성
                    val bounds = LatLngBounds(
                        LatLng(lat - dLat, lng - dLng),
                        LatLng(lat + dLat, lng + dLng)
                    )

                    onSuccess(bounds, zoom)
                } else {
                    Toast.makeText(this, "결과가 없습니다", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "API 오류: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf("Authorization" to "KakaoAK ee2b6e2d5141747d912a9540432a7a61")
            }
        }

        queue.add(request)
    }


    private fun loadPropertyMarkersFromFirestore() {
        val db = FirebaseFirestore.getInstance()
        val propertyList = mutableListOf<Property>()
        db.collection("Properties")
            .whereEqualTo("status", "available")
            .get()
            .addOnSuccessListener { documents ->
                for (doc in documents) {
                    val property = doc.toObject(Property::class.java)
                    val position = LatLng(property.latitude, property.longitude)
                    mMap.addMarker(
                        MarkerOptions()
                            .position(position)
                            .title(property.title)
                            .snippet("📍 ${property.addressMain + ' ' + property.addressDetail}\n💰 ${property.price}\n📅 ${property.startDate} ~ ${property.endDate}")
                    )

                    propertyList.add(property)
                }
                setMarkerClickListener(propertyList)
            }
            .addOnFailureListener {
                Toast.makeText(this, "매물 로딩 실패: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setMarkerClickListener(propertyList: List<Property>) {
        mMap.setOnMarkerClickListener { marker ->
            val matched = propertyList.find {
                it.latitude == marker.position.latitude && it.longitude == marker.position.longitude
            }
            matched?.let {
                val bottomSheet = PropertyBottomSheet(it)
                bottomSheet.show(supportFragmentManager, bottomSheet.tag)
            }
            true
        }
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
