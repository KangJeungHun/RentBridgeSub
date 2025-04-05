package com.example.rentbridgesub.ui.property

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.rentbridgesub.data.Property
import com.example.rentbridgesub.databinding.ActivityAddPropertyBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class AddPropertyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddPropertyBinding
    private var selectedImageUri: Uri? = null
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        selectedImageUri = uri
        binding.ivProperty.setImageURI(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddPropertyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivProperty.setOnClickListener {
            imagePicker.launch("image/*")
        }

        binding.btnRegister.setOnClickListener {
            val title = binding.etAddress.text.toString()
            val price = binding.etPrice.text.toString()
            val description = "${binding.etStartDate.text} ~ ${binding.etEndDate.text}"

            if (title.isNotEmpty() && price.isNotEmpty() && description.isNotEmpty()) {
                uploadProperty(title, price, description)
            } else {
                Toast.makeText(this, "모든 정보를 입력하세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uploadProperty(title: String, price: String, description: String) {
        val propertyId = UUID.randomUUID().toString()

        if (selectedImageUri != null) {
            val storageRef = storage.reference.child("properties/$propertyId.jpg")
            storageRef.putFile(selectedImageUri!!)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        saveProperty(propertyId, title, price, description, uri.toString())
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "이미지 업로드 실패: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            // 이미지를 선택하지 않은 경우
            saveProperty(propertyId, title, price, description, "")
        }
    }

    private fun saveProperty(
        propertyId: String,
        title: String,
        price: String,
        description: String,
        imageUrl: String
    ) {
        val property = Property(
            id = propertyId,
            title = title,
            price = price,
            description = description,
            ownerId = auth.currentUser?.uid ?: "",
            imageUrl = imageUrl
        )

        db.collection("Properties").document(propertyId).set(property)
            .addOnSuccessListener {
                Toast.makeText(this, "매물 등록 완료", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "매물 등록 실패: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
