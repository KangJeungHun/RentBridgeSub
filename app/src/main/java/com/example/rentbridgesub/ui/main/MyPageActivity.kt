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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rentbridgesub.R
import com.example.rentbridgesub.data.Property
import com.example.rentbridgesub.databinding.ActivityMyPageBinding
import com.example.rentbridgesub.ui.auth.LoginActivity
import com.example.rentbridgesub.ui.chat.ChatListActivity
import com.example.rentbridgesub.ui.editprofile.EditProfileActivity
import com.example.rentbridgesub.ui.manageproperty.ManagePropertiesActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.net.URLEncoder
import java.util.UUID

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

        loadUserName()

        adapter = PropertyAdapter(propertyList) { property ->
            val intent = Intent(this, PropertyDetailActivity::class.java)
            intent.putExtra("property", property)
            startActivity(intent)
        }

        binding.recyclerViewMyProperties.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewMyProperties.adapter = adapter

        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        binding.btnChatList.setOnClickListener {
            startActivity(Intent(this, ChatListActivity::class.java))
        }

        binding.btnEditProfile.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        binding.btnManageMyProperties.setOnClickListener {
            val intent = Intent(this, ManagePropertiesActivity::class.java)
            startActivity(intent)
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

        applyUserTypeVisibility()
        loadMyProperties()
    }

    private fun loadUserName() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("Users").document(uid).get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("name") ?: "사용자"
                binding.tvUserName.text = "$name 님의 마이페이지"
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

    override fun onBackPressed() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("앱 종료")
            .setMessage("앱을 종료하시겠습니까?")
            .setPositiveButton("예") { _, _ -> finishAffinity() }
            .setNegativeButton("아니요", null)
            .show()
    }
}
