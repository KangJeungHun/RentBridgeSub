package com.example.rentbridgesub.ui.main

import android.content.Intent
import android.os.Bundle
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

        property?.let { prop ->
            binding.tvDetailTitle.text = prop.title
            binding.tvDetailDescription.text = prop.description
            binding.tvDetailAddress.text = prop.address
            binding.tvDetailPeriod.text = "${prop.startDate} ~ ${prop.endDate}"
            binding.tvDetailPrice.text = prop.price

            if (prop.imageUrl.isNotEmpty()) {
                Picasso.get().load(prop.imageUrl).into(binding.ivDetailImage)
            } else {
                binding.ivDetailImage.setImageResource(android.R.color.darker_gray)
            }

            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            if (prop.ownerId == currentUserId) {
                binding.btnChat.visibility = android.view.View.GONE
            } else {
                binding.btnChat.visibility = android.view.View.VISIBLE
                binding.btnChat.setOnClickListener {
                    val intent = Intent(this, ChatActivity::class.java)
                    intent.putExtra("receiverId", prop.ownerId) // 🔥 정확히 ownerId 넘기기
                    startActivity(intent)
                }
            }
        }
    }
}
