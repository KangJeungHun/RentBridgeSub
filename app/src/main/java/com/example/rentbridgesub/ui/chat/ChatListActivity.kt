package com.example.rentbridgesub.ui.chat

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rentbridgesub.databinding.ActivityChatListBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ChatListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatListBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val chatUserList = mutableListOf<Pair<String, String>>() // (userId, userName)
    private lateinit var adapter: ChatListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = ChatListAdapter(chatUserList) { userId ->
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("receiverId", userId)
            startActivity(intent)
        }

        binding.recyclerViewChatList.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewChatList.adapter = adapter

        loadChatUsers()
    }

    private fun loadChatUsers() {
        val currentUserId = auth.currentUser?.uid ?: return

        db.collection("ChatRooms")
            .get()
            .addOnSuccessListener { snapshot ->
                val userIds = mutableSetOf<String>()

                for (doc in snapshot.documents) {
                    val roomId = doc.id
                    val ids = roomId.split("-")
                    if (ids.contains(currentUserId)) {
                        val otherUserId = ids.first { it != currentUserId }
                        userIds.add(otherUserId)
                    }
                }

                if (userIds.isEmpty()) {
                    chatUserList.clear()
                    adapter.notifyDataSetChanged()
                } else {
                    fetchUserNames(userIds.toList())
                }
            }
            .addOnFailureListener {
                // 에러 처리 필요 시 여기에
            }
    }

    private fun fetchUserNames(userIds: List<String>) {
        chatUserList.clear()
        var completed = 0

        for (userId in userIds) {
            db.collection("Users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    val userName = document.getString("name") ?: "알 수 없음"
                    chatUserList.add(Pair(userId, userName))
                }
                .addOnCompleteListener {
                    completed++
                    if (completed == userIds.size) {
                        // 모든 사용자 정보 가져오기가 끝났을 때
                        chatUserList.sortBy { it.second } // 이름순 정렬(optional)
                        adapter.notifyDataSetChanged()
                    }
                }
        }
    }
}
