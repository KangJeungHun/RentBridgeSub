package com.example.rentbridgesub.ui.chat

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rentbridgesub.data.ChatMessage
import com.example.rentbridgesub.databinding.ActivityChatBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adapter: ChatAdapter
    private val messageList = mutableListOf<ChatMessage>()
    private lateinit var receiverId: String
    private lateinit var chatRoomId: String
    private lateinit var senderId: String
    private var isSublessee: Boolean = false
    private var isSublessor: Boolean = false
    private var contractSent = false

    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { uploadImage(it) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        receiverId = intent.getStringExtra("receiverId") ?: ""
        isSublessee = intent.getBooleanExtra("isSublessee", false)
        isSublessor = intent.getBooleanExtra("isSublessor", false)

        if (receiverId.isEmpty()) {
            Toast.makeText(this, "채팅 상대가 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        senderId = auth.currentUser?.uid ?: ""
        chatRoomId = if (senderId < receiverId) {
            "$senderId-$receiverId"
        } else {
            "$receiverId-$senderId"
        }

        adapter = ChatAdapter(messageList, senderId)
        binding.recyclerViewChat.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewChat.adapter = adapter

        binding.btnSend.setOnClickListener { sendMessage() }
        binding.btnImage.setOnClickListener { imagePicker.launch("image/*") }
        binding.btnSendContract.setOnClickListener { sendContractMessage() }

        binding.contractLayout.visibility = View.GONE
        Log.d("ChatCheck", "isSublessor: $isSublessor, isSublessee: $isSublessee")

        loadMessages()
    }

    private fun loadMessages() {
        db.collection("ChatRooms").document(chatRoomId)
            .collection("Messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, _ ->
                messageList.clear()
                contractSent = false

                snapshot?.forEach { doc ->
                    val message = doc.toObject(ChatMessage::class.java)
                    messageList.add(message)
                    if (message.message.contains("[계약서]")) {
                        contractSent = true
                    }
                }

                adapter.notifyDataSetChanged()
                binding.recyclerViewChat.scrollToPosition(messageList.size - 1)

                // 조건에 따라 버튼 보이기
                if (isSublessor && !contractSent) {
                    binding.contractLayout.visibility = View.VISIBLE
                    binding.btnSendContract.text = "계약서 보내기"
                } else if (isSublessee && contractSent) {
                    binding.contractLayout.visibility = View.VISIBLE
                    binding.btnSendContract.text = "계약서 회신"
                } else {
                    binding.contractLayout.visibility = View.GONE
                }
            }
    }

    private fun sendMessage() {
        val text = binding.etMessage.text.toString()
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
                db.collection("ChatRooms").document(chatRoomId)
                    .collection("Messages")
                    .add(message)
            }
            .addOnFailureListener {
                Toast.makeText(this, "이미지 전송 실패: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendContractMessage() {
        val contractRef = storage.reference.child("templates/contract.pdf")
        contractRef.downloadUrl
            .addOnSuccessListener { uri ->
                val message = ChatMessage(
                    senderId = senderId,
                    receiverId = receiverId,
                    message = "[계약서] 전대인과 전차인 간의 계약서를 확인해주세요.",
                    imageUrl = uri.toString(),
                    timestamp = System.currentTimeMillis()
                )
                db.collection("ChatRooms").document(chatRoomId)
                    .collection("Messages")
                    .add(message)
                Toast.makeText(this, "계약서가 전송되었습니다.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "계약서 전송 실패: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
