package com.example.doan3.ui.vocabulary

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.doan3.R

class VocabularyAdapter(
    // Khai báo hành động khi người dùng bấm vào 1 dòng
    private val onItemClick: (Vocabulary) -> Unit
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

        // BẮT SỰ KIỆN CLICK VÀO TỪ VỰNG
        holder.itemView.setOnClickListener {
            onItemClick(vocab)
        }
    }

    override fun getItemCount(): Int = vocabularyList.size
}