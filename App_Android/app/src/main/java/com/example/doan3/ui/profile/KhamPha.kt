package com.example.doan3.ui.profile

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.card.MaterialCardView
import com.example.doan3.R
import com.example.doan3.MainActivity

class KhamPhaActivity : AppCompatActivity() {

    private var isFront = true // Biến theo dõi mặt trước/sau của thẻ

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.khampha)

        // 1. Xử lý tràn viền
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 2. Các nút điều hướng Bottom Nav

        // (Tùy chọn) Nút Trang chủ - Thêm dấu ? để nếu XML không có nút này app cũng không bị văng
        val btnTrangChu = findViewById<LinearLayout?>(R.id.btnTrangChu)
        btnTrangChu?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        // Chuyển sang màn hình Trợ lý AI
        val btnTroLyAI = findViewById<LinearLayout?>(R.id.btnTroLyAI)
        btnTroLyAI?.setOnClickListener {
            val intent = Intent(this, TroLyAIActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        // Chuyển sang màn hình Hồ sơ
        val btnHoSo = findViewById<LinearLayout?>(R.id.btnHoSo)
        btnHoSo?.setOnClickListener {
            val intent = Intent(this, hoso::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        // Chuyển sang màn hình Tài khoản
        val btnTaiKhoan = findViewById<LinearLayout?>(R.id.btnTaiKhoan)
        btnTaiKhoan?.setOnClickListener {
            val intent = Intent(this, TaiKhoanActivity::class.java) // Đảm bảo bạn có Activity này nhé
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
        // 3. Xử lý LẬT THẺ TỪ VỰNG (Click to Flip với hiệu ứng lật)
        val cardTuVung = findViewById<MaterialCardView>(R.id.cardTuVung)
        val tvWord = findViewById<TextView>(R.id.tvWord)
        val tvPronunciation = findViewById<TextView>(R.id.tvPronunciation)
        val tvMeaning = findViewById<TextView>(R.id.tvMeaning)

        cardTuVung.setOnClickListener {
            // Hiệu ứng co thẻ lại theo chiều ngang
            cardTuVung.animate().scaleX(0f).setDuration(150).withEndAction {
                // Đảo trạng thái mặt thẻ
                isFront = !isFront

                if (isFront) {
                    // Hiện mặt trước (Tiếng Anh)
                    tvWord.visibility = View.VISIBLE
                    tvPronunciation.visibility = View.VISIBLE
                    tvMeaning.visibility = View.GONE
                } else {
                    // Hiện mặt sau (Tiếng Việt)
                    tvWord.visibility = View.GONE
                    tvPronunciation.visibility = View.GONE
                    tvMeaning.visibility = View.VISIBLE
                }

                // Bung thẻ rộng ra lại bình thường
                cardTuVung.animate().scaleX(1f).setDuration(150).start()
            }.start()
        }

        // 4. Ánh xạ và xử lý sự kiện click cho 3 nút mới (Again, Good, Easy)
        val btnAgain = findViewById<MaterialCardView>(R.id.btnAgain)
        val btnGood = findViewById<MaterialCardView>(R.id.btnGood)
        val btnEasy = findViewById<MaterialCardView>(R.id.btnEasy)

        btnAgain.setOnClickListener {
            Toast.makeText(this, "Sẽ lặp lại từ này sớm hơn!", Toast.LENGTH_SHORT).show()
            resetCardToFront(tvWord, tvPronunciation, tvMeaning)
            loadNextWord()
        }

        btnGood.setOnClickListener {
            Toast.makeText(this, "Đã ghi nhớ ổn định!", Toast.LENGTH_SHORT).show()
            resetCardToFront(tvWord, tvPronunciation, tvMeaning)
            loadNextWord()
        }

        btnEasy.setOnClickListener {
            Toast.makeText(this, "Từ này quá dễ với bạn!", Toast.LENGTH_SHORT).show()
            resetCardToFront(tvWord, tvPronunciation, tvMeaning)
            loadNextWord()
        }

        // 5. Hiệu ứng nhịp thở cho nút Star hành động
        val btnHanhDong = findViewById<MaterialCardView>(R.id.btnHanhDongVuot)
        val nhapNhay = android.animation.ObjectAnimator.ofFloat(btnHanhDong, "alpha", 1f, 0.6f, 1f)
        nhapNhay.duration = 1500
        nhapNhay.repeatCount = android.animation.ObjectAnimator.INFINITE
        nhapNhay.start()
    }

    // Hàm bổ trợ để khi chuyển sang từ mới thì thẻ luôn tự động quay về mặt trước (Tiếng Anh)
    private fun resetCardToFront(tvWord: TextView, tvPronunciation: TextView, tvMeaning: TextView) {
        isFront = true
        tvWord.visibility = View.VISIBLE
        tvPronunciation.visibility = View.VISIBLE
        tvMeaning.visibility = View.GONE
    }

    private fun loadNextWord() {
        // Logic đổi data từ vựng mới của bạn viết ở đây
    }
}