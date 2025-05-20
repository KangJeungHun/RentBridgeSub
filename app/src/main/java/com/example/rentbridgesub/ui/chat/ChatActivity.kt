package com.example.rentbridgesub.ui.chat

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rentbridgesub.data.ChatMessage
import com.example.rentbridgesub.databinding.ActivityChatBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.firestore.SetOptions



class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adapter: ChatAdapter
    private val messageList = mutableListOf<ChatMessage>()
    private lateinit var receiverId: String
    private lateinit var chatRoomId: String

    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { uploadImage(it) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        receiverId = intent.getStringExtra("receiverId") ?: ""

        if (receiverId.isEmpty()) {
            Toast.makeText(this, "채팅 상대가 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
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

        binding.btnImage.setOnClickListener {
            imagePicker.launch("image/*")
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
                imageUrl = "",
                timestamp = System.currentTimeMillis()
            )

            val chatRoomRef = db.collection("ChatRooms").document(chatRoomId)

            chatRoomRef.set(mapOf("users" to listOf(senderId, receiverId)), SetOptions.merge())
            chatRoomRef.collection("Messages").add(message)
            binding.etMessage.setText("")
        }
    }

    private fun uploadImage(uri: Uri) {
        val senderId = auth.currentUser?.uid ?: return
        val fileName = "chat_images/${System.currentTimeMillis()}_${uri.lastPathSegment}"
        val ref = storage.reference.child(fileName)

        ref.putFile(uri)
            .continueWithTask { ref.downloadUrl }
            .addOnSuccessListener { downloadUri ->
                val message = ChatMessage(
                    senderId = senderId,
                    receiverId = receiverId,
                    message = "",
                    imageUrl = downloadUri.toString(),
                    timestamp = System.currentTimeMillis()
                )

                val chatRoomRef = db.collection("ChatRooms").document(chatRoomId)
                chatRoomRef.collection("Messages").add(message)
            }
            .addOnFailureListener {
                Toast.makeText(this, "이미지 전송 실패: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
