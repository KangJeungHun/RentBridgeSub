package com.example.rentbridgesub.ui.main

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.format.DateFormat
import android.text.style.ImageSpan
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
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
import com.example.rentbridgesub.ui.manageproperty.EditPropertyActivity
import com.example.rentbridgesub.ui.manageproperty.ManagePropertiesActivity
import com.example.rentbridgesub.ui.property.AddPropertyActivity
import com.google.android.gms.tasks.Tasks
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import java.net.URLEncoder
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class SublessorHomeActivity : AppCompatActivity() {
    private lateinit var managePropertyLauncher: ActivityResultLauncher<Intent>
    private lateinit var editPropertyLauncher: ActivityResultLauncher<Intent>

    private lateinit var storage: FirebaseStorage
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val propertyList = mutableListOf<Property>()

    private lateinit var cardNoContract: CardView
    private lateinit var cardContractStatus: View
    private lateinit var tvContractedPropertyTitle: TextView
    private lateinit var tvSublesseeId: TextView
    private lateinit var tvSublesseePhone: TextView
    private lateinit var tvStartDate: TextView
    private lateinit var tvEndDate: TextView

    private lateinit var registeredPropertyCard: CardView
    private lateinit var cardNoRegisteredProperty: CardView

    private lateinit var tvSelectContractHint: TextView
    private lateinit var tvSelectedContractName: TextView
    private lateinit var btnContactLandlord: Button

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

    private lateinit var btnAddProperty: ExtendedFloatingActionButton
    private var myPropertyId: String? = null
    private var propertyListener: ListenerRegistration? = null

    private lateinit var spinnerTenants: Spinner
    private val tenantOptions = mutableListOf<Pair<String, String>>()
// Pair<UID, DisplayName>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sublessor)

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val nameTextView = findViewById<TextView>(R.id.tvWelcome)
        val titleView = findViewById<TextView>(R.id.tvPropertyTitle)
        val addressView = findViewById<TextView>(R.id.tvPropertyAddress)
        val priceView = findViewById<TextView>(R.id.tvPropertyPrice)
        val imageView   = findViewById<ImageView>(R.id.imgProperty)
        val startDateView = findViewById<TextView>(R.id.tvStartDate)
        val endDateView = findViewById<TextView>(R.id.tvEndDate)
        btnAddProperty = findViewById(R.id.btnAddProperty)
        registeredPropertyCard = findViewById(R.id.registeredPropertyCard)
        cardNoRegisteredProperty = findViewById(R.id.cardNoRegisteredProperty)
        tvSelectContractHint    = findViewById(R.id.tvSelectContractHint)
        tvSelectedContractName  = findViewById(R.id.tvSelectedContractName)
        spinnerTenants          = findViewById(R.id.spinnerTenants)
        btnContactLandlord      = findViewById(R.id.btnContactLandlord)

        editPropertyLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // 수정 후 돌아왔을 때, 필요하다면 여기에서 UI 갱신 처리
                loadLatestProperty(uid, titleView, addressView, priceView)
            }
        }

//        managePropertyLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
//            if (result.resultCode == RESULT_OK) {
//                loadLatestProperty(uid, titleView, addressView, priceView)
//            }
//        }

//        findViewById<androidx.cardview.widget.CardView>(R.id.registeredPropertyCard).setOnClickListener {
//            managePropertyLauncher.launch(Intent(this, ManagePropertiesActivity::class.java))
//        }

        findViewById<androidx.cardview.widget.CardView>(R.id.registeredPropertyCard).setOnClickListener {
            if (propertyList.isNotEmpty()) {
                val prop = propertyList[0]
                Intent(this, EditPropertyActivity::class.java).apply {
                    putExtra("property", prop)
                }.also { startActivity(it) }
            } else {
                Toast.makeText(this, "등록된 매물이 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        FirebaseFirestore.getInstance().collection("Users").document(uid!!)
            .get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("name") ?: "사용자"
                val isStudent = doc.getBoolean("isStudent") == true

                if (isStudent) {
                    // 1) 기본 텍스트
                    val welcome = " 님, 환영합니다!"
                    val fullText = name + " " + welcome   // 이름 뒤에 빈칸 하나를 미리 추가

                    // 2) SpannableStringBuilder 생성
                    val ssb = SpannableStringBuilder(fullText)

                    // 3) 아이콘을 넣을 위치 계산 (이름 바로 뒤)
                    val iconIndexStart = name.length
                    val iconIndexEnd   = iconIndexStart + 1  // 빈칸 한 글자 자리

                    val badgeDp = 28
                    val badgePx = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        badgeDp.toFloat(),
                        resources.displayMetrics
                    ).toInt()

                    // 4) Drawable 준비
                    val drawable = ContextCompat.getDrawable(this, R.drawable.ic_badge_student)!!
                    drawable.setBounds(0, 0, badgePx, badgePx)

                    // 5) ImageSpan 설정
                    ssb.setSpan(
                        ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM),
                        iconIndexStart,
                        iconIndexEnd,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )

                    // 6) TextView에 적용
                    nameTextView.text = ssb

                } else {
                    nameTextView.text = "$name 님, 환영합니다!"
                }
            }

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

        btnAddProperty.setOnClickListener {
            startActivity(Intent(this, AddPropertyActivity::class.java))
        }

        // 카드 눌렀을 때 수정 화면으로
        registeredPropertyCard.setOnClickListener {
            myPropertyId?.let { id ->
                val prop = propertyList.first()
                Intent(this, EditPropertyActivity::class.java)
                    .putExtra("property", prop)
                    .also { editPropertyLauncher.launch(it) }
            }
        }

        // **여기서부터 실시간 리스너 시작**
        db.collection("Properties")
            .whereEqualTo("ownerId", uid)
            .limit(1)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e("PropListen", "리스너 오류", err)
                    return@addSnapshotListener
                }
                val doc = snap?.documents?.firstOrNull()
                if (doc != null && doc.exists()) {
                    // 문서가 있으면 UI 업데이트
                    val p = doc.toObject(Property::class.java)!!
                    myPropertyId = doc.id
                    propertyList.clear()
                    propertyList.add(p)

                    titleView.text   = p.title
                    addressView.text = "${p.addressMain} ${p.addressDetail}"
                    priceView.text   = p.price
                    startDateView.text = p.startDate
                    endDateView.text = p.endDate
                    if (p.imageUrl.isNotEmpty()) {
                        Glide.with(this)
                            .load(p.imageUrl)
                            .placeholder(R.drawable.ic_placeholder)
                            .into(imageView)
                    } else {
                        imageView.setImageResource(R.drawable.ic_placeholder)
                    }

                    registeredPropertyCard.visibility   = View.VISIBLE
                    cardNoRegisteredProperty.visibility = View.GONE
                    btnAddProperty.hide()
                } else {
                    // 문서가 없으면 등록 버튼 보여주기
                    registeredPropertyCard.visibility   = View.GONE
                    cardNoRegisteredProperty.visibility = View.VISIBLE
                    btnAddProperty.show()
                }
            }
        // **리스너 끝**

        loadLatestProperty(uid, titleView, addressView, priceView)

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

        // 1) 사용자 UID
        val me = auth.currentUser!!.uid

        // 2) ChatRooms 컬렉션에서, 내가 속한 1:1 방들을 불러와서
        db.collection("ChatRooms")
            .whereArrayContains("users", me)
            .get()
            .addOnSuccessListener { snaps ->
                val tasks = snaps.documents.map { doc ->
                    // 방 ID에서 otherUID 추출 (id 형식이 "me-other")
                    val otherId = doc.id.split("-").first { it != me }
                    // 사용자 이름 조회
                    db.collection("Users").document(otherId).get()
                        .continueWith { it.result!!.getString("name")!! to otherId }
                }
                Tasks.whenAllSuccess<Pair<String,String>>(tasks)
                    .addOnSuccessListener { results ->
                        tenantOptions.clear()
                        results.forEach { (name, uid) ->
                            tenantOptions.add(uid to name)
                        }
                        val namesWithHint = listOf("선택:") + tenantOptions.map { it.second }
                        val adapter = ArrayAdapter(
                            this,
                            android.R.layout.simple_spinner_item,
                            namesWithHint // 이름 리스트
                        ).apply {
                            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        }
                        spinnerTenants.adapter = adapter
                    }
            }

        findViewById<Button>(R.id.btnContactLandlord).setOnClickListener {
            val pos = spinnerTenants.selectedItemPosition
            // pos == 0 이면 힌트(미선택) 상태
            if (pos <= 0) {
                Toast.makeText(this, "전차인을 선택하세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val uri = selectedContractUri
            Log.d("ContactLandlord", "selectedContractUri=$uri, propertyList.size=${propertyList.size}")
            if (uri != null && propertyList.isNotEmpty()) {
                // 스피너에서 선택된 인덱스 → tenantOptions 에서 UID 꺼내기
                val pos = spinnerTenants.selectedItemPosition
                Log.d("tenantOptions", "tenantOptions.size_pos=$tenantOptions.size")

                val sublesseeId = tenantOptions[pos - 1].first

                // 예시로 첫 매물에 대한 요청
                uploadAndSendContract(uri, propertyList[0], sublesseeId)
            } else {
                Toast.makeText(this, "파일을 선택하거나 매물을 등록했는지 확인하세요", Toast.LENGTH_SHORT).show()
            }
        }

        cardNoContract = findViewById(R.id.cardNoContract)
        cardContractStatus = findViewById(R.id.cardContractStatus)
        tvContractedPropertyTitle = findViewById(R.id.tvContractedPropertyTitle)
        tvSublesseeId = findViewById(R.id.tvSublesseeId)
        tvSublesseePhone = findViewById(R.id.tvSublesseePhone)
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
                    priceView.text = "${doc.getString("price")} 만원"

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

                    btnAddProperty.hide()
                } else {
                    registeredPropertyCard.visibility   = View.GONE
                    cardNoRegisteredProperty.visibility = View.VISIBLE
                    btnAddProperty.show()
                }
            }
            .addOnFailureListener {
                registeredPropertyCard.visibility   = View.GONE
                cardNoRegisteredProperty.visibility = View.VISIBLE
                btnAddProperty.show()
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

                    // 매물 정보 가져오기
                    FirebaseFirestore.getInstance()
                        .collection("Properties")
                        .document(propertyId)
                        .get()
                        .addOnSuccessListener { propDoc ->
                            val title = propDoc.getString("title") ?: "제목 없음"
                            tvContractedPropertyTitle.text = "제목: $title"
                            tvStartDate.text = "시작일: ${propDoc.getString("startDate")}"
                            tvEndDate.text   = "종료일: ${propDoc.getString("endDate")}"
                            cardContractStatus.visibility = View.VISIBLE
                            cardNoContract.visibility = View.GONE
                        }

                    FirebaseFirestore.getInstance()
                        .collection("Consents")
                        .whereEqualTo("sublessorId", uid)
                        .get()
                        .addOnSuccessListener { prop ->
                            val sublesseeId = prop.documents[0].getString("sublesseeId") ?: return@addOnSuccessListener
                            FirebaseFirestore.getInstance()
                                .collection("Users")
                                .whereEqualTo("uid", sublesseeId)
                                .get()
                                .addOnSuccessListener { propDoc ->
                                    val userDoc = propDoc.documents[0]
                                    tvSublesseeId.text = "전차인: ${userDoc.getString("name")}"

                                    val phone = userDoc.getString("phone") ?: ""
                                    if (phone.contains("-")) {
                                        // 하이픈이 이미 있는 경우
                                        tvSublesseePhone.text = "연락처: $phone"
                                    } else {
                                        // 하이픈이 없는(숫자만) 경우
                                        val formatted = phone.replaceFirst(
                                            Regex("""(\d{3})(\d{4})(\d{4})"""),
                                            "$1-$2-$3"
                                        )
                                        tvSublesseePhone.text = "연락처: $formatted"
                                    }
                                }
                        }

                    FirebaseFirestore.getInstance()
                        .collection("Properties")
                        .document(propertyId)
                        .update("status", "rented")
                        .addOnSuccessListener {
//                            Toast.makeText(this, "매물이 계약 완료되어 숨김 처리됩니다.", Toast.LENGTH_LONG).show()
                        }
                        .addOnFailureListener { e ->
                            Log.e("StatusUpdate", "status 업데이트 실패", e)
                        }
                } else {
                    cardContractStatus.visibility = View.GONE
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
    private fun uploadAndSendContract(uri: Uri, property: Property, sublesseeId: String) {
        // a) 고유 요청 ID 생성
        val reqId = UUID.randomUUID().toString()
        // b) Firebase Storage 경로
        val ref = storage.reference.child("user_contracts/$reqId.pdf")
        // c) 업로드 후 다운로드 URL 획득
        ref.putFile(uri)
            .continueWithTask { it.result!!.storage.downloadUrl }
            .addOnSuccessListener { downloadUrl ->
                // d) Firestore 에 요청 정보 저장
                saveConsentRequest(reqId, property, downloadUrl.toString(), sublesseeId)
            }
            .addOnFailureListener {
                Toast.makeText(this, "업로드 실패: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveConsentRequest(reqId: String, property: Property, fileUrl: String, sublesseeId: String) {
        val sublessorId = auth.currentUser?.uid ?: return
        db.collection("Consents").document(reqId)
            .set(
                mapOf(
                    "propertyId" to property.id,
                    "sublessorId" to sublessorId,
                    "sublesseeId"  to sublesseeId,
                    "fileUrl" to fileUrl,
                    "requestedAt" to FieldValue.serverTimestamp(),
                    "response" to "pending"
                )
            )
            .addOnSuccessListener {
                sendConsentSms(reqId, property, fileUrl, sublessorId, sublesseeId)
            }
            .addOnFailureListener {
                Toast.makeText(this, "요청 저장 실패: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendConsentSms(reqId: String, property: Property, fileUrl: String, sublessorId: String, sublesseeId: String) {
        // 1) 두 사용자 문서 읽기
        val userColl = db.collection("Users")
        val sublessorTask = userColl.document(sublessorId).get()
        val sublesseeTask = userColl.document(sublesseeId).get()

        Tasks.whenAllSuccess<DocumentSnapshot>(sublessorTask, sublesseeTask)
            .addOnSuccessListener { results ->
                val sublessorName = (results[0].getString("name") ?: "전대인")
                val sublesseeName = (results[1].getString("name") ?: "전차인")

                // 2) SMS 본문 구성
                val phoneNumber = property.landlordPhone
                    ?.replace("-", "") ?: return@addOnSuccessListener

                val message = """
                [RentBridgeSub] 전대차 계약서 검토 요청
                매물: ${property.title}
                주소: ${property.addressMain} ${property.addressDetail}
                전대인: $sublessorName
                전차인: $sublesseeName
                계약서 링크: $fileUrl
                동의/거부 하시려면 오른쪽 링크를 눌러주세요!: https://rentbridgesub.web.app/consent.html?req=$reqId&file=${
                    URLEncoder.encode(fileUrl, "UTF-8")
                }
            """.trimIndent()

                // 3) SMS 인텐트 전송
                Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("smsto:${Uri.encode(phoneNumber)}")
                    putExtra("sms_body", message)
                }.also { smsIntent ->
                    if (smsIntent.resolveActivity(packageManager) != null) {
                        startActivity(smsIntent)
                    } else {
                        Toast.makeText(this, "SMS 앱이 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "사용자 정보를 불러오는 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show()
            }
    }

    /**
     * sublessorId == 내 uid 인 모든 Consents 문서를 구독해서,
     * response 필드가 변경될 때마다 콜백을 받는다.
     */
    private var consentListener: ListenerRegistration? = null

    private fun listenForConsentResponses() {
        val myId = auth.currentUser?.uid ?: return

        consentListener = db.collection("Consents")
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
                    consentListener?.remove()
                }
            }
    }

    /** 임대인이 동의했을 때 호출 */
    private fun onLandlordAgreed(reqId: String) {
        Toast.makeText(this, "임대인이 동의하였습니다!", Toast.LENGTH_LONG).show()

        // 1) 전송 버튼 비활성화
        btnContactLandlord.isEnabled = false
        btnContactLandlord.text = "임대인이 동의하였습니다!"

        // 2) 선택 UI도 같이 숨기고 싶으면
        tvSelectContractHint.visibility   = View.GONE
        tvSelectedContractName.visibility = View.GONE
        spinnerTenants.isEnabled          = false
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