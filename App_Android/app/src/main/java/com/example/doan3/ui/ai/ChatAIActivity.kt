package com.example.doan3.ui.ai

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast // Thêm import Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doan3.R

// Thêm các import kết nối API từ module profile của bạn
import com.example.doan3.ui.profile.ChatRequest
import com.example.doan3.ui.profile.ChatResponse
import com.example.doan3.ui.profile.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChatAIActivity : AppCompatActivity() {

    private lateinit var rvChatMessages: RecyclerView
    private lateinit var edtChatInput: EditText
    private lateinit var btnSendChat: ImageView
    private lateinit var chatAdapter: ChatAdapter
    private val messageList = ArrayList<ChatMessage>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chat_ai)

        // 1. Ánh xạ các thành phần từ XML
        rvChatMessages = findViewById(R.id.rvChatMessages)
        edtChatInput = findViewById(R.id.edtChatInput)
        btnSendChat = findViewById(R.id.btnSendChat)
        findViewById<ImageView>(R.id.btnBackChat).setOnClickListener { finish() }

        val chatType = intent.getStringExtra("CHAT_TYPE") ?: "AI_ASSISTANT"
        setupTitleBasedOnType(chatType)

        // 2. Thiết lập RecyclerView và Adapter
        chatAdapter = ChatAdapter(messageList)
        rvChatMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        rvChatMessages.adapter = chatAdapter

        // 3. Lời chào khởi đầu mặc định từ AI
        sendWelcomeMessage(chatType)

        // 4. Xử lý sự kiện bấm nút Gửi tin nhắn thực tế qua API
        btnSendChat.setOnClickListener {
            sendMessageToAI()
        }
    }

    // Hàm gửi tin nhắn và kết nối API Node.js chính thức
    private fun sendMessageToAI() {
        val userText = edtChatInput.text.toString().trim()
        if (userText.isEmpty()) return

        // 1. Hiển thị tin nhắn của User lên màn hình ngay lập tức
        val userMessage = ChatMessage(text = userText, isUser = true)
        chatAdapter.addMessage(userMessage)

        // 2. Xóa ô nhập chữ và cuộn xuống cuối cùng
        edtChatInput.setText("")
        rvChatMessages.scrollToPosition(chatAdapter.itemCount - 1)

        // 3. Gọi API gửi lên Backend Node.js
        val request = ChatRequest(message = userText)
        RetrofitClient.getInstance(this).sendChatMessage(request)
            .enqueue(object : Callback<ChatResponse> {
                override fun onResponse(call: Call<ChatResponse>, response: Response<ChatResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        val aiReplyText = response.body()!!.reply

                        // 4. Nhận kết quả thực từ Node.js và hiển thị tin nhắn của AI lên màn hình
                        val aiMessage = ChatMessage(text = aiReplyText, isUser = false)
                        chatAdapter.addMessage(aiMessage)
                        rvChatMessages.smoothScrollToPosition(chatAdapter.itemCount - 1)
                    } else {
                        // Đọc thông báo lỗi từ server Node.js gửi về
                        val errorMessage = response.errorBody()?.string() ?: "Không rõ nguyên nhân"
                        Toast.makeText(this@ChatAIActivity, "Lỗi ${response.code()}: $errorMessage", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<ChatResponse>, t: Throwable) {
                    Toast.makeText(this@ChatAIActivity, "Mất kết nối máy chủ!", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun setupTitleBasedOnType(type: String) {
        val tvChatTitle = findViewById<TextView>(R.id.tvChatTitle)
        tvChatTitle.text = when(type) {
            "GIAI_THICH" -> "Trợ lý Giải thích Từ vựng"
            "TAO_QUIZ" -> "Trợ lý Thiết kế Bài tập"
            "LO_TRINH" -> "Trợ lý Lộ trình Học tập"
            "TRO_CHUYEN" -> "Trò Chuyện Cùng AI" // THÊM DÒNG NÀY
            else -> "Trợ lý AI LexiBook"
        }
    }

    private fun sendWelcomeMessage(type: String) {
        val welcomeText = when(type) {
            "GIAI_THICH" -> "Xin chào! Mình là chuyên gia ngôn ngữ AI. Hãy gửi từ vựng hoặc đoạn văn cần mình giải nghĩa, phân tích ngữ pháp nhé! 📚"
            "TAO_QUIZ" -> "Chào bạn! Bạn muốn làm bài test về chủ đề nào hôm nay? Hãy cho mình biết để mình tạo bộ câu hỏi trắc nghiệm riêng cho bạn! 🔥"
            "LO_TRINH" -> "Chào học viên! Bạn đang ở trình độ nào và mục tiêu TOEIC/IELTS là bao nhiêu? Hãy nói cho mình biết để mình lập lộ trình cá nhân hóa nhé! 🎯"
            "TRO_CHUYEN" -> "Chào bạn! Mình có thể trò chuyện và giúp bạn luyện tập tiếng Anh bất cứ lúc nào! 💬" // THÊM DÒNG NÀY
            else -> "Xin chào! Mình có thể giúp gì cho bạn trong việc học tiếng Anh hôm nay? 🤖"
        }
        chatAdapter.addMessage(ChatMessage(welcomeText, isUser = false))
    }
}