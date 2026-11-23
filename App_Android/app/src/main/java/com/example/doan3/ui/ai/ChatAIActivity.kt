package com.example.doan3.ui.ai

import android.content.Context
import android.content.Intent
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
import com.example.doan3.MainActivity
import com.example.doan3.R
import com.example.doan3.ui.profile.ChatHistoryResponse
import com.example.doan3.ui.profile.ChatRequest
import com.example.doan3.ui.profile.ChatResponse
import com.example.doan3.ui.profile.CreateSessionRequest
import com.example.doan3.ui.profile.CreateSessionResponse
import com.example.doan3.ui.profile.RetrofitClient
import com.example.doan3.ui.profile.UserSessionsResponse
import com.google.android.material.bottomsheet.BottomSheetDialog
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChatAIActivity : AppCompatActivity() {

    companion object {
        var activeSessionId: String? = null
        var activeSessionUserId: String? = null

        fun clearActiveSession() {
            activeSessionId = null
            activeSessionUserId = null
        }
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
    private var isSending = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chat_ai)

        rvChatMessages = findViewById(R.id.rvChatMessages)
        edtChatInput = findViewById(R.id.edtChatInput)
        btnSendMessage = findViewById(R.id.btnSendMessage)
        btnBack = findViewById(R.id.btnBack)
        btnHistoryChat = findViewById(R.id.btnHistoryChat)
        btnNewChat = findViewById(R.id.btnNewChat)

        btnBack.setOnClickListener { finish() }
        setupBottomNavigation()

        // Cô lập activeSessionId theo userId để không lộ phòng chat giữa các tài khoản
        val sharedPrefs = getSharedPreferences("DoAn3Prefs", Context.MODE_PRIVATE)
        val currentUserId = sharedPrefs.getString("USER_ID", "") ?: ""
        if (activeSessionUserId != currentUserId) {
            clearActiveSession()
            activeSessionUserId = currentUserId
        }

        val intentSessionId = intent.getStringExtra("SESSION_ID")
        val initialMessage = intent.getStringExtra("INITIAL_MESSAGE")

        if (intentSessionId != null) {
            currentSessionId = intentSessionId
            activeSessionId = currentSessionId
            activeSessionUserId = currentUserId
        } else if (initialMessage.isNullOrBlank()) {
            currentSessionId = activeSessionId
        } else {
            // Câu hỏi nhanh từ TroLyAIFragment -> bắt đầu đoạn chat mới
            currentSessionId = null
            activeSessionId = null
        }

        chatAdapter = ChatAdapter(messageList)
        rvChatMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        rvChatMessages.adapter = chatAdapter

        if (currentSessionId != null) {
            loadChatHistory(currentSessionId!!)
        } else {
            sendWelcomeMessage()
        }

        btnSendMessage.setOnClickListener {
            handleUserSendMessage()
        }

        btnNewChat.setOnClickListener {
            currentSessionId = null
            activeSessionId = null
            messageList.clear()
            chatAdapter.notifyDataSetChanged()
            sendWelcomeMessage()
            Toast.makeText(this, "Đã bắt đầu đoạn chat mới!", Toast.LENGTH_SHORT).show()
        }

        btnHistoryChat.setOnClickListener {
            fetchAndShowHistory()
        }

        if (!initialMessage.isNullOrBlank()) {
            edtChatInput.setText(initialMessage)
            handleUserSendMessage()
        }
    }

    private fun navigateToMainTab(tabName: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("OPEN_FRAGMENT", tabName)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        overridePendingTransition(0, 0)
        finish()
    }

    private fun setupBottomNavigation() {
        findViewById<View>(R.id.btnTrangChu)?.setOnClickListener { navigateToMainTab("HOME") }
        findViewById<View>(R.id.btnKhamPha)?.setOnClickListener { navigateToMainTab("LEARNING") }
        findViewById<View>(R.id.btnTroLyAI)?.setOnClickListener { navigateToMainTab("AI_ASSISTANT") }
        findViewById<View>(R.id.btnHoSo)?.setOnClickListener { navigateToMainTab("PROFILE") }
        findViewById<View>(R.id.btnTuVung)?.setOnClickListener { navigateToMainTab("VOCABULARY") }
    }

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
                    if (isFinishing || isDestroyed) return
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
                            val item = layoutInflater.inflate(R.layout.item_chat_session, container, false)
                            val tvTitle = item.findViewById<TextView>(R.id.tvSessionTitle)
                            tvTitle.text = session.title

                            item.setOnClickListener {
                                bottomSheetDialog.dismiss()
                                currentSessionId = session._id
                                activeSessionId = currentSessionId
                                activeSessionUserId = userId
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
                    if (isFinishing || isDestroyed) return
                    Toast.makeText(this@ChatAIActivity, "Lỗi kết nối máy chủ!", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun setSendingState(sending: Boolean) {
        isSending = sending
        btnSendMessage.isEnabled = !sending
        btnSendMessage.alpha = if (sending) 0.5f else 1.0f
    }

    private fun handleUserSendMessage() {
        if (isSending) return
        val userText = edtChatInput.text.toString().trim()
        if (userText.isEmpty()) return

        setSendingState(true)
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
        val request = CreateSessionRequest(userId, "AI_ASSISTANT", firstMessage)

        RetrofitClient.getInstance(this).createChatSession(request)
            .enqueue(object : Callback<CreateSessionResponse> {
                override fun onResponse(call: Call<CreateSessionResponse>, response: Response<CreateSessionResponse>) {
                    if (isFinishing || isDestroyed) return
                    if (response.isSuccessful && response.body() != null) {
                        currentSessionId = response.body()!!.sessionId
                        activeSessionId = currentSessionId
                        activeSessionUserId = userId
                        sendChatToBackend(firstMessage, currentSessionId!!)
                    } else {
                        setSendingState(false)
                        Toast.makeText(this@ChatAIActivity, "Lỗi tạo phòng chat!", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<CreateSessionResponse>, t: Throwable) {
                    if (isFinishing || isDestroyed) return
                    setSendingState(false)
                    Toast.makeText(this@ChatAIActivity, "Mất kết nối mạng!", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun sendChatToBackend(userMessage: String, sessionId: String) {
        val request = ChatRequest(message = userMessage, sessionId = sessionId)

        RetrofitClient.getInstance(this).sendChatMessage(request)
            .enqueue(object : Callback<ChatResponse> {
                override fun onResponse(call: Call<ChatResponse>, response: Response<ChatResponse>) {
                    setSendingState(false)
                    if (isFinishing || isDestroyed) return
                    if (response.isSuccessful && response.body() != null) {
                        val aiReplyText = response.body()!!.reply
                        chatAdapter.addMessage(ChatMessage(text = aiReplyText, isUser = false))
                        rvChatMessages.smoothScrollToPosition(chatAdapter.itemCount - 1)
                    } else {
                        Toast.makeText(this@ChatAIActivity, "Lỗi từ AI!", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ChatResponse>, t: Throwable) {
                    setSendingState(false)
                    if (isFinishing || isDestroyed) return
                    Toast.makeText(this@ChatAIActivity, "Mất kết nối máy chủ!", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun loadChatHistory(sessionId: String) {
        RetrofitClient.getInstance(this).getChatHistory(sessionId)
            .enqueue(object : Callback<ChatHistoryResponse> {
                override fun onResponse(call: Call<ChatHistoryResponse>, response: Response<ChatHistoryResponse>) {
                    if (isFinishing || isDestroyed) return
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
                    if (isFinishing || isDestroyed) return
                    Toast.makeText(this@ChatAIActivity, "Không thể tải lịch sử chat", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun sendWelcomeMessage() {
        val welcomeText = "Chào bạn! Mình có thể trò chuyện và giúp bạn luyện tập tiếng Anh bất cứ lúc nào! \uD83D\uDCAC"
        chatAdapter.addMessage(ChatMessage(welcomeText, isUser = false))
    }
}