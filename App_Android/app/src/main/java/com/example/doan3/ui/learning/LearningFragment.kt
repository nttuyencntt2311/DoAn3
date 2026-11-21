package com.example.doan3.ui.learning

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doan3.MainActivity
import com.example.doan3.R
import com.example.doan3.databinding.FragmentLearningBinding
import com.example.doan3.databinding.ItemCategoryCardBinding
import com.example.doan3.ui.profile.CustomCategoriesResponse
import com.example.doan3.ui.profile.KhamPhaActivity
import com.example.doan3.ui.profile.RetrofitClient
import com.example.doan3.ui.profile.TaoQuizActivity
import com.example.doan3.ui.roadmap.IeltsQuizActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

data class CategoryTopic(val name: String, val iconResId: Int)

class LearningFragment : Fragment() {

    private var _binding: FragmentLearningBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLearningBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnMenu.setOnClickListener {
            (activity as? MainActivity)?.openVocabularyScreen()
        }

        val topics = listOf(
            CategoryTopic("Hợp đồng & Kinh doanh", R.drawable.ic_business),
            CategoryTopic("Môi trường văn phòng", R.drawable.ic_office),
            CategoryTopic("Hệ thống thông tin & Công nghệ", R.drawable.ic_computer),
            CategoryTopic("Kế toán & Tài chính", R.drawable.ic_finance),
            CategoryTopic("Dịch vụ & Khách hàng", R.drawable.ic_customer)
        )

        val adapter = CategoryAdapter(topics) { selectedCategoryName ->
            val intent = Intent(requireContext(), KhamPhaActivity::class.java)
            intent.putExtra("CATEGORY_NAME", selectedCategoryName)
            startActivity(intent)
        }

        binding.rvLessons.layoutManager = LinearLayoutManager(requireContext())
        binding.rvLessons.adapter = adapter

        binding.rvCustomQuizzes.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        loadCustomQuizzes()
    }

    override fun onResume() {
        super.onResume()
        loadCustomQuizzes()
    }

    private fun loadCustomQuizzes() {
        val ctx = context ?: return
        RetrofitClient.getInstance(ctx).getCustomQuizCategories()
            .enqueue(object : Callback<CustomCategoriesResponse> {
                override fun onResponse(
                    call: Call<CustomCategoriesResponse>,
                    response: Response<CustomCategoriesResponse>
                ) {
                    if (_binding == null || !isAdded) return
                    val categories = if (response.isSuccessful) {
                        response.body()?.categories ?: emptyList()
                    } else {
                        emptyList()
                    }
                    bindCustomQuizList(categories)
                }

                override fun onFailure(call: Call<CustomCategoriesResponse>, t: Throwable) {
                    if (_binding == null || !isAdded) return
                    bindCustomQuizList(emptyList())
                }
            })
    }

    private fun bindCustomQuizList(categories: List<String>) {
        if (categories.isEmpty()) {
            val createPrompt = listOf(CategoryTopic("+ Tạo bộ Quiz AI mới", R.drawable.ic_lexi_quiz))
            binding.rvCustomQuizzes.adapter = CategoryAdapter(createPrompt) {
                startActivity(Intent(requireContext(), TaoQuizActivity::class.java))
            }
        } else {
            val items = categories.map { CategoryTopic(it, R.drawable.ic_lexi_quiz) }
            binding.rvCustomQuizzes.adapter = CategoryAdapter(items) { categoryName ->
                val intent = Intent(requireContext(), IeltsQuizActivity::class.java).apply {
                    putExtra("STEP_TITLE", categoryName)
                    putExtra("STEP_DESC", "Bộ Quiz tự tạo: $categoryName")
                    putExtra("QUESTION_COUNT", 10)
                }
                startActivity(intent)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class CategoryAdapter(
        private val categoryList: List<CategoryTopic>,
        private val onItemClick: (String) -> Unit
    ) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

        inner class CategoryViewHolder(val itemBinding: ItemCategoryCardBinding) :
            RecyclerView.ViewHolder(itemBinding.root) {
            fun bind(category: CategoryTopic) {
                itemBinding.tvCategoryName.text = category.name
                itemBinding.imgCategoryIcon.setImageResource(category.iconResId)
                itemBinding.root.setOnClickListener {
                    onItemClick(category.name)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
            val itemBinding = ItemCategoryCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return CategoryViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
            holder.bind(categoryList[position])
        }

        override fun getItemCount(): Int = categoryList.size
    }
}