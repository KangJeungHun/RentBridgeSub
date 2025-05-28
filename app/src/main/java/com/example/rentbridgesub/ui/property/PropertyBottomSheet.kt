package com.example.rentbridgesub.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.rentbridgesub.data.Property
import com.example.rentbridgesub.databinding.BottomSheetMapPropertyBinding
import com.example.rentbridgesub.ui.chat.ChatActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
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
        binding.tvAddress.text = property.address
        binding.tvPrice.text = property.price
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
