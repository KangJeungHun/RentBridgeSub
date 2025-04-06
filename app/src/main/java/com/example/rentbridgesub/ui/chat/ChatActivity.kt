package com.example.rentbridgesub.ui.chat

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rentbridgesub.data.ChatMessage
import com.example.rentbridgesub.databinding.ActivityChatBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adapter: ChatAdapter
    private val messageList = mutableListOf<ChatMessage>()
    private lateinit var receiverId: String
    private lateinit var chatRoomId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        receiverId = intent.getStringExtra("receiverId") ?: ""

        if (receiverId.isEmpty()) {
            Toast.makeText(this, "채팅 상대가 없습니다. (receiverId 없음)", Toast.LENGTH_SHORT).show()
            finish()
            return
        } else {
            Toast.makeText(this, "채팅 상대 UID: $receiverId", Toast.LENGTH_SHORT).show()
        }

        val senderId = auth.currentUser?.uid ?: ""
        chatRoomId = if (senderId < receiverId) {
            "$senderId-$receiverId"
        } else {
            "$receiverId-$senderId"
        }

        adapter = ChatAdapter(messageList, senderId)
        binding.recyclerViewChat.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewChat.adapter = adapter

        loadMessages()

        binding.btnSend.setOnClickListener {
            sendMessage()
        }
    }

    private fun loadMessages() {
        db.collection("ChatRooms").document(chatRoomId)
            .collection("Messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, _ ->
                messageList.clear()
                snapshot?.forEach { doc ->
                    val message = doc.toObject(ChatMessage::class.java)
                    messageList.add(message)
                }
                adapter.notifyDataSetChanged()
                binding.recyclerViewChat.scrollToPosition(messageList.size - 1)
            }
    }

    private fun sendMessage() {
        val text = binding.etMessage.text.toString()
        val senderId = auth.currentUser?.uid ?: return

        if (text.isNotEmpty()) {
            val message = ChatMessage(
                senderId = senderId,
                receiverId = receiverId,
                message = text,
                timestamp = System.currentTimeMillis()
            )

            val chatRoomRef = db.collection("ChatRooms").document(chatRoomId)

            // 1. ChatRoom 문서 생성 (없으면)
            chatRoomRef.get().addOnSuccessListener { document ->
                if (!document.exists()) {
                    val chatRoomData = hashMapOf(
                        "users" to listOf(senderId, receiverId),
                        "lastMessage" to text,
                        "timestamp" to System.currentTimeMillis()
                    )
                    chatRoomRef.set(chatRoomData)
                } else {
                    // 이미 존재하면 lastMessage 갱신
                    chatRoomRef.update(
                        mapOf(
                            "lastMessage" to text,
                            "timestamp" to System.currentTimeMillis()
                        )
                    )
                }

                // 2. Messages 하위 컬렉션에 메시지 저장
                chatRoomRef.collection("Messages")
                    .add(message)
                    .addOnSuccessListener {
                        binding.etMessage.setText("")
                    }
            }
        }
    }


}
