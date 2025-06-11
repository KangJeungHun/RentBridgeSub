package com.example.rentbridgesub.ui.main

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rentbridgesub.data.Property
import com.example.rentbridgesub.databinding.ActivityPropertyListBinding
import com.google.firebase.firestore.FirebaseFirestore

class PropertyListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPropertyListBinding
    private val db = FirebaseFirestore.getInstance()
    private val propertyList = mutableListOf<Property>()
    private lateinit var adapter: PropertyAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPropertyListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = PropertyAdapter(propertyList) { property ->
            val intent = Intent(this, PropertyDetailActivity::class.java)
            intent.putExtra("property", property)
            startActivity(intent)
        }

        binding.recyclerViewAllProperties.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewAllProperties.adapter = adapter

        loadAllProperties()
    }

    private fun loadAllProperties() {
        db.collection("Properties")
            .get()
            .addOnSuccessListener { result ->
                propertyList.clear()

                val allProperties = result.map { it.toObject(Property::class.java) }

                val recommended = allProperties.filter {
                    it.price.toIntOrNull()?.let { price -> price < 1000000 } == true
                }.map {
                    it.copy(isRecommended = true) // ✨ 추천 표시
                }

                val nonRecommended = allProperties.filter {
                    it.price.toIntOrNull()?.let { price -> price >= 1000000 } == true
                }

                // 추천 먼저 정렬
                propertyList.addAll(recommended + nonRecommended)

                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "매물 불러오기 실패: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

}
