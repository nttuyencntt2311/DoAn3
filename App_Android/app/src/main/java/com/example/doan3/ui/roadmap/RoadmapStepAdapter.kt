package com.example.doan3.ui.roadmap

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.doan3.R
import com.example.doan3.databinding.ItemRoadmapStepBinding

class RoadmapStepAdapter : RecyclerView.Adapter<RoadmapStepAdapter.StepViewHolder>() {

    private var stepList = listOf<RoadmapStep>()
    // 1. Sửa lại khai báo biến và hàm setOnContinueClickListener
    private var onContinueClickListener: ((RoadmapStep, Int) -> Unit)? = null

    // 🚀 ĐÃ THÊM HÀM NÀY ĐỂ FRAGMENT CÓ THỂ GỌI ĐƯỢC
    fun setOnContinueClickListener(listener: (RoadmapStep, Int) -> Unit) {
        this.onContinueClickListener = listener
    }


    fun submitList(list: List<RoadmapStep>) {
        stepList = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StepViewHolder {
        val binding = ItemRoadmapStepBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StepViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StepViewHolder, position: Int) {
        val step = stepList[position]

        // Ẩn dây nối trên cùng nếu là item đầu tiên
        holder.binding.lineTop.visibility = if (position == 0) View.INVISIBLE else View.VISIBLE
        // Ẩn dây nối dưới cùng nếu là item cuối cùng
        holder.binding.lineBottom.visibility = if (position == stepList.size - 1) View.INVISIBLE else View.VISIBLE

        // Màu của dây nối (Line Top) phụ thuộc vào trạng thái của chặng TRƯỚC ĐÓ
        if (position > 0) {
            val previousStep = stepList[position - 1]
            if (previousStep.status == StepStatus.COMPLETED) {
                holder.binding.lineTop.setBackgroundColor(Color.parseColor("#059669")) // Dây xanh lá
            } else {
                holder.binding.lineTop.setBackgroundColor(Color.parseColor("#E2E8F0")) // Dây xám
            }
        }

        holder.bind(step)
    }

    override fun getItemCount(): Int = stepList.size

    inner class StepViewHolder(val binding: ItemRoadmapStepBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(step: RoadmapStep) {
            binding.tvStepTitle.text = step.title
            binding.tvStepDesc.text = if (!step.xaiReason.isNullOrBlank()) {
                "${step.description}\n\n🧠 Vì sao AI gợi ý chặng này: ${step.xaiReason}"
            } else {
                step.description
            }

            // Reset View
            binding.tvTag.visibility = View.GONE
            binding.tvProgressText.visibility = View.GONE
            binding.progressStep.visibility = View.GONE
            binding.btnContinueLearning.visibility = View.GONE

            when (step.status) {
                StepStatus.COMPLETED -> {
                    // Cấu hình UI chặng HOÀN THÀNH (Nền tảng)
                    binding.layoutIconOuter.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
                    binding.layoutIconInner.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#059669"))
                    binding.ivStepIcon.setImageResource(R.drawable.ic_check)

                    binding.cardContent.setCardBackgroundColor(Color.WHITE)
                    binding.cardContent.strokeColor = Color.parseColor("#E2E8F0")
                    binding.cardContent.strokeWidth = 2

                    binding.tvStepTitle.setTextColor(Color.parseColor("#0F172A"))
                    binding.tvStepDesc.setTextColor(Color.parseColor("#64748B"))

                    binding.tvTag.visibility = View.VISIBLE
                    binding.tvTag.text = "Đã hoàn thành ✓"
                    binding.tvTag.setTextColor(Color.parseColor("#059669"))
                    binding.tvTag.setBackgroundColor(Color.parseColor("#ECFDF5"))

                    binding.btnContinueLearning.visibility = View.VISIBLE
                    binding.btnContinueLearning.text = "Tiếp tục học ngay"
                    binding.btnContinueLearning.text = "Ôn lại chặng này"
                    binding.btnContinueLearning.setOnClickListener {
                        val questionCount = (position + 1) * 5
                        onContinueClickListener?.invoke(step, questionCount)
                    }
                }

                StepStatus.CURRENT -> {
                    // Cấu hình UI chặng HIỆN TẠI (Giao tiếp cơ bản)
                    binding.layoutIconOuter.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DBEAFE"))
                    binding.layoutIconInner.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#0A52CC"))
                    binding.ivStepIcon.setImageResource(R.drawable.ic_play)

                    binding.cardContent.setCardBackgroundColor(Color.WHITE)
                    binding.cardContent.strokeColor = Color.parseColor("#2563EB")
                    binding.cardContent.strokeWidth = 5

                    binding.tvStepTitle.setTextColor(Color.parseColor("#2563EB"))
                    binding.tvStepDesc.setTextColor(Color.parseColor("#64748B"))

                    binding.tvTag.visibility = View.VISIBLE
                    binding.tvTag.text = "HIỆN TẠI"
                    binding.tvTag.setTextColor(Color.parseColor("#1E40AF"))
                    binding.tvTag.setBackgroundColor(Color.parseColor("#DBEAFE"))

                    binding.tvProgressText.visibility = View.VISIBLE
                    binding.progressStep.visibility = View.VISIBLE
                    binding.btnContinueLearning.visibility = View.VISIBLE

                    binding.tvProgressText.text = "${step.currentProgress} /${step.maxProgress} từ"
                    binding.progressStep.max = step.maxProgress
                    binding.progressStep.progress = step.currentProgress

                    // 2. Tính toán số câu hỏi và truyền vào callback
                    binding.btnContinueLearning.setOnClickListener {
                        // Tính số lượng câu hỏi: Vị trí 0 -> 5 câu, Vị trí 1 -> 10 câu...
                        val questionCount = (position + 1) * 5

                        // Truyền cả step và questionCount đi
                        onContinueClickListener?.invoke(step, questionCount)
                    }
                }

                StepStatus.LOCKED -> {
                    // Cấu hình UI chặng KHOÁ (Thành thạo công việc)
                    binding.layoutIconOuter.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
                    binding.layoutIconInner.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E2E8F0"))
                    binding.ivStepIcon.setImageResource(R.drawable.ic_lock)

                    binding.cardContent.setCardBackgroundColor(Color.parseColor("#F8FAFC"))
                    binding.cardContent.strokeColor = Color.parseColor("#E2E8F0")
                    binding.cardContent.strokeWidth = 2

                    binding.tvStepTitle.setTextColor(Color.parseColor("#94A3B8"))
                    binding.tvStepDesc.setTextColor(Color.parseColor("#CBD5E1"))
                }
            }
        }
    }
}