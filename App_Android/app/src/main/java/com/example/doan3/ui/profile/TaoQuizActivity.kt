package com.example.doan3.ui.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.doan3.MainActivity
import com.example.doan3.R
import com.example.doan3.databinding.ActivityTaoQuizBinding
import com.example.doan3.ui.roadmap.IeltsQuizActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.BufferedReader
import java.io.InputStreamReader

class TaoQuizActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaoQuizBinding

    private var selectedQuantity = 10
    private var selectedDifficulty = "Trung bình"

    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val fileContent = readTextFromUri(it).take(5000)
            binding.edtContent.setText(fileContent)
            Toast.makeText(this, "Đã tải dữ liệu từ tệp lên!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaoQuizBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            finish()
        }

        // Mở danh sách các bộ Quiz tự tạo trong mục Bài học
        binding.btnHistory.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("OPEN_FRAGMENT", "LEARNING")
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }

        binding.edtContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val length = s?.length ?: 0
                binding.tvCharCount.text = "$length / 5000 ký tự"
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnUpload.setOnClickListener {
            pickFileLauncher.launch("text/plain")
        }

        binding.sbDifficulty.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                when (progress) {
                    0 -> {
                        selectedDifficulty = "Dễ"
                        binding.tvDifficultyLabel.text = "Dễ"
                        binding.tvDifficultyLabel.setBackgroundColor(ContextCompat.getColor(this@TaoQuizActivity, R.color.primary_blue))
                    }
                    1 -> {
                        selectedDifficulty = "Trung bình"
                        binding.tvDifficultyLabel.text = "Trung bình"
                        binding.tvDifficultyLabel.setBackgroundColor(ContextCompat.getColor(this@TaoQuizActivity, R.color.orange_medium))
                    }
                    2 -> {
                        selectedDifficulty = "Khó"
                        binding.tvDifficultyLabel.text = "Khó"
                        binding.tvDifficultyLabel.setBackgroundColor(ContextCompat.getColor(this@TaoQuizActivity, R.color.text_main))
                    }
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.btnQty5.setOnClickListener { updateQuantitySelection(5) }
        binding.btnQty10.setOnClickListener { updateQuantitySelection(10) }
        binding.btnQty20.setOnClickListener { updateQuantitySelection(20) }

        binding.btnTaoQuiz.setOnClickListener {
            val content = binding.edtContent.text.toString().trim()
            if (content.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập nội dung hoặc tải tài liệu lên nhé!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (content.length > 5000) {
                Toast.makeText(this, "Nội dung tối đa 5000 ký tự!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val questionType = when (binding.rgQuestionType.checkedRadioButtonId) {
                R.id.rbTracNghiem -> "Trắc nghiệm"
                R.id.rbDungSai -> "Đúng / Sai"
                R.id.rbDienKhuyet -> "Điền vào chỗ trống"
                else -> "Trắc nghiệm"
            }

            generateQuizWithAI(content, questionType, selectedDifficulty, selectedQuantity)
        }
    }

    private fun readTextFromUri(uri: Uri): String {
        val stringBuilder = StringBuilder()
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        stringBuilder.append(line).append("\n")
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Không thể đọc tệp này!", Toast.LENGTH_SHORT).show()
        }
        return stringBuilder.toString()
    }

    private fun generateQuizWithAI(content: String, type: String, difficulty: String, quantity: Int) {
        binding.btnTaoQuiz.isEnabled = false
        binding.btnTaoQuiz.text = "AI đang xử lý..."

        val enrichedContent = "[$type | Độ khó: $difficulty] $content"
        val request = GenerateCustomQuizRequest(content = enrichedContent, questionCount = quantity)

        RetrofitClient.getInstance(this).generateCustomQuiz(request)
            .enqueue(object : Callback<GenerateCustomQuizResponse> {
                override fun onResponse(
                    call: Call<GenerateCustomQuizResponse>,
                    response: Response<GenerateCustomQuizResponse>
                ) {
                    if (isFinishing || isDestroyed) return
                    binding.btnTaoQuiz.isEnabled = true
                    binding.btnTaoQuiz.text = "Tạo Quiz ngay"

                    if (response.isSuccessful && response.body() != null) {
                        val categoryName = response.body()!!.categoryName
                        Toast.makeText(
                            this@TaoQuizActivity,
                            "Tạo bộ Quiz AI thành công! Bắt đầu làm bài...",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Chuyển thẳng sang màn hình làm bài Quiz thay vì Flashcard
                        val difficultyLevel = when (difficulty) {
                            "Dễ" -> 1
                            "Khó" -> 5
                            else -> 3
                        }
                        val intent = Intent(this@TaoQuizActivity, IeltsQuizActivity::class.java).apply {
                            putExtra("STEP_TITLE", categoryName)
                            putExtra("STEP_DESC", "[$type | Độ khó: $difficulty] ${content.take(300)}")
                            putExtra("QUESTION_COUNT", quantity)
                            putExtra("DIFFICULTY_LEVEL", difficultyLevel)
                        }
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(
                            this@TaoQuizActivity,
                            "Lỗi từ Server (${response.code()})",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<GenerateCustomQuizResponse>, t: Throwable) {
                    if (isFinishing || isDestroyed) return
                    binding.btnTaoQuiz.isEnabled = true
                    binding.btnTaoQuiz.text = "Tạo Quiz ngay"
                    Toast.makeText(
                        this@TaoQuizActivity,
                        "Lỗi kết nối mạng: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun updateQuantitySelection(qty: Int) {
        selectedQuantity = qty

        binding.btnQty5.setBackgroundResource(R.drawable.bg_button_outline)
        binding.btnQty5.setTextColor(ContextCompat.getColor(this, R.color.text_main))

        binding.btnQty10.setBackgroundResource(R.drawable.bg_button_outline)
        binding.btnQty10.setTextColor(ContextCompat.getColor(this, R.color.text_main))

        binding.btnQty20.setBackgroundResource(R.drawable.bg_button_outline)
        binding.btnQty20.setTextColor(ContextCompat.getColor(this, R.color.text_main))

        when (qty) {
            5 -> {
                binding.btnQty5.setBackgroundResource(R.drawable.bg_button_selected)
                binding.btnQty5.setTextColor(ContextCompat.getColor(this, R.color.white))
            }
            10 -> {
                binding.btnQty10.setBackgroundResource(R.drawable.bg_button_selected)
                binding.btnQty10.setTextColor(ContextCompat.getColor(this, R.color.white))
            }
            20 -> {
                binding.btnQty20.setBackgroundResource(R.drawable.bg_button_selected)
                binding.btnQty20.setTextColor(ContextCompat.getColor(this, R.color.white))
            }
        }
    }
}