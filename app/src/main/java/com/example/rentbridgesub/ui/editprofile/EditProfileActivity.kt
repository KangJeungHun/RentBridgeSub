package com.example.rentbridgesub.ui.editprofile

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.rentbridgesub.databinding.ActivityEditProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditProfileBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadUserData()

        binding.btnSave.setOnClickListener {
            val name = binding.editName.text.toString().trim()
            val phone = binding.editPhone.text.toString().trim()

            val userId = auth.currentUser?.uid ?: return@setOnClickListener

            db.collection("Users").document(userId)
                .update(mapOf("name" to name, "phone" to phone))
                .addOnSuccessListener {
                    Toast.makeText(this, "수정 성공", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "수정 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("Users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    binding.editName.setText(document.getString("name") ?: "")
                    binding.editPhone.setText(document.getString("phone") ?: "")
                }
            }
    }
}
