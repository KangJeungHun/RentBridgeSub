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
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rentbridgesub.R
import com.example.rentbridgesub.data.Property
import com.example.rentbridgesub.databinding.ActivityMyPageBinding
import com.example.rentbridgesub.ui.auth.LoginActivity
import com.example.rentbridgesub.ui.chat.ChatListActivity
import com.example.rentbridgesub.ui.editprofile.EditProfileActivity
import com.example.rentbridgesub.ui.editprofile.StudentProfileActivity
import com.example.rentbridgesub.ui.favorites.FavoritesActivity
import com.example.rentbridgesub.ui.manageproperty.ManagePropertiesActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import org.w3c.dom.Text
import java.net.URLEncoder
import java.util.UUID

class MyPageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyPageBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val propertyList = mutableListOf<Property>()
    private lateinit var adapter: PropertyAdapter

    private lateinit var cardNoContract: CardView
    private lateinit var cardContractStatus: View
    private lateinit var tvContractedPropertyTitle: TextView
    private lateinit var tvSublessorId: TextView
    private lateinit var tvSublessorPhone: TextView
    private lateinit var tvStartDate: TextView
    private lateinit var tvEndDate: TextView

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

        binding.tvLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        binding.btnChatList.setOnClickListener {
            startActivity(Intent(this, ChatListActivity::class.java))
        }

        binding.tvEditProfile.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        binding.btnVerifyStudent.setOnClickListener {
            startActivity(Intent(this, StudentProfileActivity::class.java))
        }

        binding.btnMyFavorites.setOnClickListener {
            startActivity(Intent(this, FavoritesActivity::class.java))
        }

        binding.btnManageMyProperties.setOnClickListener {
            val intent = Intent(this, ManagePropertiesActivity::class.java)
            startActivity(intent)
        }

        val homeBtn = findViewById<LinearLayout>(R.id.navHome)
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
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

                val tvUserName = findViewById<TextView>(R.id.tvUserName)

                // 3) **여기**: 학생 인증 여부에 따라 뱃지 붙이기
                if (doc.getBoolean("isStudent") == true) {
                    tvUserName.setCompoundDrawablesWithIntrinsicBounds(
                        0, 0, R.drawable.ic_badge_student, 0
                    )
                } else {
                    tvUserName.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                }
            }

        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        findViewById<LinearLayout>(R.id.navMap).setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
            finish()
        }

        findViewById<LinearLayout>(R.id.navChat).setOnClickListener {
            startActivity(Intent(this, ChatListActivity::class.java))
        }

        cardNoContract = findViewById(R.id.cardNoContract)
        cardContractStatus = findViewById(R.id.cardContractStatus)
        tvContractedPropertyTitle = findViewById(R.id.tvContractedPropertyTitle)
        tvSublessorId = findViewById(R.id.tvSublessorId)
        tvSublessorPhone = findViewById(R.id.tvSublessorPhone)
        tvStartDate  = findViewById(R.id.tvStartDate)
        tvEndDate    = findViewById(R.id.tvEndDate)

        applyUserTypeVisibility()
        loadMyProperties()
        listenForAgreedContracts()
    }

    private fun loadUserName() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("Users").document(uid).get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("name") ?: "사용자"
                val email = doc.getString("email") ?: ""
                binding.tvUserName.text = "$name"
                binding.tvUserEmail.text = "$email"
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
                        binding.cardNoContract.visibility = View.GONE
                        binding.tvcardContractStatus.visibility = View.GONE
                    }

                    "sublessee" -> {
                        binding.btnManageMyProperties.visibility = View.GONE
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

    private fun listenForAgreedContracts() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("Consents")
            .whereEqualTo("sublesseeId", uid)
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
                            tvSublessorId.text = "전대인: ${propDoc.getString("name")}"
                            tvStartDate.text = "시작일: ${propDoc.getString("startDate")}"
                            tvEndDate.text   = "종료일: ${propDoc.getString("endDate")}"
                            cardContractStatus.visibility = View.VISIBLE
                            cardNoContract.visibility = View.GONE
                        }

                    FirebaseFirestore.getInstance()
                        .collection("Consents")
                        .whereEqualTo("sublesseeId", uid)
                        .get()
                        .addOnSuccessListener { prop ->
                            val sublessorId = prop.documents[0].getString("sublessorId") ?: return@addOnSuccessListener
                            FirebaseFirestore.getInstance()
                                .collection("Users")
                                .whereEqualTo("uid", sublessorId)
                                .get()
                                .addOnSuccessListener { propDoc ->
                                    val userDoc = propDoc.documents[0]
                                    tvSublessorId.text = "전대인: ${userDoc.getString("name")}"

                                    val phone = userDoc.getString("phone") ?: ""
                                    if (phone.contains("-")) {
                                        // 하이픈이 이미 있는 경우
                                        tvSublessorPhone.text = "연락처: $phone"
                                    } else {
                                        // 하이픈이 없는(숫자만) 경우
                                        val formatted = phone.replaceFirst(
                                            Regex("""(\d{3})(\d{4})(\d{4})"""),
                                            "$1-$2-$3"
                                        )
                                        tvSublessorPhone.text = "연락처: $formatted"
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

    override fun onBackPressed() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("앱 종료")
            .setMessage("앱을 종료하시겠습니까?")
            .setPositiveButton("예") { _, _ -> finishAffinity() }
            .setNegativeButton("아니요", null)
            .show()
    }
}
