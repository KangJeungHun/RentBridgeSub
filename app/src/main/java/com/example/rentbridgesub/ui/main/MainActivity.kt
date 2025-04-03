package com.example.rentbridgesub.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.rentbridgesub.databinding.ActivityMainBinding
import com.example.rentbridgesub.ui.property.AddPropertyActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val uid = auth.currentUser?.uid ?: return

        db.collection("Users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val userType = document.getString("userType")
                    binding.tvWelcome.text = "환영합니다! $userType"

                    if (userType == "sublessor") {
                        // 전대인 → 전체 매물 보기 버튼 숨기기
                        binding.btnPropertyList.visibility = View.GONE
                    } else if (userType == "sublessee") {
                        // 전차인 → 매물 등록 버튼만 숨기기, 마이페이지는 그대로
                        binding.btnAddProperty.visibility = View.GONE
                    }
                } else {
                    binding.tvWelcome.text = "유저 정보 없음"
                }
            }
            .addOnFailureListener {
                binding.tvWelcome.text = "유저 정보 불러오기 실패"
            }

        binding.btnAddProperty.setOnClickListener {
            startActivity(Intent(this, AddPropertyActivity::class.java))
        }

        binding.btnMyPage.setOnClickListener {
            startActivity(Intent(this, MyPageActivity::class.java))
        }

        binding.btnPropertyList.setOnClickListener {
            startActivity(Intent(this, PropertyListActivity::class.java))
        }
    }
}
