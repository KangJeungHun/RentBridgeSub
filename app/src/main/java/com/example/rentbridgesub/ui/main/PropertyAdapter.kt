package com.example.rentbridgesub.ui.main

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.rentbridgesub.R
import com.example.rentbridgesub.data.Property
import com.example.rentbridgesub.databinding.ItemPropertyBinding
import com.example.rentbridgesub.ui.chat.ChatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class PropertyAdapter(
    private val properties: List<Property>,
    private val onItemClick: (Property) -> Unit
) : RecyclerView.Adapter<PropertyAdapter.PropertyViewHolder>() {

    private val db = FirebaseFirestore.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser!!.uid

    inner class PropertyViewHolder(val binding: ItemPropertyBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PropertyViewHolder {
        val binding = ItemPropertyBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PropertyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PropertyViewHolder, position: Int) {
        val property = properties[position]
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        // 이미지 로드
        val thumbView = holder.binding.ivPropertyThumb
        if (!property.imageUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView)
                .load(property.imageUrl)
                .placeholder(R.drawable.ic_placeholder)
                .into(thumbView)
        } else {
            thumbView.setImageResource(R.drawable.ic_placeholder)
        }

        // 1) 현재 유저의 favorites 배열에 prop.id 가 있는지 미리 조회
        db.collection("Users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val favs = doc.get("favorites") as? List<String> ?: emptyList()
                val isFav = property.id in favs
                holder.binding.btnFavorite.setImageResource(
                    if (isFav) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border
                )
            }

        // 2) 찜 버튼 클릭하면 토글
        holder.binding.btnFavorite.setOnClickListener {
            Log.d("FavDebug", "하트 클릭! propertyId=${property.id}")
            val userRef = db.collection("Users").document(uid)
            userRef.get().addOnSuccessListener { doc ->
                val rawfavs = doc.get("favorites") as? List<String> ?: emptyList()
                val favs = rawfavs.filter { it.isNotBlank() }
                Log.d("FavDebug", "현재 favorites: $favs")

                if (property.id in favs) {
                    userRef.update("favorites", FieldValue.arrayRemove(property.id))
                        .addOnSuccessListener {
                            Log.d("FavDebug", "제거 성공")
                            holder.binding.btnFavorite.setImageResource(R.drawable.ic_favorite_border)
                        }
                        .addOnFailureListener { e ->
                            Log.e("FavDebug", "제거 실패", e)
                        }
                } else {
                    userRef.update("favorites", FieldValue.arrayUnion(property.id))
                        .addOnSuccessListener {
                            Log.d("FavDebug", "추가 성공")
                            holder.binding.btnFavorite.setImageResource(R.drawable.ic_favorite_filled)
                        }
                        .addOnFailureListener { e ->
                            Log.e("FavDebug", "추가 실패", e)
                        }
                }
            }.addOnFailureListener { e ->
                Log.e("FavDebug", "유저 문서 조회 실패", e)
            }
        }


        holder.binding.tvTitle.text = property.title
        holder.binding.tvPrice.text = "가격: ${property.price} 만원"
        holder.binding.tvPeriod.text = "기간: ${property.startDate} ~ ${property.endDate}"

        // ✨ 추천 라벨 표시
        holder.binding.tvRecommended.visibility = if (property.isRecommended) View.VISIBLE else View.GONE

//        if (property.ownerId == currentUserId) {
//            holder.binding.btnChat.visibility = View.GONE
//        } else {
//            holder.binding.btnChat.visibility = View.VISIBLE
//            holder.binding.btnChat.setOnClickListener {
//                val intent = Intent(holder.itemView.context, ChatActivity::class.java)
//                intent.putExtra("receiverId", property.ownerId)
//                holder.itemView.context.startActivity(intent)
//            }
//        }

        holder.itemView.setOnClickListener {
            onItemClick(property)
        }
    }


    override fun getItemCount(): Int = properties.size
}
