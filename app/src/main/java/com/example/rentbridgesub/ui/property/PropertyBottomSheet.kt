package com.example.rentbridgesub.ui.property

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.rentbridgesub.R
import com.example.rentbridgesub.data.Property
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PropertyBottomSheet(private val property: Property) : BottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_map_property, container, false)
        view.findViewById<TextView>(R.id.tvTitle).text = property.title
        view.findViewById<TextView>(R.id.tvAddress).text = property.address
        view.findViewById<TextView>(R.id.tvPrice).text = property.price
//        view.findViewById<TextView>(R.id.tvPeriod).text = "${property.startDate} ~ ${property.endDate}"
        return view
    }
}
