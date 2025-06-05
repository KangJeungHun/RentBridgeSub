package com.example.rentbridgesub.ui.chat

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.rentbridgesub.R

class FullImageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_image)

        val imageView: ImageView = findViewById(R.id.fullImageView)
        val imageUrl = intent.getStringExtra("imageUrl")

        if (imageUrl != null) {
            Glide.with(this).load(imageUrl).into(imageView)
        }

        findViewById<ImageView>(R.id.btnClose).setOnClickListener {
            finish()
        }
    }
}
