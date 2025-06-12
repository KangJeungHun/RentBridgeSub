package com.example.rentbridgesub.ui.main

import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.util.TypedValue
import android.view.View // ✅ 추가!
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.rentbridgesub.R
import com.example.rentbridgesub.data.Property
import com.example.rentbridgesub.databinding.ActivityPropertyDetailBinding
import com.example.rentbridgesub.ui.chat.ChatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class PropertyDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPropertyDetailBinding
    private lateinit var property: Property
    private var isRecommended: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPropertyDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        property = (intent.getSerializableExtra("property") as? Property)!!
        isRecommended = intent.getBooleanExtra("isRecommended", false)

        val db = FirebaseFirestore.getInstance()
        property?.let {
            db.collection("Users").document(it.ownerId)  // 'Users' 컬렉션에 있다고 가정
                .get()
                .addOnSuccessListener { document ->
                    val name = document.getString("name") ?: "이름 없음"
                    val isStudent = document.getBoolean("isStudent") == true

                    // 1) 기본 텍스트
                    val prefix = "등록자: "
                    val fullText = prefix + name

                    // 2) SpannableStringBuilder 로 생성
                    val ssb = SpannableStringBuilder(fullText)

                    // 3) 학생 인증된 경우에만 badge 삽입
                    if (isStudent) {
                        // badge 를 넣을 위치: prefix + name 바로 뒤
                        val iconPosStart = prefix.length + name.length
                        val iconPosEnd   = iconPosStart + 1

                        val badgeDp = 20
                        val badgePx = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            badgeDp.toFloat(),
                            resources.displayMetrics
                        ).toInt()

                        // 4) Drawable 준비
                        val drawable = ContextCompat.getDrawable(this, R.drawable.ic_badge_student)!!
                        drawable.setBounds(0, 0, badgePx, badgePx)

                        // 5) 이름 뒤에 빈 칸 하나 넣기
                        ssb.insert(iconPosStart, " ")

                        // 6) ImageSpan 설정
                        ssb.setSpan(
                            ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM),
                            iconPosStart,
                            iconPosEnd,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }

                    // 7) TextView 에 적용
                    binding.tvOwnerName.text = ssb
                }
                .addOnFailureListener {
                    binding.tvOwnerName.text = "등록자 정보를 불러올 수 없음"
                }
        }

        property?.let { prop ->
            binding.tvTitle.text = prop.title
            binding.tvDescription.text = prop.description
            binding.tvAddress.text = prop.addressMain + ' ' + prop.addressDetail
            binding.tvPeriod.text = "${prop.startDate} ~ ${prop.endDate}"
            binding.tvPrice.text = "${prop.price} 만원"

            if (prop.imageUrl.isNotEmpty()) {
                Glide.with(binding.ivPropertyImage.context)
                    .load(prop.imageUrl)
                    .placeholder(R.drawable.ic_placeholder)
                    .into(binding.ivPropertyImage)
            } else {
                binding.ivPropertyImage.setImageResource(android.R.color.darker_gray)
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

        // 1) 현재 유저
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userRef = FirebaseFirestore.getInstance().collection("Users").document(uid)

        FirebaseFirestore.getInstance().collection("Users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                when (doc.getString("userType")) {
                    "sublessor" -> { // 전대인이면, 채팅 및 찜 가리기
                        binding.btnFavorite.visibility = View.GONE

                        if (uid == property.ownerId) {
                            binding.btnChat.visibility = View.GONE
                        }
                    }
                }
            }

        // 2) 즐겨찾기 버튼 초기 상태 결정
        val favBtn = binding.btnFavorite
        userRef.get().addOnSuccessListener { doc ->
            val favs = doc.get("favorites") as? List<String> ?: emptyList()
            val isFav = property.id in favs
            favBtn.setImageResource(
                if (isFav) R.drawable.ic_favorite_filled
                else    R.drawable.ic_favorite_border
            )
        }

        // 3) 버튼 클릭 시 토글
        favBtn.setOnClickListener {
            userRef.get().addOnSuccessListener { doc ->
                val favs = doc.get("favorites") as? List<String> ?: emptyList()
                if (property.id in favs) {
                    // 이미 찜→제거
                    userRef.update("favorites", FieldValue.arrayRemove(property.id))
                        .addOnSuccessListener {
                            favBtn.setImageResource(R.drawable.ic_favorite_border)
                        }
                } else {
                    // 찜 추가
                    userRef.update("favorites", FieldValue.arrayUnion(property.id))
                        .addOnSuccessListener {
                            favBtn.setImageResource(R.drawable.ic_favorite_filled)
                        }
                }
            }
        }
    }
}
