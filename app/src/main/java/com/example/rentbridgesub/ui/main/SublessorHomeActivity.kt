package com.example.rentbridgesub.ui.main

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.text.format.DateFormat
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.rentbridgesub.R
import com.example.rentbridgesub.data.ChatSummary
import com.example.rentbridgesub.data.Property
import com.example.rentbridgesub.databinding.ActivitySublessorBinding
import com.example.rentbridgesub.ui.chat.ChatActivity
import com.example.rentbridgesub.ui.chat.ChatListActivity
import com.example.rentbridgesub.ui.chat.ChatSummaryAdapter
import com.example.rentbridgesub.ui.manageproperty.ManagePropertiesActivity
import com.example.rentbridgesub.ui.property.AddPropertyActivity
import com.google.android.gms.tasks.Tasks
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import java.net.URLEncoder
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class SublessorHomeActivity : AppCompatActivity() {
    private lateinit var managePropertyLauncher: ActivityResultLauncher<Intent>

    private lateinit var binding: ActivitySublessorBinding
    private lateinit var storage: FirebaseStorage
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val propertyList = mutableListOf<Property>()

    private lateinit var cardNoContract: CardView
    private lateinit var layoutContractStatus: View
    private lateinit var tvContractedPropertyTitle: TextView
    private lateinit var tvStartDate: TextView
    private lateinit var tvEndDate: TextView

    private lateinit var registeredPropertyCard: CardView
    private lateinit var cardNoRegisteredProperty: CardView

    private lateinit var tvSelectContractHint: TextView
    private lateinit var tvSelectedContractName: TextView

    private val summaries = mutableListOf<ChatSummary>()
    private lateinit var summaryAdapter: ChatSummaryAdapter

    // 사용자가 고른 로컬 계약서 URI
    private var selectedContractUri: Uri? = null

    // 파일 선택 결과 처리
    private val pickContractLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                selectedContractUri = uri

                tvSelectContractHint.visibility = View.GONE
                tvSelectedContractName.apply {
                    text = getFileNameFromUri(uri) ?: uri.lastPathSegment
                    visibility = View.VISIBLE
                }

                findViewById<Button>(R.id.btnContactLandlord).isEnabled = true
//                Toast.makeText(this, "계약서 파일 선택됨", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sublessor)

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val nameTextView = findViewById<TextView>(R.id.tvWelcome)
        val titleView = findViewById<TextView>(R.id.tvPropertyTitle)
        val addressView = findViewById<TextView>(R.id.tvPropertyAddress)
        val priceView = findViewById<TextView>(R.id.tvPropertyPrice)

        managePropertyLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                loadLatestProperty(uid, titleView, addressView, priceView)
            }
        }

        findViewById<androidx.cardview.widget.CardView>(R.id.registeredPropertyCard).setOnClickListener {
            managePropertyLauncher.launch(Intent(this, ManagePropertiesActivity::class.java))
        }

        FirebaseFirestore.getInstance().collection("Users").document(uid!!)
            .get()
            .addOnSuccessListener {
                val name = it.getString("name") ?: "사용자"
                nameTextView.text = "$name 님, 환영합니다!"
            }

        registeredPropertyCard       = findViewById(R.id.registeredPropertyCard)
        cardNoRegisteredProperty     = findViewById(R.id.cardNoRegisteredProperty)

        loadLatestProperty(uid, titleView, addressView, priceView)

        tvSelectContractHint      = findViewById(R.id.tvSelectContractHint)
        tvSelectedContractName    = findViewById(R.id.tvSelectedContractName)

        findViewById<LinearLayout>(R.id.navMap).setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
            finish()
        }

        findViewById<LinearLayout>(R.id.navMyPage).setOnClickListener {
            startActivity(Intent(this, MyPageActivity::class.java))
            finish()
        }

        findViewById<LinearLayout>(R.id.navChat).setOnClickListener {
            startActivity(Intent(this, ChatListActivity::class.java))
        }

        findViewById<ExtendedFloatingActionButton>(R.id.btnAddProperty).setOnClickListener {
            startActivity(Intent(this, AddPropertyActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            // stay here
        }

        storage = FirebaseStorage.getInstance()

        findViewById<Button>(R.id.btnViewContract).setOnClickListener {
            downloadContractPdf()
        }

        // 1) 계약서 선택
        findViewById<CardView>(R.id.cardSelectContract).setOnClickListener {
            pickContractLauncher.launch("*/*")  // 모든 파일 중에서 선택, PDF 만 막으려면 "application/pdf"
        }

        findViewById<Button>(R.id.btnContactLandlord).setOnClickListener {
            val uri = selectedContractUri
            Log.d("ContactLandlord", "selectedContractUri=$uri, propertyList.size=${propertyList.size}")
            if (uri != null && propertyList.isNotEmpty()) {
                // 예시로 첫 매물에 대한 요청
                uploadAndSendContract(uri, propertyList[0])
            } else {
                Toast.makeText(this, "파일을 선택하거나 매물을 등록했는지 확인하세요", Toast.LENGTH_SHORT).show()
            }
        }

        cardNoContract = findViewById(R.id.cardNoContract)
        layoutContractStatus = findViewById(R.id.cardContractStatus)
        tvContractedPropertyTitle = findViewById(R.id.tvContractedPropertyTitle)
        tvStartDate  = findViewById(R.id.tvStartDate)
        tvEndDate    = findViewById(R.id.tvEndDate)

        listenForAgreedContracts()
        listenForConsentResponses()

        val rv = findViewById<RecyclerView>(R.id.recentChatsRecycler)
        summaryAdapter = ChatSummaryAdapter(summaries) { summary ->
            // 클릭하면 해당 채팅방 열기
            Intent(this, ChatActivity::class.java).also {
                it.putExtra("receiverId", summary.otherUserId)
                startActivity(it)
            }
        }
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = summaryAdapter

        loadLatestChatSummary()
    }

    private fun loadLatestProperty(uid: String, titleView: TextView, addressView: TextView, priceView: TextView) {
        val imageView = findViewById<ImageView>(R.id.imgProperty)

        FirebaseFirestore.getInstance().collection("Properties")
            .whereEqualTo("ownerId", uid)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val doc = documents.documents[0]
                    val property = doc.toObject(Property::class.java)!!

                    propertyList.clear()
                    propertyList.add(property)

                    titleView.text = doc.getString("title")
                    addressView.text = doc.getString("addressMain") + ' ' + doc.getString("addressDetail")
                    priceView.text = doc.getString("price")

                    val imageUrl = doc.getString("imageUrl")
                    if (!imageUrl.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_placeholder)
                            .into(imageView)
                    } else {
                        imageView.setImageResource(R.drawable.ic_placeholder)
                    }
                    registeredPropertyCard.visibility   = View.VISIBLE
                    cardNoRegisteredProperty.visibility = View.GONE
                } else {
                    registeredPropertyCard.visibility   = View.GONE
                    cardNoRegisteredProperty.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener {
                registeredPropertyCard.visibility   = View.GONE
                cardNoRegisteredProperty.visibility = View.VISIBLE
            }
    }

    private fun listenForAgreedContracts() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("Consents")
            .whereEqualTo("sublessorId", uid)
            .whereEqualTo("response", "agree")
            .limit(1)
            .addSnapshotListener { snaps, error ->
                if (error != null) return@addSnapshotListener

                if (snaps != null && !snaps.isEmpty) {
                    val doc = snaps.documents[0]
                    val propertyId = doc.getString("propertyId") ?: return@addSnapshotListener

                    // 매물 제목 가져오기
                    FirebaseFirestore.getInstance()
                        .collection("Properties")
                        .document(propertyId)
                        .get()
                        .addOnSuccessListener { propDoc ->
                            val title = propDoc.getString("title") ?: "제목 없음"
                            tvContractedPropertyTitle.text = "제목: $title"
                            tvStartDate.text = "시작일: ${propDoc.getString("startDate")}"
                            tvEndDate.text   = "종료일: ${propDoc.getString("endDate")}"
                            layoutContractStatus.visibility = View.VISIBLE
                            cardNoContract.visibility = View.GONE
                        }
                } else {
                    layoutContractStatus.visibility = View.GONE
                    cardNoContract.visibility     = View.VISIBLE
                }
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

    // ContentResolver로 파일명 가져오기
    private fun getFileNameFromUri(uri: Uri): String? {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME),
            null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) return cursor.getString(idx)
            }
        }
        return null
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
            .set(
                mapOf(
                    "propertyId" to property.id,
                    "sublessorId" to currentUser,
                    "fileUrl" to fileUrl,
                    "requestedAt" to FieldValue.serverTimestamp(),
                    "response" to "pending"
                )
            )
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
            동의/거부 버튼 누르시려면 다음 링크를 눌러주세요! --> https://rentbridgesub.web.app/consent.html?req=$reqId&file=${
            URLEncoder.encode(
                fileUrl,
                "UTF-8"
            )
        }
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
                    val resp = data["response"] as? String ?: return@forEach

                    when (resp) {
                        "agree" -> onLandlordAgreed(reqId)
                        "reject" -> onLandlordRejected(reqId)
                    }
                }
            }
    }

    /** 임대인이 동의했을 때 호출 */
    private fun onLandlordAgreed(reqId: String) {
        Toast.makeText(this, "임대인이 동의했습니다!", Toast.LENGTH_LONG).show()
        // TODO: 여기서 원하는 UI 업데이트 코드 추가
    }

    /** 임대인이 거부했을 때 호출 */
    private fun onLandlordRejected(reqId: String) {
        Toast.makeText(this, "임대인이 거부했습니다.", Toast.LENGTH_LONG).show()
        // TODO: 여기서 원하는 UI 업데이트 코드 추가
    }

    private fun loadLatestChatSummary() {
        val me = auth.currentUser!!.uid
        db.collection("ChatRooms")
            .whereArrayContains("users", me)
            .get()
            .addOnSuccessListener { roomsSnap ->
                summaries.clear()
                // 1) 모든 방 마지막 메시지 비동기 조회
                val tasks = roomsSnap.documents.map { roomDoc ->
                    val roomId = roomDoc.id
                    roomDoc.reference
                        .collection("Messages")
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .limit(1)
                        .get()
                        .continueWith { it.result?.documents?.firstOrNull() to roomId }
                }
                Tasks.whenAllSuccess<Pair<DocumentSnapshot?, String>>(tasks)
                    .addOnSuccessListener { results ->
                        // 2) 결과마다 ChatSummary 생성
                        results.forEach { (msgDoc, roomId) ->
                            msgDoc?.let { doc ->
                                val other = roomId.split("-").first { it != me }
                                val summary = ChatSummary(
                                    chatRoomId    = roomId,
                                    otherUserId   = other,
                                    lastMessage   = doc.getString("message")
                                        ?: if (doc.getString("fileUrl") != null) "파일을 보냈습니다."
                                        else if(doc.getString("imageUrl")!=null) "사진을 보냈습니다."
                                        else "",
                                    timestamp     = doc.getLong("timestamp") ?: 0L
                                )
                                summaries.add(summary)
                            }
                        }
                        // 3) User 정보 채우고 정렬
                        val userTasks = summaries.map { summary ->
                            db.collection("Users").document(summary.otherUserId).get()
                                .continueWith { task ->
                                    val userDoc = task.result!!
                                    summary.otherUserName = userDoc.getString("name") ?: "알 수 없음"
                                    summary.avatarUrl     = userDoc.getString("avatarUrl") ?: ""
                                    summary
                                }
                        }
                        Tasks.whenAllSuccess<ChatSummary>(userTasks)
                            .addOnSuccessListener {
                                summaries.sortByDescending { it.timestamp }
                                summaryAdapter.notifyDataSetChanged()
                            }
                    }
            }
    }

    override fun onBackPressed() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("앱 종료")
            .setMessage("앱을 종료하시겠습니까?")
            .setPositiveButton("예") { _, _ -> finishAffinity() }
            .setNegativeButton("아니요", null)
            .show()
    }
}