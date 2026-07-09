package com.example.doan3.ui.vocabulary

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Vocabulary(
    // 🚀 Bổ sung alternate: Bắt trọn ID dù Server trả về "_id" hay "id"
    @SerializedName(value = "_id", alternate = ["id"])
    val id: String = "", // Bỏ String? đi, đảm bảo ID luôn tồn tại để URL không bị lỗi

    @SerializedName("word")
    val word: String = "", // Thêm giá trị mặc định để tránh lỗi parse JSON

    @SerializedName("phonetic")
    val pronunciation: String = "",

    @SerializedName("meaning")
    val meaning: String = "",

    // --- XỬ LÝ LỌC TAB ---
    @SerializedName("category")
    val category: String = "Tất cả",

    val defEng: String = "",
    val proficiency: Int = 0,
    val type: String = "Noun",

    // Danh sách ví dụ (chứa tiếng Anh và tiếng Việt)
    val examples: List<ExampleSentence> = listOf(),

    // Đồng nghĩa & Trái nghĩa
    val synonyms: List<String> = listOf(),
    val antonyms: List<String> = listOf(),

    // AI Tip & Score
    val aiTip: String = "",
    val popularityScore: Int = 0,
    val popularityText: String = "Độ phổ biến: Cao",

    val imageUrl: String = "",
    val lastReviewTime: String = ""
) : Serializable

data class ExampleSentence(
    @SerializedName("eng")
    val eng: String = "",

    @SerializedName("vi")
    val vi: String = ""
) : Serializable

data class VocabStatsResponse(
    val totalWords: Int = 0,
    val masteryPercentage: Int = 0,
    val masteredWords: Int = 0,
    val learningWords: Int = 0,
    val newWords: Int = 0,
    val newWordsThisWeek: Int = 0
)