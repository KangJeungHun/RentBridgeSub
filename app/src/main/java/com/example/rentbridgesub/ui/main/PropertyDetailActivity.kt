package com.example.rentbridgesub.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.View // ✅ 추가!
import androidx.appcompat.app.AppCompatActivity
import com.example.rentbridgesub.data.Property
import com.example.rentbridgesub.databinding.ActivityPropertyDetailBinding
import com.example.rentbridgesub.ui.chat.ChatActivity
import com.squareup.picasso.Picasso
import com.google.firebase.auth.FirebaseAuth

class PropertyDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPropertyDetailBinding
    private var property: Property? = null
    private var isRecommended: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPropertyDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        property = intent.getSerializableExtra("property") as? Property
        isRecommended = intent.getBooleanExtra("isRecommended", false)

        property?.let { prop ->
            binding.tvDetailTitle.text = prop.title
            binding.tvDetailDescription.text = prop.description
            binding.tvDetailAddress.text = prop.addressMain + ' ' + prop.addressDetail
            binding.tvDetailPeriod.text = "${prop.startDate} ~ ${prop.endDate}"
            binding.tvDetailPrice.text = prop.price

            if (prop.imageUrl.isNotEmpty()) {
                Picasso.get().load(prop.imageUrl).into(binding.ivDetailImage)
            } else {
                binding.ivDetailImage.setImageResource(android.R.color.darker_gray)
            }

            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            if (prop.ownerId == currentUserId) {
                binding.btnChat.visibility = View.GONE
            } else {
                binding.btnChat.visibility = View.VISIBLE
                binding.btnChat.setOnClickListener {
                    val intent = Intent(this, ChatActivity::class.java)
                    intent.putExtra("receiverId", prop.ownerId)
                    startActivity(intent)
                }
            }

            // ✅ 추천 여부 표시
            binding.tvRecommended.visibility = if (isRecommended) View.VISIBLE else View.GONE
        }
    }
}
