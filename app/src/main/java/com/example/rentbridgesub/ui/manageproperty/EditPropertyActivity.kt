package com.example.rentbridgesub.ui.manageproperty

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.rentbridgesub.data.Property
import com.example.rentbridgesub.databinding.ActivityEditPropertyBinding
import com.google.firebase.firestore.FirebaseFirestore

class EditPropertyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditPropertyBinding
    private lateinit var property: Property
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditPropertyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        @Suppress("DEPRECATION")
        property = intent.getSerializableExtra("property") as Property

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

            db.collection("Properties").document(property.id)
                .update(
                    mapOf(
                        "title" to updatedTitle,
                        "description" to updatedDesc,
                        "price" to updatedPrice,
                        "address" to updatedAddress,
                        "startDate" to updatedStartDate,
                        "endDate" to updatedEndDate
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
}
