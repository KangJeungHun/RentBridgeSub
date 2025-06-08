package com.example.rentbridgesub.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.os.Looper
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.rentbridgesub.R
import com.example.rentbridgesub.data.Property
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

                    // 서울 같은 넓은 지역을 보기 좋게 보여주기 위해 bounds 적용
                    val bounds = LatLngBounds(
                        LatLng(lat - 0.01, lng - 0.01),  // 남서
                        LatLng(lat + 0.01, lng + 0.01)   // 북동
                    )

                    val zoomLevel = when (category) {
                        "SW8", "BK9", "MT1", "OL7" -> 17f  // 지하철, 은행, 마트 등 -> 더 확대
                        else -> 14f  // 일반 지역
                    }

                    onSuccess(bounds, zoomLevel)
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
