package com.example.rentbridgesub.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.rentbridgesub.data.Property
import com.example.rentbridgesub.databinding.BottomSheetMapPropertyBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.tvTitle.text = property.title
        binding.tvAddress.text = property.address
        binding.tvPrice.text = property.price
        binding.tvPeriod.text = "${property.startDate} ~ ${property.endDate}"

        // 🔽 이미지 로드
        if (property.imageUrl.isNotEmpty()) {
            com.squareup.picasso.Picasso.get().load(property.imageUrl).into(binding.ivPropertyImage)
        } else {
            binding.ivPropertyImage.setImageResource(android.R.color.darker_gray)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
