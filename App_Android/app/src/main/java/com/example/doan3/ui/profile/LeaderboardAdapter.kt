package com.example.doan3.ui.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.doan3.R

// Data Class khớp với JSON backend trả về
data class LeaderboardUser(val _id: String, val name: String, val xp: Int, val level: Int)

// ĐÃ SỬA: Bổ sung thêm biến currentUserName vào tham số của class
class LeaderboardAdapter(
    private val list: List<LeaderboardUser>,
    private val currentUserName: String
) : RecyclerView.Adapter<LeaderboardAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRank: TextView = view.findViewById(R.id.tvRank)
        val tvPlayerName: TextView = view.findViewById(R.id.tvPlayerName)
        val tvPlayerLevel: TextView = view.findViewById(R.id.tvPlayerLevel)
        val tvPlayerXP: TextView = view.findViewById(R.id.tvPlayerXP)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_leaderboard, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = list[position]
        holder.tvRank.text = (position + 1).toString()
        holder.tvPlayerLevel.text = "Cấp độ ${user.level}"
        holder.tvPlayerXP.text = "${user.xp} XP"

        // Kiểm tra và bôi đậm nếu đúng là người đang đăng nhập
        if (user.name == currentUserName) {
            holder.itemView.setBackgroundColor(android.graphics.Color.parseColor("#EBF4FF")) // Nền xanh nhạt
            holder.tvPlayerName.text = "${user.name} (Bạn)" // Thêm chữ (Bạn)
            holder.tvPlayerName.setTextColor(android.graphics.Color.parseColor("#007BFF")) // Chữ xanh đậm
        } else {
            holder.itemView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            holder.tvPlayerName.text = user.name // ĐÃ SỬA: Gán lại tên gốc để tránh lỗi tái chế View của RecyclerView
            holder.tvPlayerName.setTextColor(android.graphics.Color.parseColor("#1E293B"))
        }

        // Đổi màu hạng cho Top 1, 2, 3
        when (position) {
            0 -> holder.tvRank.setTextColor(android.graphics.Color.parseColor("#F59E0B")) // Hạng 1: Vàng
            1 -> holder.tvRank.setTextColor(android.graphics.Color.parseColor("#94A3B8")) // Hạng 2: Bạc
            2 -> holder.tvRank.setTextColor(android.graphics.Color.parseColor("#B45309")) // Hạng 3: Đồng
            else -> holder.tvRank.setTextColor(android.graphics.Color.parseColor("#1E293B")) // Bình thường
        }
    }

    override fun getItemCount(): Int = list.size
}