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
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.net.URLEncoder
import java.util.UUID

class MyPageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyPageBinding
    private lateinit var storage: FirebaseStorage
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val propertyList = mutableListOf<Property>()
    private lateinit var adapter: PropertyAdapter
    private val SMS_PERMISSION_CODE = 1001

    // 사용자가 고른 로컬 계약서 URI
    private var selectedContractUri: Uri? = null

    // 파일 선택 결과 처리
    private val pickContractLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                selectedContractUri = uri
                binding.btnContactLandlord.isEnabled = true
                Toast.makeText(this, "계약서 파일 선택됨", Toast.LENGTH_SHORT).show()
            }
        }

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

        // 1) 계약서 선택
        binding.btnSelectContract.setOnClickListener {
            pickContractLauncher.launch("*/*")  // 모든 파일 중에서 선택, PDF 만 막으려면 "application/pdf"
        }

        binding.btnContactLandlord.setOnClickListener {
            val uri = selectedContractUri
            if (uri != null && propertyList.isNotEmpty()) {
                // 예시로 첫 매물에 대한 요청
                uploadAndSendContract(uri, propertyList[0])
            } else {
                Toast.makeText(this, "파일을 선택하거나 매물을 등록했는지 확인하세요", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.navMap).setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }

        applyUserTypeVisibility()
        loadMyProperties()

        // 4) sublessor 가 보낸 모든 동의 요청의 응답을 실시간으로 감지
        listenForConsentResponses()
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

//    private fun sharePdfWithTextToKakao(messageText: String) {
//        // 1) Firebase Storage에서 PDF 다운로드 URL 가져오기
//        val pdfRef = FirebaseStorage.getInstance()
//            .reference.child("templates/contract.pdf")
//        pdfRef.downloadUrl
//            .addOnSuccessListener { uri ->
//                // 2) 공유 인텐트 준비
//                val sendIntent = Intent(Intent.ACTION_SEND).apply {
//                    // PDF + 텍스트를 함께 보내기 위해 MIME 타입은 application/pdf
//                    type = "application/pdf"
//                    // PDF URI
//                    putExtra(Intent.EXTRA_STREAM, uri)
//                    // 메시지 텍스트
//                    putExtra(Intent.EXTRA_TEXT, messageText)
//                    // 카카오톡으로만 보내도록 패키지 지정
//                    `package` = "com.kakao.talk"
//                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//                }
//                // 3) 카톡이 설치되어 있으면 실행, 아니면 토스트
//                if (sendIntent.resolveActivity(packageManager) != null) {
//                    startActivity(sendIntent)
//                } else {
//                    Toast.makeText(this,
//                        "카카오톡이 설치되어 있지 않습니다.", Toast.LENGTH_SHORT).show()
//                }
//            }
//            .addOnFailureListener {
//                Toast.makeText(this,
//                    "PDF 파일을 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
//            }
//    }


//    private fun checkSmsPermissionAndSend() {
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
//            != PackageManager.PERMISSION_GRANTED
//        ) {
//            ActivityCompat.requestPermissions(
//                this,
//                arrayOf(Manifest.permission.SEND_SMS),
//                SMS_PERMISSION_CODE
//            )
//        } else {
//            sendConsentSmsToLandlord()
//        }
//    }

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

    //
    // --------------------------------------
    // 4) 업로드 → SMS 발송
    private fun uploadAndSendContract(uri: Uri, property: Property) {
        // a) 고유 요청 ID 생성
        val reqId = UUID.randomUUID().toString()
        // b) Firebase Storage 경로
        val ref = storage.reference.child("user_contracts/$reqId.pdf")
        // c) 업로드 후 다운로드 URL 획득
        ref.putFile(uri)
            .continueWithTask { it.result!!.storage.downloadUrl }
            .addOnSuccessListener { downloadUrl ->
                // d) Firestore 에 요청 정보 저장
                saveConsentRequest(reqId, property, downloadUrl.toString())
            }
            .addOnFailureListener {
                Toast.makeText(this, "업로드 실패: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveConsentRequest(reqId: String, property: Property, fileUrl: String) {
        val currentUser = auth.currentUser?.uid ?: return
        db.collection("Consents").document(reqId)
            .set(mapOf(
                "propertyId"  to property.id,
                "sublessorId" to currentUser,
                "fileUrl"     to fileUrl,
                "requestedAt" to FieldValue.serverTimestamp(),
                "response"    to "pending"
            ))
            .addOnSuccessListener {
                sendConsentSms(reqId, property, fileUrl)
            }
            .addOnFailureListener {
                Toast.makeText(this, "요청 저장 실패: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendConsentSms(reqId: String, property: Property, fileUrl: String) {
        val phoneNumber = property.landlordPhone
            ?.replace("-", "") ?: return

        val message = """
            [RentBridgeSub] 전대차 계약서 검토 요청
            매물: ${property.title}
            주소: ${property.addressMain} ${property.addressDetail}
            계약서 링크: $fileUrl
            동의/거부 버튼 누르시려면 다음 링크를 눌러주세요! --> https://rentbridge.app/consent?req=$reqId
        """.trimIndent()

        val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:${Uri.encode(phoneNumber)}")
            putExtra("sms_body", message)
        }
        if (smsIntent.resolveActivity(packageManager) != null) {
            startActivity(smsIntent)
        } else {
            Toast.makeText(this, "SMS 앱이 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }


//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == SMS_PERMISSION_CODE &&
//            grantResults.isNotEmpty() &&
//            grantResults[0] == PackageManager.PERMISSION_GRANTED
//        ) {
//            sendConsentSmsToLandlord()
//        } else {
//            Toast.makeText(this, "SMS 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
//        }
//    }

    /**
     * sublessorId == 내 uid 인 모든 Consents 문서를 구독해서,
     * response 필드가 변경될 때마다 콜백을 받는다.
     */
    private fun listenForConsentResponses() {
        val myId = auth.currentUser?.uid ?: return

        db.collection("Consents")
            .whereEqualTo("sublessorId", myId)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("ConsentListen", "리스너 오류", error)
                    return@addSnapshotListener
                }
                snapshots?.documentChanges?.forEach { change ->
                    val data = change.document.data
                    val reqId = change.document.id
                    val resp  = data["response"] as? String ?: return@forEach

                    when (resp) {
                        "agree"  -> onLandlordAgreed(reqId)
                        "reject" -> onLandlordRejected(reqId)
                    }
                }
            }
    }

    /** 임대인이 동의했을 때 호출 */
    private fun onLandlordAgreed(reqId: String) {
        Toast.makeText(this, "임대인이 동의했습니다! (req=$reqId)", Toast.LENGTH_LONG).show()
        // TODO: 여기서 원하는 UI 업데이트 코드 추가
    }

    /** 임대인이 거부했을 때 호출 */
    private fun onLandlordRejected(reqId: String) {
        Toast.makeText(this, "임대인이 거부했습니다. (req=$reqId)", Toast.LENGTH_LONG).show()
        // TODO: 여기서 원하는 UI 업데이트 코드 추가
    }
}
