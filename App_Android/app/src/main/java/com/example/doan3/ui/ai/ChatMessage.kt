package com.example.doan3.ui.ai // Đổi package theo đúng chuẩn của bạn

data class ChatMessage(
    val text: String,
    val isUser: Boolean // true = Người dùng gửi, false = AI gửi
)