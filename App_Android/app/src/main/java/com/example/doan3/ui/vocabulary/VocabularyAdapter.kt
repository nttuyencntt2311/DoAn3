package com.example.doan3.ui.vocabulary

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView // THÊM IMPORT NÀY CHO NÚT LOA
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.doan3.R

class VocabularyAdapter(
    private val onItemClick: (Vocabulary) -> Unit,
    private val onSpeakClick: (String) -> Unit // Nhận lệnh phát âm từ Fragment
) : RecyclerView.Adapter<VocabularyAdapter.VocabularyViewHolder>() {

    private var vocabularyList: List<Vocabulary> = listOf()

    fun updateData(newList: List<Vocabulary>) {
        this.vocabularyList = newList
        notifyDataSetChanged()
    }

    class VocabularyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvWord: TextView = itemView.findViewById(R.id.tvWord)
        val tvPronunciation: TextView = itemView.findViewById(R.id.tvPronunciation)
        val tvMeaning: TextView = itemView.findViewById(R.id.tvMeaning)
        val tvPercent: TextView = itemView.findViewById(R.id.tvPercent)
        val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)

        // 1. MỚI THÊM: Ánh xạ nút Loa (Nhớ đảm bảo ID trong file item_vocabulary.xml của em là btnSpeaker nhé)
        val btnSpeaker: ImageView = itemView.findViewById(R.id.btnSpeaker)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VocabularyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_vocabulary, parent, false)
        return VocabularyViewHolder(view)
    }

    override fun onBindViewHolder(holder: VocabularyViewHolder, position: Int) {
        val vocab = vocabularyList[position]

        holder.tvWord.text = vocab.word
        holder.tvPronunciation.text = vocab.pronunciation
        holder.tvMeaning.text = vocab.meaning
        holder.tvPercent.text = "${vocab.proficiency}%"
        holder.progressBar.progress = vocab.proficiency

        // BẮT SỰ KIỆN CLICK VÀO CẢ DÒNG TỪ VỰNG ĐỂ XEM CHI TIẾT
        holder.itemView.setOnClickListener {
            onItemClick(vocab)
        }

        // 2. MỚI THÊM: BẮT SỰ KIỆN CLICK VÀO NÚT LOA ĐỂ PHÁT ÂM
        holder.btnSpeaker.setOnClickListener {
            onSpeakClick(vocab.word)
        }
    }

    override fun getItemCount(): Int = vocabularyList.size
}