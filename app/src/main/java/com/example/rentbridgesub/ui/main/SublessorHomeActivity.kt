package com.example.rentbridgesub.ui.main

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.rentbridgesub.R
import com.example.rentbridgesub.ui.manageproperty.ManagePropertiesActivity
import com.example.rentbridgesub.ui.property.AddPropertyActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SublessorHomeActivity : AppCompatActivity() {
    private lateinit var managePropertyLauncher: ActivityResultLauncher<Intent>

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
            startActivity(Intent(this, MyPageActivity::class.java))
            finish()
        }

        findViewById<Button>(R.id.btnAddProperty).setOnClickListener {
            startActivity(Intent(this, AddPropertyActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            // stay here
        }
    }

    private fun loadLatestProperty(uid: String, titleView: TextView, addressView: TextView, priceView: TextView) {
        FirebaseFirestore.getInstance().collection("Properties")
            .whereEqualTo("ownerId", uid)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val property = documents.documents[0]
                    titleView.text = property.getString("title") ?: "제목 없음"
                    addressView.text = property.getString("address") ?: "주소 없음"
                    priceView.text = property.getString("price") ?: "가격 정보 없음"
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

    override fun onBackPressed() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("앱 종료")
            .setMessage("앱을 종료하시겠습니까?")
            .setPositiveButton("예") { _, _ -> finishAffinity() }
            .setNegativeButton("아니요", null)
            .show()
    }
}