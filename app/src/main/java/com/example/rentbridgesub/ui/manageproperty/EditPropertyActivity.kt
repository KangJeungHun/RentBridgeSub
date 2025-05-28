package com.example.rentbridgesub.ui.manageproperty

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.rentbridgesub.data.Property
import com.example.rentbridgesub.databinding.ActivityEditPropertyBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso

class EditPropertyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditPropertyBinding
    private lateinit var property: Property
    private val db = FirebaseFirestore.getInstance()

    private var selectedImageUri: Uri? = null
    private val storage = FirebaseStorage.getInstance()

    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            selectedImageUri = uri
            binding.etImage.setImageURI(uri)
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditPropertyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        @Suppress("DEPRECATION")
        property = intent.getSerializableExtra("property") as Property

//        // 기존 이미지 로드 (Glide 또는 Picasso 사용)
//        Picasso.get()
//            .load(property.imageUrl)
//            .into(binding.etImage)

        // 이미지 뷰 클릭 시 이미지 선택
        binding.etImage.setOnClickListener {
            imagePicker.launch("image/*")
        }

        binding.etTitle.setText(property.title)
        binding.etDescription.setText(property.description)
        binding.etPrice.setText(property.price)
        binding.etAddress.setText(property.address)
        binding.etStartDate.setText(property.startDate)
        binding.etEndDate.setText(property.endDate)

        binding.btnUpdate.setOnClickListener {
            val updatedTitle = binding.etTitle.text.toString()
            val updatedDesc = binding.etDescription.text.toString()
            val updatedPrice = binding.etPrice.text.toString()
            val updatedAddress = binding.etAddress.text.toString()
            val updatedStartDate = binding.etStartDate.text.toString()
            val updatedEndDate = binding.etEndDate.text.toString()

            if (!property.id.isNullOrEmpty() && selectedImageUri != null) {
                val extension = contentResolver.getType(selectedImageUri!!)
                    ?.substringAfterLast("/") ?: "jpg"

                Log.d("Upload", "selectedImageUri = $selectedImageUri")
                Log.d("Upload", "property.id = ${property.id}")
                Log.d("Upload", "Uploading image to properties/${property.id}.$extension")


                val fileRef = storage.reference.child("properties/${property.id}.$extension")
                val inputStream = contentResolver.openInputStream(selectedImageUri!!)

                fileRef.putStream(inputStream!!)
                    .continueWithTask { task ->
                        if (!task.isSuccessful) {
                            task.exception?.let { throw it }
                        }
                        fileRef.downloadUrl
                    }
                    .addOnSuccessListener { downloadUri ->
                        updateFirestoreProperty(
                            updatedTitle, updatedDesc, updatedPrice,
                            updatedAddress, updatedStartDate, updatedEndDate,
                            downloadUri.toString()
                        )
                    }
                    .addOnFailureListener {
                        Log.e("UploadError", "이미지 업로드 실패", it)
                        Toast.makeText(this, "이미지 업로드 실패: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "이미지 또는 매물 ID가 유효하지 않습니다", Toast.LENGTH_SHORT).show()
                updateFirestoreProperty(
                    updatedTitle, updatedDesc, updatedPrice,
                    updatedAddress, updatedStartDate, updatedEndDate,
                    property.imageUrl // 기존 이미지 유지
                )
            }
        }
    }

    private fun updateFirestoreProperty(
        title: String,
        desc: String,
        price: String,
        address: String,
        startDate: String,
        endDate: String,
        imageUrl: String
    ) {
        db.collection("Properties").document(property.id)
            .update(
                mapOf(
                    "title" to title,
                    "description" to desc,
                    "price" to price,
                    "address" to address,
                    "startDate" to startDate,
                    "endDate" to endDate,
                    "imageUrl" to imageUrl
                )
            )
            .addOnSuccessListener {
                Toast.makeText(this, "매물 수정 완료", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "수정 실패: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

}
