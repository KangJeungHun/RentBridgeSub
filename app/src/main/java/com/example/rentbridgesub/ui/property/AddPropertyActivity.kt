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
import java.util.UUID

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
            val address = binding.etAddress.text.toString()
            val price = binding.etPrice.text.toString()
            val startDate = binding.etStartDate.text.toString()
            val endDate = binding.etEndDate.text.toString()

            if (address.isNotEmpty() && startDate.isNotEmpty() && endDate.isNotEmpty()) {
                uploadProperty(address, price, startDate, endDate)
            } else {
                Toast.makeText(this, "주소와 계약 기간을 입력하세요", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uploadProperty(address: String, price: String, startDate: String, endDate: String) {
        val propertyId = UUID.randomUUID().toString()

        if (selectedImageUri != null) {
            val storageRef = storage.reference.child("properties/$propertyId.jpg")
            storageRef.putFile(selectedImageUri!!)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        saveProperty(propertyId, address, price, startDate, endDate, uri.toString())
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "사진 업로드 실패: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            // 사진 없이 등록
            saveProperty(propertyId, address, price, startDate, endDate, "")
        }
    }

    private fun saveProperty(
        propertyId: String,
        address: String,
        price: String,
        startDate: String,
        endDate: String,
        imageUrl: String
    ) {
        val property = Property(
            id = propertyId,
            ownerId = auth.currentUser?.uid ?: "",
            address = address,
            price = price,
            startDate = startDate,
            endDate = endDate,
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
