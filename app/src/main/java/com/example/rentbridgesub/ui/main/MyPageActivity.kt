package com.example.rentbridgesub.ui.main

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rentbridgesub.data.Property
import com.example.rentbridgesub.databinding.ActivityMyPageBinding
import com.example.rentbridgesub.ui.chat.ChatListActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MyPageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyPageBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val propertyList = mutableListOf<Property>()
    private lateinit var adapter: PropertyAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = PropertyAdapter(propertyList) { property ->
            val intent = Intent(this, PropertyDetailActivity::class.java)
            intent.putExtra("property", property)
            startActivity(intent)
        }

        binding.recyclerViewMyProperties.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewMyProperties.adapter = adapter

        // ⭐️ 채팅 목록 버튼 클릭 처리
        binding.btnChatList.setOnClickListener {
            startActivity(Intent(this, ChatListActivity::class.java))
        }

        loadMyProperties()
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
}
