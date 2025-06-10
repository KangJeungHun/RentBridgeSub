package com.example.rentbridgesub.ui.chat

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
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

    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { uploadImage(it) }
        }

    // 1) 파일 선택용 launcher 추가
    private val filePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { uploadFile(it) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        senderId = auth.currentUser?.uid ?: ""

        receiverId = intent.getStringExtra("receiverId") ?: ""
        isSublessee = intent.getBooleanExtra("isSublessee", false)
        isSublessor = intent.getBooleanExtra("isSublessor", false)

        if (receiverId.isEmpty()) {
            Toast.makeText(this, "채팅 상대가 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        chatRoomId = if (senderId < receiverId) {
            "$senderId-$receiverId"
        } else {
            "$receiverId-$senderId"
        }

        Log.d("ChatDebug", "senderId: $senderId, receiverId: $receiverId")

        adapter = ChatAdapter(messageList, senderId)
        binding.recyclerViewChat.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewChat.adapter = adapter

        binding.btnSend.setOnClickListener { sendMessage() }
        binding.btnImage.setOnClickListener { imagePicker.launch("image/*") }
        // 2) 채팅 입력창 옆에 “파일” 버튼(OnClickListener)에 연결
        binding.btnFile.setOnClickListener { filePicker.launch("*/*") }

        Log.d("ChatCheck", "isSublessor: $isSublessor, isSublessee: $isSublessee")

        loadMessages()
    }

    private fun loadMessages() {
        db.collection("ChatRooms").document(chatRoomId)
            .collection("Messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, _ ->
                messageList.clear()

                snapshot?.forEach { doc ->
                    val message = doc.toObject(ChatMessage::class.java)

                    // 내가 받은 메시지이면서 아직 안 읽은 경우
                    if (message.receiverId == senderId && !message.read) {
                        doc.reference.update("read", true)
                        setResult(RESULT_OK)
                    }

                    messageList.add(message)
                }

                adapter.notifyDataSetChanged()
                binding.recyclerViewChat.scrollToPosition(messageList.size - 1)
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
                fileUrl = "",
                fileName = "",
                timestamp = System.currentTimeMillis(),
                read = false
            )
            val chatRoomRef = db.collection("ChatRooms").document(chatRoomId)
            chatRoomRef.set(mapOf("users" to listOf(senderId, receiverId)), SetOptions.merge())
            chatRoomRef.collection("Messages").add(message).addOnSuccessListener { setResult(RESULT_OK) }
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
                    fileUrl = "",
                    fileName = "",
                    timestamp = System.currentTimeMillis(),
                    read = false
                )
                db.collection("ChatRooms").document(chatRoomId)
                    .collection("Messages")
                    .add(message).addOnSuccessListener { setResult(RESULT_OK) }
            }
            .addOnFailureListener {
                Toast.makeText(this, "이미지 전송 실패: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // 3) 업로드 함수 (이미지 업로드와 거의 동일)
    private fun uploadFile(uri: Uri) {
        // 실제 디스플레이 이름 가져오기
        val cursor = contentResolver.query(uri, null, null, null, null)
        val nameIndex = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME) ?: -1
        cursor?.moveToFirst()
        val originalName = if (nameIndex >= 0) cursor!!.getString(nameIndex) else uri.lastPathSegment!!
        cursor?.close()

        val fileName = "chat_files/${System.currentTimeMillis()}_$originalName"
        val ref = storage.reference.child(fileName)

        ref.putFile(uri)
            .continueWithTask { task ->
                if (!task.isSuccessful) throw task.exception!!
                ref.downloadUrl
            }
            .addOnSuccessListener { downloadUri ->
                // 4) 채팅 메시지에 파일 URL과 함께 전송
                val message = ChatMessage(
                    senderId    = senderId,
                    receiverId  = receiverId,
                    message     = "[파일] ${uri.lastPathSegment}",
                    imageUrl    = "",               // 이미지 대신
                    fileUrl     = downloadUri.toString(),
                    fileName    = originalName,
                    timestamp   = System.currentTimeMillis(),
                    read = false
                )
                db.collection("ChatRooms")
                    .document(chatRoomId)
                    .collection("Messages")
                    .add(message).addOnSuccessListener { setResult(RESULT_OK) }
            }
            .addOnFailureListener {
                Toast.makeText(this, "파일 전송 실패: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
