package com.example.rentbridgesub.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.rentbridgesub.databinding.ActivityMainBinding
import com.example.rentbridgesub.ui.auth.LoginActivity
import com.example.rentbridgesub.ui.chat.ChatListActivity
import com.example.rentbridgesub.ui.property.AddPropertyActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var userType: String = ""
    private var userName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val uid = auth.currentUser?.uid
        if (uid == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {
            db.collection("Users").document(uid)
                .get()
                .addOnSuccessListener { document ->
                    userType = document.getString("userType") ?: ""
                    userName = document.getString("name") ?: ""
                    binding.tvWelcome.text = "환영합니다! $userName"

                    if (userType == "sublessor") {
                        binding.btnAddProperty.visibility = android.view.View.VISIBLE
                        binding.btnViewProperties.visibility = android.view.View.GONE
                    } else {
                        binding.btnAddProperty.visibility = android.view.View.GONE
                        binding.btnViewProperties.visibility = android.view.View.VISIBLE
                    }
                }
                .addOnFailureListener {
                    binding.tvWelcome.text = "유저 정보 불러오기 실패"
                }
        }

        binding.btnAddProperty.setOnClickListener {
            startActivity(Intent(this, AddPropertyActivity::class.java)) // 🔥 수정 완료
        }

        binding.btnMyPage.setOnClickListener {
            startActivity(Intent(this, MyPageActivity::class.java))
        }

        binding.btnViewProperties.setOnClickListener {
            startActivity(Intent(this, PropertyListActivity::class.java))
        }
    }
}
