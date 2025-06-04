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
    private val chatUserList = mutableListOf<Pair<String, String>>() // (상대방UID, 이름)
    private lateinit var adapter: ChatListAdapter
    private var isSublessor: Boolean = false
    private var isSublessee: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val uid = auth.currentUser?.uid ?: return
        db.collection("Users").document(uid)
            .get()
            .addOnSuccessListener { document ->
                isSublessor = document.getString("type") == "sublessor"
                isSublessee = document.getString("type") == "sublessee"
            }

        adapter = ChatListAdapter(chatUserList) { userId ->
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("receiverId", userId)
            intent.putExtra("isSublessor", isSublessor)
            intent.putExtra("isSublessee", isSublessee)
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
                val otherUserIds = mutableSetOf<String>()

                for (doc in snapshot.documents) {
                    val roomId = doc.id
                    val ids = roomId.split("-")
                    if (ids.size == 2) {
                        val (id1, id2) = ids
                        if (id1 == currentUserId) {
                            otherUserIds.add(id2)
                        } else if (id2 == currentUserId) {
                            otherUserIds.add(id1)
                        }
                    }
                }

                if (otherUserIds.isEmpty()) {
                    chatUserList.clear()
                    adapter.notifyDataSetChanged()
                    return@addOnSuccessListener
                }

                db.collection("Users")
                    .whereIn("uid", otherUserIds.toList())
                    .get()
                    .addOnSuccessListener { usersSnapshot ->
                        chatUserList.clear()
                        for (doc in usersSnapshot.documents) {
                            val uid = doc.getString("uid") ?: continue
                            val name = doc.getString("name") ?: "알 수 없음"
                            chatUserList.add(Pair(uid, name))
                        }
                        adapter.notifyDataSetChanged()
                    }
            }
    }
}
