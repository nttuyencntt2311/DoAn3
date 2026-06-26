package com.example.doan3.ui.vocabulary

import java.io.Serializable

data class Vocabulary(
    val word: String,
    val pronunciation: String,
    val meaning: String, // Định nghĩa tiếng Việt
    val defEng: String,  // Định nghĩa tiếng Anh
    val proficiency: Int,
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
    val eng: String,
    val vi: String
) : Serializable