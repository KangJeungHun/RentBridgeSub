package com.example.rentbridgesub.ui.main

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.rentbridgesub.R
import com.example.rentbridgesub.ui.property.AddPropertyActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SublessorHomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sublessor)

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val nameTextView = findViewById<TextView>(R.id.tvWelcome)

        FirebaseFirestore.getInstance().collection("Users").document(uid!!)
            .get()
            .addOnSuccessListener {
                val name = it.getString("name") ?: "사용자"
                nameTextView.text = "$name 님, 환영합니다!"
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

    override fun onBackPressed() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("앱 종료")
            .setMessage("앱을 종료하시겠습니까?")
            .setPositiveButton("예") { _, _ -> finishAffinity() }
            .setNegativeButton("아니요", null)
            .show()
    }
}