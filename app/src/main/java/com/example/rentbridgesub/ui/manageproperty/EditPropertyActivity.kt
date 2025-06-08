package com.example.rentbridgesub.ui.manageproperty

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.rentbridgesub.data.Property
import com.example.rentbridgesub.databinding.ActivityEditPropertyBinding
import com.example.rentbridgesub.ui.WebView.WebViewActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.MemoryPolicy
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso

class EditPropertyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditPropertyBinding
    private lateinit var property: Property
    private val db = FirebaseFirestore.getInstance()
    private var selectedImageUri: Uri? = null
    private val storage = FirebaseStorage.getInstance()
    private var addressMain = ""      // 도로명 주소
    private var addressDetail = ""    // 상세주소

    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            selectedImageUri = uri
            binding.etImage.setImageURI(uri)
        }

    private val addressLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val address = result.data?.getStringExtra("selectedAddress")
            Log.d("AddPropertyActivity", "받은 주소: $address")  // ✅ 결과 수신 로그
            binding.etAddress.setText(address)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditPropertyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        @Suppress("DEPRECATION")
        property = intent.getSerializableExtra("property") as Property
        Log.d("EditActivity", "받은 property.id = ${property.id}")

        if (property.imageUrl.isNotEmpty()) {
            Picasso.get()
                .load(property.imageUrl)
                .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
                .networkPolicy(NetworkPolicy.NO_CACHE, NetworkPolicy.NO_STORE)
                .into(binding.etImage)
        }

        binding.etImage.setOnClickListener {
            imagePicker.launch("image/*")
        }

        // 기존 데이터 설정
        binding.etTitle.setText(property.title)
        binding.etDescription.setText(property.description)
        binding.etPrice.setText(property.price)
        binding.etAddress.setText(property.addressMain)

        binding.etAddress.apply {
            isFocusable = false
            isFocusableInTouchMode = false
            isCursorVisible = false
            // (Optional) 소프트키보드가 뜨는 걸 방지하려면:
            inputType = InputType.TYPE_NULL

            setOnClickListener {
                // 이 로직은 기존과 동일하게 한 번만 클릭해도 실행됩니다.
                addressLauncher.launch(Intent(this@EditPropertyActivity, WebViewActivity::class.java))
            }
        }

        binding.etAddressDetail.setText(property.addressDetail)
        binding.etStartDate.setText(property.startDate)
        binding.etEndDate.setText(property.endDate)
        binding.etLandlordPhone.setText(property.landlordPhone) // ✅ 추가됨

        binding.btnSave.setOnClickListener {
            val updatedTitle = binding.etTitle.text.toString()
            val updatedDesc = binding.etDescription.text.toString()
            val updatedPrice = binding.etPrice.text.toString()
            val updatedAddressMain = binding.etAddress.text.toString()
            val updatedAddressDetail = binding.etAddressDetail.text.toString()
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
                                updatedAddressMain,
                                updatedAddressDetail,
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
                    updatedAddressMain,
                    updatedAddressDetail,
                    updatedStartDate,
                    updatedEndDate,
                    property.imageUrl,
                    updatedLandlordPhone
                )
            }
        }

        binding.btnDelete.setOnClickListener {
            db.collection("Properties")
                .document(property.id)
                .delete()
                .addOnSuccessListener {
                    Toast.makeText(this, "매물이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                    val resultIntent = Intent().apply {
                        putExtra("deleted", true)
                    }
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "삭제 실패: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateFirestoreProperty(
        title: String,
        desc: String,
        price: String,
        addressMain: String,
        addressDetail: String,
        startDate: String,
        endDate: String,
        imageUrl: String,
        landlordPhone: String
    ) {
        val updatedProperty = property.copy(
            title = title,
            description = desc,
            price = price,
            addressMain = addressMain,
            addressDetail = addressDetail,
            startDate = startDate,
            endDate = endDate,
            imageUrl = imageUrl,
            landlordPhone = landlordPhone
        )

        db.collection("Properties")
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
            .addOnFailureListener {
                Toast.makeText(this, "수정 실패: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
