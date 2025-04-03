package com.example.rentbridgesub.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.rentbridgesub.data.Property
import com.example.rentbridgesub.databinding.ActivityPropertyDetailBinding
import com.squareup.picasso.Picasso

class PropertyDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPropertyDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPropertyDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val property = intent.getSerializableExtra("property") as? Property
        property?.let {
            binding.tvDetailAddress.text = it.address
            binding.tvDetailPeriod.text = "${it.startDate} ~ ${it.endDate}"
            binding.tvDetailPrice.text = "${it.price}원"
            Picasso.get().load(it.imageUrl).into(binding.ivDetailImage)
        }
    }
}
