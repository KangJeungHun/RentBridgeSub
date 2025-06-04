package com.example.rentbridgesub.ui.manageproperty

import android.app.Activity
import android.content.Intent
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
import com.squareup.picasso.MemoryPolicy
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso

class EditPropertyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditPropertyBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var property: Property
    private var selectedImageUri: Uri? = null

    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedImageUri = it
                binding.etImage.setImageURI(it)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditPropertyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        // 받아온 Property 객체
        property = intent.getSerializableExtra("property") as Property
        Log.d("EditActivity", "받은 property.id = ${property.id}")

        // 기존 이미지 표시
        if (!property.imageUrl.isNullOrEmpty()) {
            Picasso.get()
                .load(property.imageUrl)
                .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
                .networkPolicy(NetworkPolicy.NO_CACHE, NetworkPolicy.NO_STORE)
                .into(binding.etImage)
        }

        // 이미지 뷰 클릭 시 이미지 선택
        binding.etImage.setOnClickListener {
            imagePicker.launch("image/*")
        }

        // 기존 정보 세팅
        binding.etTitle.setText(property.title)
        binding.etDescription.setText(property.description)
        binding.etAddress.setText(property.address)
        binding.etPrice.setText(property.price)
        binding.etStartDate.setText(property.startDate)
        binding.etEndDate.setText(property.endDate)
        binding.etLandlordPhone.setText(property.landlordPhone)

        // 수정 버튼
        binding.btnUpdate.setOnClickListener {
            val updatedTitle = binding.etTitle.text.toString()
            val updatedDesc = binding.etDescription.text.toString()
            val updatedPrice = binding.etPrice.text.toString()
            val updatedAddress = binding.etAddress.text.toString()
            val updatedStartDate = binding.etStartDate.text.toString()
            val updatedEndDate = binding.etEndDate.text.toString()
            val updatedLandlordPhone = binding.etLandlordPhone.text.toString()

            if (selectedImageUri != null) {
                val fileRef = storage.reference.child("properties/${property.id}.jpg")

                fileRef.putFile(selectedImageUri!!)
                    .addOnSuccessListener {
                        fileRef.downloadUrl.addOnSuccessListener { downloadUri ->
                            updateFirestoreProperty(
                                updatedTitle,
                                updatedDesc,
                                updatedPrice,
                                updatedAddress,
                                updatedStartDate,
                                updatedEndDate,
                                downloadUri.toString(),
                                updatedLandlordPhone
                            )
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "사진 업로드 실패: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                updateFirestoreProperty(
                    updatedTitle,
                    updatedDesc,
                    updatedPrice,
                    updatedAddress,
                    updatedStartDate,
                    updatedEndDate,
                    property.imageUrl,
                    updatedLandlordPhone
                )
            }
        }

        // 삭제 버튼
        binding.btnDelete.setOnClickListener {
            firestore.collection("Properties")
                .document(property.id)
                .delete()
                .addOnSuccessListener {
                    Toast.makeText(this, "매물이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "삭제 실패: ${e.message}", Toast.LENGTH_SHORT).show()
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
        imageUrl: String,
        landlordPhone: String
    ) {
        val updatedProperty = property.copy(
            title = title,
            description = desc,
            price = price,
            address = address,
            startDate = startDate,
            endDate = endDate,
            imageUrl = imageUrl,
            landlordPhone = landlordPhone
        )

        firestore.collection("Properties")
            .document(property.id)
            .set(updatedProperty)
            .addOnSuccessListener {
                Toast.makeText(this, "매물이 수정되었습니다.", Toast.LENGTH_SHORT).show()
                val resultIntent = Intent().apply {
                    putExtra("updatedProperty", updatedProperty)
                }
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "수정 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
