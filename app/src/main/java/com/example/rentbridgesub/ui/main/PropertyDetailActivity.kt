package com.example.rentbridgesub.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.rentbridgesub.data.Property
import com.example.rentbridgesub.databinding.ActivityPropertyDetailBinding
import com.example.rentbridgesub.ui.chat.ChatActivity
import com.squareup.picasso.Picasso
import com.google.firebase.auth.FirebaseAuth

class PropertyDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPropertyDetailBinding
    private var property: Property? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPropertyDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        property = intent.getSerializableExtra("property") as? Property

        property?.let {
            binding.tvDetailAddress.text = it.address
            binding.tvDetailPeriod.text = "${it.startDate} ~ ${it.endDate}"
            binding.tvDetailPrice.text = it.price

            if (it.imageUrl.isNotEmpty()) {
                Picasso.get().load(it.imageUrl).into(binding.ivDetailImage)
            } else {
                binding.ivDetailImage.setImageResource(android.R.color.darker_gray)
            }

            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            if (it.ownerId == currentUserId) {
                binding.btnChat.visibility = View.GONE
            } else {
                binding.btnChat.visibility = View.VISIBLE
                binding.btnChat.setOnClickListener {
                    val intent = Intent(this, ChatActivity::class.java)
                    intent.putExtra("propertyId", property?.id)
                    intent.putExtra("receiverId", property?.ownerId)
                    startActivity(intent)
                }
            }
        }
    }
}
