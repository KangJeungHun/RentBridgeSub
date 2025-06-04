package com.example.rentbridgesub.ui.property

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.rentbridgesub.databinding.ActivityAddPropertyBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import org.json.JSONObject
import java.util.UUID

class AddPropertyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddPropertyBinding
    private var selectedImageUri: Uri? = null
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
            val title = binding.etTitle.text.toString()
            val description = binding.etDescription.text.toString()
            val address = binding.etAddress.text.toString()
            val price = binding.etPrice.text.toString()
            val startDate = binding.etStartDate.text.toString()
            val endDate = binding.etEndDate.text.toString()
            val landlordPhone = binding.etLandlordPhone.text.toString()

            if (title.isNotEmpty() && description.isNotEmpty() &&
                price.isNotEmpty() && address.isNotEmpty() &&
                startDate.isNotEmpty() && endDate.isNotEmpty() &&
                landlordPhone.isNotEmpty()) {

                if (selectedImageUri == null) {
                    // 이미지 없이 등록
                    callRegisterPropertyFunction(
                        UUID.randomUUID().toString(),
                        title,
                        description,
                        address,
                        price,
                        startDate,
                        endDate,
                        "", // imageUrl을 빈 문자열로 명시
                        landlordPhone
                    )
                } else {
                    // 이미지 업로드 후 등록
                    uploadImageAndRegister(title, description, address, price, startDate, endDate, landlordPhone)
                }
            } else {
                Toast.makeText(this, "모든 정보를 입력하세요", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uploadImageAndRegister(
        title: String,
        description: String,
        address: String,
        price: String,
        startDate: String,
        endDate: String,
        landlordPhone: String
    ) {
        val propertyId = UUID.randomUUID().toString()
        val storageRef = storage.reference.child("properties/$propertyId.jpg")

        selectedImageUri?.let { uri ->
            storageRef.putFile(uri)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                        callRegisterPropertyFunction(
                            propertyId,
                            title,
                            description,
                            address,
                            price,
                            startDate,
                            endDate,
                            downloadUrl.toString(),
                            landlordPhone
                        )
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "사진 업로드 실패: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun callRegisterPropertyFunction(
        propertyId: String,
        title: String,
        description: String,
        address: String,
        price: String,
        startDate: String,
        endDate: String,
        imageUrl: String,
        landlordPhone: String
    ) {
        val json = JSONObject().apply {
            put("id", propertyId)
            put("ownerId", auth.currentUser?.uid)
            put("title", title)
            put("description", description)
            put("address", address)
            put("price", price)
            put("startDate", startDate)
            put("endDate", endDate)
            put("imageUrl", imageUrl) // 항상 명시
            put("landlordPhone", landlordPhone)
        }
        Log.d("RegisterProperty", "보내는 JSON 데이터: $json")

        val url = "https://us-central1-rentbridgesub.cloudfunctions.net/registerProperty"

        val request = JsonObjectRequest(
            Request.Method.POST,
            url,
            json,
            { response ->
                Toast.makeText(this, "등록 성공", Toast.LENGTH_SHORT).show()
                finish()
            },
            { error ->
                Log.e("RegisterProperty", "에러 상세: ${error.networkResponse?.statusCode} / ${error.message}")
                Toast.makeText(this, "오류: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )

        Volley.newRequestQueue(this).add(request)
    }
}
