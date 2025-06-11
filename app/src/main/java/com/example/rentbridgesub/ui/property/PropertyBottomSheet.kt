package com.example.rentbridgesub.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.rentbridgesub.R
import com.example.rentbridgesub.data.Property
import com.example.rentbridgesub.databinding.BottomSheetMapPropertyBinding
import com.example.rentbridgesub.ui.chat.ChatActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class PropertyBottomSheet(private val property: Property) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetMapPropertyBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetMapPropertyBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onStart() {
        super.onStart()
        dialog?.let {
            val bottomSheet = it.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.layoutParams?.height = ViewGroup.LayoutParams.MATCH_PARENT
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.tvTitle.text = property.title
        binding.tvAddress.text = property.addressMain + ' ' + property.addressDetail
        binding.tvPrice.text = "${property.price} 만원"
        binding.tvPeriod.text = "${property.startDate} ~ ${property.endDate}"
        binding.tvDescription.text = property.description

        // 🔍 등록자 이름 가져오기
        val db = FirebaseFirestore.getInstance()
        db.collection("Users").document(property.ownerId)  // 'Users' 컬렉션에 있다고 가정
            .get()
            .addOnSuccessListener { document ->
                val name = document.getString("name") ?: "이름 없음"
                binding.tvOwnerName.text = "등록자: $name"
            }
            .addOnFailureListener {
                binding.tvOwnerName.text = "등록자 정보를 불러올 수 없음"
            }

        // 🔽 이미지 로드
        if (property.imageUrl.isNotEmpty()) {
            com.squareup.picasso.Picasso.get().load(property.imageUrl).into(binding.ivPropertyImage)
        } else {
            binding.ivPropertyImage.setImageResource(android.R.color.darker_gray)
        }

        binding.btnChat.setOnClickListener {
            val intent = Intent(requireContext(), ChatActivity::class.java)
            intent.putExtra("receiverId", property.ownerId)  // Property 데이터에 들어있다고 가정
            startActivity(intent)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
