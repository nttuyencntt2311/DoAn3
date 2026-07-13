package com.example.doan3.ui.ai // Sửa lại đúng package của bạn nếu cần

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.doan3.R
import io.noties.markwon.Markwon

class ChatAdapter(private val messageList: ArrayList<ChatMessage>) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    // Lớp giữ các thành phần giao diện của một dòng tin nhắn
    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val layoutUser: View = itemView.findViewById(R.id.layoutUser)
        val tvUserMessage: TextView = itemView.findViewById(R.id.tvUserMessage)

        val layoutAI: View = itemView.findViewById(R.id.layoutAI)
        val tvAIMessage: TextView = itemView.findViewById(R.id.tvAIMessage)

        // 1. Khởi tạo Markwon ở đây, lấy context từ itemView
        val markwon = Markwon.create(itemView.context)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.chatmessage, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messageList[position]

        if (message.isUser) {
            // Nếu là Người dùng gửi
            holder.layoutUser.visibility = View.VISIBLE
            holder.layoutAI.visibility = View.GONE
            holder.tvUserMessage.text = message.text
        } else {
            // Nếu là AI gửi
            holder.layoutUser.visibility = View.GONE
            holder.layoutAI.visibility = View.VISIBLE

            // 2. Sử dụng Markwon để hiển thị chữ có định dạng Markdown cho AI
            holder.markwon.setMarkdown(holder.tvAIMessage, message.text)
        }
    }

    override fun getItemCount(): Int = messageList.size

    fun addMessage(message: ChatMessage) {
        messageList.add(message)
        notifyItemInserted(messageList.size - 1)
    }
}