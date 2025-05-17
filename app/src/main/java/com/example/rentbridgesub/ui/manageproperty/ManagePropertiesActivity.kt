package com.example.rentbridgesub.ui.manageproperty

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rentbridgesub.data.Property
import com.example.rentbridgesub.databinding.ActivityManagePropertiesBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ManagePropertiesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityManagePropertiesBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val propertyList = mutableListOf<Property>()
    private lateinit var adapter: ManagePropertyAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManagePropertiesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = ManagePropertyAdapter(propertyList) { property ->
            val intent = Intent(this, EditPropertyActivity::class.java)
            intent.putExtra("property", property)
            startActivity(intent)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        loadMyProperties()
    }

    private fun loadMyProperties() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("Properties")
            .whereEqualTo("ownerId", userId)
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
                Toast.makeText(this, "매물 불러오기 실패: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
