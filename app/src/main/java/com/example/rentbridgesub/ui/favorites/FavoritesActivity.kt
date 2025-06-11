package com.example.rentbridgesub.ui.favorites

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rentbridgesub.data.Property
import com.example.rentbridgesub.databinding.ActivityFavoritesBinding
import com.example.rentbridgesub.ui.main.PropertyAdapter
import com.example.rentbridgesub.ui.main.PropertyDetailActivity
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

class FavoritesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFavoritesBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val favoritesList = mutableListOf<Property>()
    private lateinit var adapter: PropertyAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavoritesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // RecyclerView setup
        adapter = PropertyAdapter(favoritesList) { property ->
            Intent(this, PropertyDetailActivity::class.java).apply {
                putExtra("property", property)
                startActivity(this)
            }
        }
        binding.recyclerViewFavorites.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewFavorites.adapter = adapter

        loadFavorites()
    }

    private fun loadFavorites() {
        val uid = auth.currentUser?.uid ?: return
        val userRef = db.collection("Users").document(uid)

        // 1) favorites 필드 실시간 구독
        userRef.addSnapshotListener { docSnap, err ->
            if (err != null || docSnap == null) return@addSnapshotListener

            val rawFavs = docSnap.get("favorites") as? List<String> ?: emptyList()
            val favs = rawFavs.filter { it.isNotBlank() }
            if (favs.isEmpty()) {
                favoritesList.clear()
                adapter.notifyDataSetChanged()
                binding.recyclerViewFavorites.visibility = View.GONE
                binding.tvEmptyState.visibility        = View.VISIBLE
                return@addSnapshotListener
            }

            // 2) 10개씩 나눠서 whereIn 쿼리
            favoritesList.clear()
            val chunks = favs.chunked(10)
            val tasks = chunks.map { chunk ->
                db.collection("Properties")
                    .whereIn(FieldPath.documentId(), chunk)
                    .get()
            }

            // 3) 모든 쿼리 완료 후 리스트에 합치기
            Tasks.whenAllSuccess<QuerySnapshot>(tasks)
                .addOnSuccessListener { results ->
                    results.forEach { snap ->
                        snap.documents.mapNotNullTo(favoritesList) { it.toObject(Property::class.java) }
                    }
                    binding.recyclerViewFavorites.visibility = View.VISIBLE
                    binding.tvEmptyState.visibility        = View.GONE
                    adapter.notifyDataSetChanged()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "찜한 매물 불러오기 실패: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
