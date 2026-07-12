package com.example.doan3.ui.ai

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doan3.R

import com.example.doan3.ui.profile.ChatRequest
import com.example.doan3.ui.profile.ChatResponse
import com.example.doan3.ui.profile.CreateSessionRequest
import com.example.doan3.ui.profile.CreateSessionResponse
import com.example.doan3.ui.profile.ChatHistoryResponse
import com.example.doan3.ui.profile.UserSessionsResponse
import com.example.doan3.ui.profile.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.example.doan3.ui.profile.ChatSessionItem

class ChatAIActivity : AppCompatActivity() {

    // THÊM BIẾN NÀY: Lưu trữ tạm thời ID phòng chat trên RAM (Tắt app sẽ tự động mất)
    companion object {
        var activeSessionId: String? = null
    }

    private lateinit var rvChatMessages: RecyclerView
    private lateinit var edtChatInput: EditText
    private lateinit var btnSendMessage: View
    private lateinit var btnBack: ImageView
    private lateinit var btnHistoryChat: ImageView
    private lateinit var btnNewChat: ImageView

    private lateinit var chatAdapter: ChatAdapter
    private val messageList = ArrayList<ChatMessage>()

    private var currentSessionId: String? = null
    private var currentChatType: String = "AI_ASSISTANT"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chat_ai)

        // 1. ÁNH XẠ ID
        rvChatMessages = findViewById(R.id.rvChatMessages)
        edtChatInput = findViewById(R.id.edtChatInput)
        btnSendMessage = findViewById(R.id.btnSendMessage)
        btnBack = findViewById(R.id.btnBack)
        btnHistoryChat = findViewById(R.id.btnHistoryChat)
        btnNewChat = findViewById(R.id.btnNewChat)

        btnBack.setOnClickListener { finish() }

        // 2. NHẬN DỮ LIỆU TỪ INTENT VÀ BỘ NHỚ RAM
        val intentSessionId = intent.getStringExtra("SESSION_ID")
        currentChatType = intent.getStringExtra("CHAT_TYPE") ?: "AI_ASSISTANT"

        if (intentSessionId != null) {
            currentSessionId = intentSessionId
            // Lưu đè lại cái ID này thành phòng chat "đang dở" trên RAM
            activeSessionId = currentSessionId
        } else {
            // Lấy lại ID từ RAM (nếu app vẫn còn đang chạy nền)
            currentSessionId = activeSessionId
        }

        // 3. CÀI ĐẶT RECYCLERVIEW
        chatAdapter = ChatAdapter(messageList)
        rvChatMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        rvChatMessages.adapter = chatAdapter

        // 4. LOGIC KHỞI ĐỘNG
        if (currentSessionId != null) {
            loadChatHistory(currentSessionId!!)
        } else {
            sendWelcomeMessage(currentChatType)
        }

        // 5. CÁC SỰ KIỆN NÚT BẤM
        btnSendMessage.setOnClickListener {
            handleUserSendMessage()
        }

        btnNewChat.setOnClickListener {
            // Xóa session cũ trên màn hình
            currentSessionId = null

            // QUAN TRỌNG: Xóa ID phòng dở dang trong RAM
            activeSessionId = null

            messageList.clear()
            chatAdapter.notifyDataSetChanged()
            sendWelcomeMessage(currentChatType)
            Toast.makeText(this, "Đã bắt đầu đoạn chat mới!", Toast.LENGTH_SHORT).show()
        }

        btnHistoryChat.setOnClickListener {
            fetchAndShowHistory()
        }
    }

    // ==========================================
    // XỬ LÝ LỊCH SỬ CHAT VỚI BOTTOM SHEET
    // ==========================================
    private fun fetchAndShowHistory() {
        val sharedPrefs = getSharedPreferences("DoAn3Prefs", Context.MODE_PRIVATE)
        val userId = sharedPrefs.getString("USER_ID", "") ?: ""

        if (userId.isEmpty()) {
            Toast.makeText(this, "Chưa xác định được tài khoản!", Toast.LENGTH_SHORT).show()
            return
        }

        RetrofitClient.getInstance(this).getUserSessions(userId)
            .enqueue(object : Callback<UserSessionsResponse> {
                override fun onResponse(call: Call<UserSessionsResponse>, response: Response<UserSessionsResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        val sessions = response.body()!!.sessions
                        if (sessions.isEmpty()) {
                            Toast.makeText(this@ChatAIActivity, "Bạn chưa có lịch sử trò chuyện nào.", Toast.LENGTH_SHORT).show()
                            return
                        }

                        val bottomSheetDialog = BottomSheetDialog(this@ChatAIActivity)
                        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet_history, null)
                        bottomSheetDialog.setContentView(view)

                        val container = view.findViewById<LinearLayout>(R.id.llHistoryContainer)

                        for (session in sessions) {
                            val item = layoutInflater.inflate(R.layout.item_chat_session, null)
                            val tvTitle = item.findViewById<TextView>(R.id.tvSessionTitle)

                            tvTitle.text = session.title

                            item.setOnClickListener {
                                bottomSheetDialog.dismiss()

                                currentSessionId = session._id

                                // Lưu cache phòng đang nhắn dở vào RAM
                                activeSessionId = currentSessionId

                                messageList.clear()
                                chatAdapter.notifyDataSetChanged()
                                loadChatHistory(currentSessionId!!)
                            }

                            container.addView(item)
                        }

                        bottomSheetDialog.show()
                    }
                }
                override fun onFailure(call: Call<UserSessionsResponse>, t: Throwable) {
                    Toast.makeText(this@ChatAIActivity, "Lỗi kết nối máy chủ!", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // ==========================================
    // LOGIC NHẮN TIN VÀ GỌI API AI
    // ==========================================
    private fun handleUserSendMessage() {
        val userText = edtChatInput.text.toString().trim()
        if (userText.isEmpty()) return

        chatAdapter.addMessage(ChatMessage(text = userText, isUser = true))
        edtChatInput.setText("")
        rvChatMessages.scrollToPosition(chatAdapter.itemCount - 1)

        if (currentSessionId == null) {
            createSessionAndSendMessage(userText)
        } else {
            sendChatToBackend(userText, currentSessionId!!)
        }
    }

    private fun createSessionAndSendMessage(firstMessage: String) {
        val sharedPrefs = getSharedPreferences("DoAn3Prefs", Context.MODE_PRIVATE)
        val userId = sharedPrefs.getString("USER_ID", "unknown_user") ?: "unknown_user"
        val request = CreateSessionRequest(userId, currentChatType, firstMessage)

        RetrofitClient.getInstance(this).createChatSession(request)
            .enqueue(object : Callback<CreateSessionResponse> {
                override fun onResponse(call: Call<CreateSessionResponse>, response: Response<CreateSessionResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        currentSessionId = response.body()!!.sessionId

                        // QUAN TRỌNG: Lưu ngay cái phòng vừa tạo vào RAM
                        activeSessionId = currentSessionId

                        sendChatToBackend(firstMessage, currentSessionId!!)
                    } else {
                        Toast.makeText(this@ChatAIActivity, "Lỗi tạo phòng chat!", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<CreateSessionResponse>, t: Throwable) {
                    Toast.makeText(this@ChatAIActivity, "Mất kết nối mạng!", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun sendChatToBackend(userMessage: String, sessionId: String) {
        val request = ChatRequest(message = userMessage, sessionId = sessionId)

        RetrofitClient.getInstance(this).sendChatMessage(request)
            .enqueue(object : Callback<ChatResponse> {
                override fun onResponse(call: Call<ChatResponse>, response: Response<ChatResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        val aiReplyText = response.body()!!.reply
                        chatAdapter.addMessage(ChatMessage(text = aiReplyText, isUser = false))
                        rvChatMessages.smoothScrollToPosition(chatAdapter.itemCount - 1)
                    } else {
                        Toast.makeText(this@ChatAIActivity, "Lỗi từ AI!", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<ChatResponse>, t: Throwable) {
                    Toast.makeText(this@ChatAIActivity, "Mất kết nối máy chủ!", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun loadChatHistory(sessionId: String) {
        RetrofitClient.getInstance(this).getChatHistory(sessionId)
            .enqueue(object : Callback<ChatHistoryResponse> {
                override fun onResponse(call: Call<ChatHistoryResponse>, response: Response<ChatHistoryResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        val historyMessages = response.body()!!.messages
                        for (msg in historyMessages) {
                            val isUser = msg.role == "user"
                            chatAdapter.addMessage(ChatMessage(text = msg.text, isUser = isUser))
                        }
                        if (chatAdapter.itemCount > 0) {
                            rvChatMessages.scrollToPosition(chatAdapter.itemCount - 1)
                        }
                    }
                }
                override fun onFailure(call: Call<ChatHistoryResponse>, t: Throwable) {
                    Toast.makeText(this@ChatAIActivity, "Không thể tải lịch sử chat", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun sendWelcomeMessage(type: String) {
        val welcomeText = when(type) {
            "GIAI_THICH" -> "Xin chào! Mình là chuyên gia ngôn ngữ AI. Hãy gửi từ vựng hoặc đoạn văn cần mình giải nghĩa nhé! \uD83D\uDCDA"
            "TAO_QUIZ" -> "Chào bạn! Bạn muốn làm bài test về chủ đề nào hôm nay? \uD83D\uDD25"
            "LO_TRINH" -> "Chào học viên! Hãy nói cho mình biết mục tiêu TOEIC/IELTS để mình lập lộ trình cá nhân hóa nhé! \uD83C\uDFAF"
            "TRO_CHUYEN" -> "Chào bạn! Mình có thể trò chuyện và giúp bạn luyện tập tiếng Anh bất cứ lúc nào! \uD83D\uDCAC"
            else -> "Chào bạn! Tôi có thể giúp gì cho việc học tiếng Anh của bạn hôm nay?"
        }
        chatAdapter.addMessage(ChatMessage(welcomeText, isUser = false))
    }
}