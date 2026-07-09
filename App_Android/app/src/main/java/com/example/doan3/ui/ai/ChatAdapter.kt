package com.example.doan3.ui.ai // Sửa lại đúng package của bạn nếu cần

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.doan3.R

class ChatAdapter(private val messageList: ArrayList<ChatMessage>) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    // Lớp giữ các thành phần giao diện của một dòng tin nhắn
    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Hãy kiểm tra và chỉnh sửa lại các ID này cho khớp chính xác với ID bạn đặt trong chatmessage.xml nhé
        val layoutUser: View = itemView.findViewById(R.id.layoutUser)         // Khung chứa tin nhắn của User
        val tvUserMessage: TextView = itemView.findViewById(R.id.tvUserMessage) // Chữ tin nhắn của User

        val layoutAI: View = itemView.findViewById(R.id.layoutAI)             // Khung chứa tin nhắn của AI
        val tvAIMessage: TextView = itemView.findViewById(R.id.tvAIMessage)     // Chữ tin nhắn của AI
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        // Nạp file giao diện chatmessage.xml bạn vừa tạo vào RecyclerView
        val view = LayoutInflater.from(parent.context).inflate(R.layout.chatmessage, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messageList[position]

        if (message.isUser) {
            // Nếu là Người dùng gửi: Hiện khung chat của User, ẩn khung chat của AI
            holder.layoutUser.visibility = View.VISIBLE
            holder.layoutAI.visibility = View.GONE
            holder.tvUserMessage.text = message.text
        } else {
            // Nếu là AI gửi: Ẩn khung chat của User, hiện khung chat của AI
            holder.layoutUser.visibility = View.GONE
            holder.layoutAI.visibility = View.VISIBLE
            holder.tvAIMessage.text = message.text
        }
    }

    override fun getItemCount(): Int = messageList.size

    // Hàm tiện ích hỗ trợ thêm tin nhắn mới vào danh sách và tự động cập nhật UI
    fun addMessage(message: ChatMessage) {
        messageList.add(message)
        notifyItemInserted(messageList.size - 1)
    }
}