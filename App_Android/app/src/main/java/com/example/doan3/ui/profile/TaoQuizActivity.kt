package com.example.doan3.ui.profile

import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.doan3.R
import com.example.doan3.databinding.ActivityTaoQuizBinding
import java.io.BufferedReader
import java.io.InputStreamReader

class TaoQuizActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaoQuizBinding

    private var selectedQuantity = 10
    private var selectedDifficulty = "Trung bình"

    // 1. BỘ CHỌN TỆP: Lắng nghe kết quả khi người dùng chọn file từ điện thoại
    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            // Đọc nội dung file và gán vào ô nhập liệu
            val fileContent = readTextFromUri(it)
            binding.edtContent.setText(fileContent)
            Toast.makeText(this, "Đã tải dữ liệu từ tệp lên!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaoQuizBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Nút Back
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Đếm số ký tự khi nhập nội dung
        binding.edtContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val length = s?.length ?: 0
                binding.tvCharCount.text = "$length / 5000 ký tự"
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Nút TẢI LÊN TÀI LIỆU
        binding.btnUpload.setOnClickListener {
            // Gọi bộ chọn tệp, lọc lấy các tệp dạng văn bản (text)
            pickFileLauncher.launch("text/plain")
            // Lưu ý: Sau này nếu muốn hỗ trợ đọc file PDF hoặc Word, ta sẽ cần thêm thư viện riêng (như iTextPDF hoặc Apache POI)
        }

        // Xử lý thanh Mức độ khó (SeekBar)
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

        // Xử lý nút Số lượng câu hỏi
        binding.btnQty5.setOnClickListener { updateQuantitySelection(5) }
        binding.btnQty10.setOnClickListener { updateQuantitySelection(10) }
        binding.btnQty20.setOnClickListener { updateQuantitySelection(20) }

        // Nút TẠO QUIZ NGAY
        binding.btnTaoQuiz.setOnClickListener {
            val content = binding.edtContent.text.toString().trim()
            if (content.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập nội dung hoặc tải tài liệu lên nhé!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val questionType = when (binding.rgQuestionType.checkedRadioButtonId) {
                R.id.rbTracNghiem -> "Trắc nghiệm"
                R.id.rbDungSai -> "Đúng / Sai"
                R.id.rbDienKhuyet -> "Điền vào chỗ trống"
                else -> "Trắc nghiệm" // Mặc định
            }

            // Gọi quy trình: Gửi cho AI -> Lấy kết quả -> Lưu dữ liệu
            generateQuizWithAI(content, questionType, selectedDifficulty, selectedQuantity)
        }
    }

    // 2. HÀM ĐỌC DỮ LIỆU TỪ TỆP VĂN BẢN
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

    // 3. HÀM GIAO TIẾP VỚI AI (Mô phỏng quy trình)
    private fun generateQuizWithAI(content: String, type: String, difficulty: String, quantity: Int) {
        Toast.makeText(this, "Đang gửi tài liệu cho AI phân tích...", Toast.LENGTH_SHORT).show()

        /* ======== GÓC LẬP TRÌNH AI ========
        Sau này bạn dùng API của Gemini hoặc ChatGPT, bạn sẽ ráp vào đây.
        Yêu cầu (Prompt) chuẩn sẽ như thế này:
        */
        val promptForAI = """
            Dựa vào nội dung sau: "$content"
            Hãy tạo cho tôi $quantity câu hỏi $type, với độ khó là $difficulty.
            Hãy trả về định dạng JSON bao gồm: danh sách câu hỏi, các đáp án và đáp án đúng.
        """.trimIndent()

        // (Giả lập) Thời gian chờ AI trả lời mất khoảng 2-3 giây.
        // Dưới đây là kết quả giả lập AI đã sinh ra quiz xong:
        val aiGeneratedQuizJson = "[ { \"question\": \"Câu hỏi 1\", \"answer\": \"A\" } ]" // Ví dụ chuỗi JSON

        // Sau khi có kết quả từ AI, ta đem đi lưu trữ
        saveQuizToDatabase(aiGeneratedQuizJson)
    }

    // 4. HÀM LƯU TRỮ VÀO DATABASE (Mô phỏng)
    private fun saveQuizToDatabase(quizDataJson: String) {
        /*
        ======== GÓC LƯU TRỮ ========
        Chỗ này bạn sẽ dùng Room Database hoặc SQLite để INSERT vào bảng.
        - Bước 1: Lưu Lịch sử (Thời gian tạo, độ khó, loại câu hỏi)
        - Bước 2: Parse chuỗi JSON bên trên thành List, rồi lưu từng Câu hỏi.
        */

        Toast.makeText(this, "Đã nhờ AI làm xong và LƯU QUIZ THÀNH CÔNG!", Toast.LENGTH_LONG).show()

        // (Tùy chọn) Chuyển sang màn hình Làm bài luôn hoặc màn hình Lịch sử
        // val intent = Intent(this, LamBaiQuizActivity::class.java)
        // startActivity(intent)
        // finish()
    }

    // Hàm hỗ trợ đổi màu nút
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