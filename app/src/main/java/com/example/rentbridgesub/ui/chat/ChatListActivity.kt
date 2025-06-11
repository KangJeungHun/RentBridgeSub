package com.example.rentbridgesub.ui.chat

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rentbridgesub.data.ChatListItem
import com.example.rentbridgesub.databinding.ActivityChatListBinding
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot

class ChatListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatListBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val chatUserList = mutableListOf<ChatListItem>() // (상대방UID, 이름)
    private lateinit var adapter: ChatListAdapter
    private var isSublessor: Boolean = false
    private var isSublessee: Boolean = false

    // ① 결과를 받아올 런처
    private val chatLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // 채팅방에서 메시지가 추가/읽음 처리 등 변경이 발생했을 때
            loadChatUsers()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val uid = auth.currentUser?.uid ?: return
        db.collection("Users").document(uid)
            .get()
            .addOnSuccessListener { document ->
                isSublessor = document.getString("userType") == "sublessor"
                isSublessee = document.getString("userType") == "sublessee"

                adapter = ChatListAdapter(chatUserList) { userId ->
                    val currentUserId = auth.currentUser?.uid ?: ""
                    Log.d("ChatListDebug", "currentUserId: $currentUserId, 상대방 uid: $userId")
                    val intent = Intent(this, ChatActivity::class.java)
                    intent.putExtra("receiverId", userId)
                    intent.putExtra("isSublessor", isSublessor)
                    intent.putExtra("isSublessee", isSublessee)
                    chatLauncher.launch(intent)
                }
                binding.recyclerViewChatList.layoutManager = LinearLayoutManager(this)
                binding.recyclerViewChatList.adapter = adapter

                loadChatUsers()

                setSupportActionBar(binding.toolbar)
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
            }
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
                        if (id1 == currentUserId && id2 != currentUserId) {
                            otherUserIds.add(id2)
                        } else if (id2 == currentUserId && id1 != currentUserId) {
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
                        val userMap = mutableMapOf<String, String>()

                        for (doc in usersSnapshot.documents) {
                            val uid = doc.getString("uid") ?: continue
                            val name = doc.getString("name") ?: "알 수 없음"
                            userMap[uid] = name
                        }
                        // 사용자마다 마지막 메시지를 가져오기
                        for ((uid, name) in userMap) {
                            val chatRoomId = if (uid < currentUserId) "$uid-$currentUserId" else "$currentUserId-$uid"

                            // 마지막 메시지 쿼리
                            val messageQuery = db.collection("ChatRooms")
                                .document(chatRoomId)
                                .collection("Messages")
                                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                                .limit(1)

                            // 안읽은 메시지 수 쿼리
                            val unreadQuery = db.collection("ChatRooms")
                                .document(chatRoomId)
                                .collection("Messages")
                                .whereEqualTo("receiverId", currentUserId)
                                .whereEqualTo("read", false)

                            Tasks.whenAllSuccess<QuerySnapshot>(messageQuery.get(), unreadQuery.get())
                                .addOnSuccessListener { results ->
                                    val msgSnap = results[0] as QuerySnapshot
                                    val unreadSnap = results[1] as QuerySnapshot

                                    val lastMessageDoc = msgSnap.documents.firstOrNull()
                                    val lastMessage = lastMessageDoc?.getString("message") ?: ""
                                    val fileName = lastMessageDoc?.getString("fileName") ?: ""
                                    val imageUrl = lastMessageDoc?.getString("imageUrl") ?: ""
                                    val timestamp = lastMessageDoc?.getLong("timestamp") ?: 0L
                                    val read = lastMessageDoc?.getBoolean("read") ?: true
                                    val unreadCount = unreadSnap.size()
                                    Log.d("FirestoreUnread", "Unread for $uid: $unreadCount")
                                    Log.d("FirestoreUnread", "Unread for $currentUserId: $unreadCount")

                                    chatUserList.add(
                                        ChatListItem(
                                            userId = uid,
                                            userName = name,
                                            lastMessage = lastMessage,
                                            fileName = fileName,
                                            imageUrl = imageUrl,
                                            timestamp = timestamp,
                                            read = read,
                                            unreadCount = unreadCount
                                        )
                                    )
                                    chatUserList.sortByDescending { it.timestamp }
                                    adapter.notifyDataSetChanged()
                                }
                        }
                    }
            }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> { finish(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
