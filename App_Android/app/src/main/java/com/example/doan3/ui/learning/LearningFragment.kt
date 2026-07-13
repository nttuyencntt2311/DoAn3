package com.example.doan3.ui.learning

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doan3.R // QUAN TRỌNG: Thêm dòng này để gọi được các file icon
import com.example.doan3.databinding.FragmentLearningBinding
import com.example.doan3.databinding.ItemCategoryCardBinding
import com.example.doan3.ui.profile.KhamPhaActivity

// 1. ĐÃ SỬA: Đổi wordCount thành iconResId để chứa Icon
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

        // 2. ĐÃ SỬA: Thay các số 100 bằng đường dẫn đến 5 file Icon
        val topics = listOf(
            CategoryTopic("Hợp đồng & Kinh doanh", R.drawable.ic_business),
            CategoryTopic("Môi trường văn phòng", R.drawable.ic_office),
            CategoryTopic("Hệ thống thông tin & Công nghệ", R.drawable.ic_computer),
            CategoryTopic("Kế toán & Tài chính", R.drawable.ic_finance),
            CategoryTopic("Dịch vụ & Khách hàng", R.drawable.ic_customer)
        )

        // Khởi tạo Adapter và hứng sự kiện Click
        val adapter = CategoryAdapter(topics) { selectedCategoryName ->
            val intent = Intent(requireContext(), KhamPhaActivity::class.java)
            intent.putExtra("CATEGORY_NAME", selectedCategoryName)
            startActivity(intent)
        }

        // Gắn Adapter vào RecyclerView
        binding.rvLessons.layoutManager = LinearLayoutManager(requireContext())
        binding.rvLessons.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ---------------------------------------------------------
    // Adapter nội bộ để hiển thị danh sách Chủ Đề
    // ---------------------------------------------------------
    inner class CategoryAdapter(
        private val categoryList: List<CategoryTopic>,
        private val onItemClick: (String) -> Unit
    ) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

        inner class CategoryViewHolder(val itemBinding: ItemCategoryCardBinding) : RecyclerView.ViewHolder(itemBinding.root) {
            fun bind(category: CategoryTopic) {
                // Đổ dữ liệu text
                itemBinding.tvCategoryName.text = category.name

                // 3. ĐÃ SỬA: Lệnh này sẽ lấy Icon nhét vào giao diện
                itemBinding.imgCategoryIcon.setImageResource(category.iconResId)

                // Bắt sự kiện click trên toàn bộ Thẻ (Card)
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