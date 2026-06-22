package com.example.doan3.ui.vocabulary

import java.io.Serializable

// Định nghĩa cấu trúc của 1 từ vựng
data class Vocabulary(
    val word: String,
    val pronunciation: String,
    val meaning: String,
    val proficiency: Int, // Mức độ thông thạo (%)

    // THÊM CÁC TRƯỜNG DÀNH CHO TRANG CHI TIẾT TỪ VỰNG
    val type: String = "NOUN",
    val exampleEng: String = "The campaign is gaining momentum as more people join.",
    val exampleVi: String = "Chiến dịch đang lấy được đà khi có thêm nhiều người tham gia.",
    val imageUrl: String = "",
    val imageCaption: String = "Hình ảnh minh họa",
    val lastReviewTime: String = "22:45 • Hôm qua"
) : Serializable // Cần có Serializable để có thể gửi dữ liệu từ trang này sang trang kia