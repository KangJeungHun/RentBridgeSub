package com.example.rentbridgesub.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
import com.example.rentbridgesub.R
import com.example.rentbridgesub.ui.chat.ChatListActivity
import com.example.rentbridgesub.ui.manageproperty.ManagePropertiesActivity
import com.example.rentbridgesub.ui.property.AddPropertyActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class SublessorHomeActivity : AppCompatActivity() {
    private lateinit var managePropertyLauncher: ActivityResultLauncher<Intent>

    private lateinit var cardNoContract: CardView
    private lateinit var layoutContractStatus: View
    private lateinit var tvContractedPropertyTitle: TextView
    private lateinit var tvStartDate: TextView
    private lateinit var tvEndDate: TextView

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

        loadLatestProperty(uid, titleView, addressView, priceView)

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

        findViewById<Button>(R.id.btnAddProperty).setOnClickListener {
            startActivity(Intent(this, AddPropertyActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            // stay here
        }

        cardNoContract = findViewById(R.id.cardNoContract)
        layoutContractStatus = findViewById(R.id.cardContractStatus)
        tvContractedPropertyTitle = findViewById(R.id.tvContractedPropertyTitle)
        tvStartDate  = findViewById(R.id.tvStartDate)
        tvEndDate    = findViewById(R.id.tvEndDate)

        listenForAgreedContracts()
    }

    private fun loadLatestProperty(uid: String, titleView: TextView, addressView: TextView, priceView: TextView) {
        val imageView = findViewById<ImageView>(R.id.imgProperty)

        FirebaseFirestore.getInstance().collection("Properties")
            .whereEqualTo("ownerId", uid)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val property = documents.documents[0]
                    titleView.text = property.getString("title")
                    addressView.text = property.getString("addressMain") + ' ' + property.getString("addressDetail")
                    priceView.text = property.getString("price")

                    val imageUrl = property.getString("imageUrl")
                    if (!imageUrl.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_placeholder)
                            .into(imageView)
                    } else {
                        imageView.setImageResource(R.drawable.ic_placeholder)
                    }
                } else {
                    titleView.text = "등록된 매물이 없습니다"
                    addressView.text = ""
                    priceView.text = ""
                }
            }
            .addOnFailureListener {
                titleView.text = "매물 정보를 불러올 수 없습니다"
                addressView.text = ""
                priceView.text = ""
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

    override fun onBackPressed() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("앱 종료")
            .setMessage("앱을 종료하시겠습니까?")
            .setPositiveButton("예") { _, _ -> finishAffinity() }
            .setNegativeButton("아니요", null)
            .show()
    }
}