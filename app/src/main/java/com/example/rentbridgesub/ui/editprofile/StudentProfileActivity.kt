package com.example.rentbridgesub.ui.editprofile

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.rentbridgesub.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class StudentProfileActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    fun isValidStudentEmail(email: String): Boolean {
        val domain = email.substringAfterLast("@")
        return domain.endsWith("ac.kr")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_profile)

        auth = FirebaseAuth.getInstance()
        val user = auth.currentUser ?: return

        // 1) 뷰 바인딩
        val tvProfileName  = findViewById<TextView>(R.id.tvProfileName)
        val tvProfileEmail = findViewById<TextView>(R.id.tvProfileEmail)
        val btnVerify = findViewById<Button>(R.id.btnVerifyStudent)

        // 2) 이메일 세팅
        tvProfileEmail.text = user.email

        // 3) Firestore에서 사용자 이름 읽어와 세팅
        db.collection("Users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("name") ?: "사용자"
                tvProfileName.text = name

                // 3) **여기**: 학생 인증 여부에 따라 뱃지 붙이기
                if (doc.getBoolean("isStudent") == true) {
                    tvProfileName.setCompoundDrawablesWithIntrinsicBounds(
                        0, 0, R.drawable.ic_badge_student, 0
                    )
                } else {
                    tvProfileName.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                }
            }
            .addOnFailureListener {
                tvProfileName.text = "사용자"
            }

        btnVerify.setOnClickListener {
            val user = auth.currentUser
            val email = user?.email ?: return@setOnClickListener

            // 1) 이메일 도메인 검사
            if (!isValidStudentEmail(email)) {
                Toast.makeText(this,
                    "`.ac.kr` 도메인 이메일만 인증 가능합니다.",
                    Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 2) 이미 인증된 상태인지 확인
            user.reload().addOnSuccessListener {
                if (user.isEmailVerified) {
                    onStudentVerified(user.uid)
                } else {
                    // 3) 인증 메일 재발송
                    user.sendEmailVerification()
                        .addOnSuccessListener {
                            Toast.makeText(this, "인증 메일을 보냈습니다.\n메일함에서 링크 클릭 후 다시 눌러주세요.", Toast.LENGTH_LONG).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "메일 발송 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        }
    }

    private fun onStudentVerified(uid: String) {
        // 4) Firestore에 학생 인증 표시
        db.collection("Users").document(uid)
            .update("isStudent", true)
            .addOnSuccessListener {
                Toast.makeText(this, "학생 인증이 완료되었습니다!", Toast.LENGTH_SHORT).show()
                // 버튼 비활성화
                findViewById<Button>(R.id.btnVerifyStudent).apply {
                    text = "학생 인증 완료"
                    isEnabled = false
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "학생 인증 저장 실패: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
