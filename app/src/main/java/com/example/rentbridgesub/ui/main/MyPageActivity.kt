package com.example.rentbridgesub.ui.main

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.telephony.SmsManager
import android.view.View
import android.webkit.WebView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rentbridgesub.R
import com.example.rentbridgesub.data.Property
import com.example.rentbridgesub.databinding.ActivityMyPageBinding
import com.example.rentbridgesub.ui.auth.LoginActivity
import com.example.rentbridgesub.ui.chat.ChatListActivity
import com.example.rentbridgesub.ui.editprofile.EditProfileActivity
import com.example.rentbridgesub.ui.manageproperty.ManagePropertiesActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.net.URLEncoder

class MyPageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyPageBinding
    private lateinit var storage: FirebaseStorage
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val propertyList = mutableListOf<Property>()
    private lateinit var adapter: PropertyAdapter
    private val SMS_PERMISSION_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadUserName()

        adapter = PropertyAdapter(propertyList) { property ->
            val intent = Intent(this, PropertyDetailActivity::class.java)
            intent.putExtra("property", property)
            startActivity(intent)
        }

        binding.recyclerViewMyProperties.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewMyProperties.adapter = adapter

        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        binding.btnChatList.setOnClickListener {
            startActivity(Intent(this, ChatListActivity::class.java))
        }

        binding.btnEditProfile.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        binding.btnManageMyProperties.setOnClickListener {
            val intent = Intent(this, ManagePropertiesActivity::class.java)
            startActivity(intent)
        }

        storage = FirebaseStorage.getInstance()

        binding.btnViewContract.setOnClickListener {
            downloadContractPdf()
        }

        binding.btnContactLandlord.setOnClickListener {
            checkSmsPermissionAndSend()
        }

        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.navMap).setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }

        applyUserTypeVisibility()
        loadMyProperties()
    }

    private fun loadUserName() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("Users").document(uid).get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("name") ?: "사용자"
                binding.tvUserName.text = "$name 님의 마이페이지"
            }
            .addOnFailureListener {
                Toast.makeText(this, "사용자 이름 불러오기 실패: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun applyUserTypeVisibility() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("Users").document(uid).get()
            .addOnSuccessListener { doc ->
                val userType = doc.getString("userType")
                when (userType) {
                    "sublessor" -> {
                        binding.btnMyFavorites.visibility = View.GONE
                        binding.btnContactLandlord.visibility = View.VISIBLE
                    }

                    "sublessee" -> {
                        binding.btnManageMyProperties.visibility = View.GONE
                        binding.btnContactLandlord.visibility = View.GONE
                    }
                }
            }
    }

    private fun loadMyProperties() {
        val currentUserId = auth.currentUser?.uid ?: return

        db.collection("Properties")
            .whereEqualTo("ownerId", currentUserId)
            .get()
            .addOnSuccessListener { result ->
                propertyList.clear()
                for (doc in result) {
                    val property = doc.toObject(Property::class.java)
                    propertyList.add(property)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "내 매물 불러오기 실패: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkSmsPermissionAndSend() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.SEND_SMS),
                SMS_PERMISSION_CODE
            )
        } else {
            sendConsentSmsToLandlord()
        }
    }

    private fun downloadContractPdf() {
        val pdfRef = storage.reference.child("templates/contract.pdf")
        pdfRef.downloadUrl
            .addOnSuccessListener { uri ->
                val url = uri.toString()
                val request = DownloadManager.Request(Uri.parse(url))
                    .setTitle("전대차_계약서.pdf")
                    .setDescription("계약서 파일을 다운로드합니다")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS,
                        "전대차_계약서.pdf"
                    )
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(false)

                val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                dm.enqueue(request)

                Toast.makeText(this, "다운로드를 시작했습니다. 알림을 확인하세요.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "계약서를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendConsentSmsToLandlord() {
        val currentUserId = auth.currentUser?.uid ?: return

        db.collection("Properties")
            .whereEqualTo("ownerId", currentUserId)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    Toast.makeText(this, "등록된 매물이 없습니다.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                for (doc in result) {
                    val property = doc.toObject(Property::class.java)
                    val rawPhone = property.landlordPhone?.trim()
                    val landlordPhone = rawPhone?.replace("-", "")

                    if (landlordPhone.isNullOrBlank() || !landlordPhone.matches(Regex("^\\+?\\d{10,15}$"))) {
                        Toast.makeText(this, "유효한 임대인 전화번호가 없습니다: $rawPhone", Toast.LENGTH_SHORT).show()
                        continue
                    }

                    val message = """
                    [렌트브릿지] 계약서 검토 요청
                    매물 제목: ${property.title}
                    주소: ${property.addressMain + ' ' + property.addressDetail}
                    전대인: ${auth.currentUser?.email}
                    계약서 파일을 참고해주세요.
                """.trimIndent()

                    try {
                        SmsManager.getDefault().sendTextMessage(landlordPhone, null, message, null, null)
                        Toast.makeText(this, "임대인에게 문자 전송 완료", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this, "SMS 전송 실패: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "매물 정보 불러오기 실패: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == SMS_PERMISSION_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            sendConsentSmsToLandlord()
        } else {
            Toast.makeText(this, "SMS 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }
}
