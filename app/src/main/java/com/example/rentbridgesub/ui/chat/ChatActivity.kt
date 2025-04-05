package com.example.rentbridgesub.ui.chat

import android.os.Bundle
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
                    val message = doc.toObject(com.example.rentbridgesub.data.Message::class.java)
                    // 🔥 Message -> ChatMessage 변환
                    val chatMessage = ChatMessage(
                        senderId = message.senderId,
                        receiverId = message.receiverId,
                        message = message.content,
                        timestamp = message.timestamp
                    )
                    messageList.add(chatMessage)
                }
                adapter.notifyDataSetChanged()
            }
    }

    private fun sendMessage() {
        val text = binding.etMessage.text.toString()
        val senderId = auth.currentUser?.uid ?: return

        if (text.isNotEmpty()) {
            val message = com.example.rentbridgesub.data.Message(
                senderId = senderId,
                receiverId = receiverId,
                content = text,
                timestamp = System.currentTimeMillis()
            )

            db.collection("ChatRooms").document(chatRoomId)
                .collection("Messages")
                .add(message)

            binding.etMessage.setText("")
        }
    }
}
