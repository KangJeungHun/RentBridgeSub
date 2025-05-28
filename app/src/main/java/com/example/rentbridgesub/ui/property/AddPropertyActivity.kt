package com.example.rentbridgesub.ui.property

import android.net.Uri
import android.os.Bundle
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

            if (title.isNotEmpty() && description.isNotEmpty() &&
                price.isNotEmpty() && address.isNotEmpty() &&
                startDate.isNotEmpty() && endDate.isNotEmpty()) {

                if (selectedImageUri == null) {
//                    uploadImageAndRegister(title, description, address, price, startDate, endDate)
                    Toast.makeText(this, "이미지를 선택하세요", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                } else {
                    uploadImageAndRegister(title, description, address, price, startDate, endDate)
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
        endDate: String
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
                            downloadUrl.toString()
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
        imageUrl: String
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
            put("imageUrl", imageUrl)
        }

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
                Toast.makeText(this, "오류: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )

        Volley.newRequestQueue(this).add(request)
    }
}
