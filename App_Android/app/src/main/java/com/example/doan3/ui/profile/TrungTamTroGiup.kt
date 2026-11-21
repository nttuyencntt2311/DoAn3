package com.example.doan3.ui.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.doan3.MainActivity
import com.example.doan3.R

class HelpCenterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.trungtamtrogiup)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<View>(R.id.btnEmailSupport).setOnClickListener {
            startActivity(Intent(this, SendEmailActivity::class.java))
        }

        findViewById<View>(R.id.btnCommunity).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/groups/tuvungtienganh"))
            startActivity(intent)
        }

        // Mở tab Tài khoản trong MainActivity
        findViewById<View>(R.id.btnAccountSupport).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("OPEN_FRAGMENT", "ACCOUNT")
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }

        // Mở tab Bài học trong MainActivity
        findViewById<View>(R.id.btnLessonSupport).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("OPEN_FRAGMENT", "LEARNING")
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }

        setupSearchAndFaq()
    }

    private fun setupSearchAndFaq() {
        val edtSearchHelp = findViewById<EditText>(R.id.edtSearchHelp)
        val btnSearchAction = findViewById<View>(R.id.btnSearchAction)
        val tvViewAllFaq = findViewById<TextView>(R.id.tvViewAllFaq)

        val faqItems = listOf(
            Triple(
                findViewById<View>(R.id.cardFaq1),
                findViewById<TextView>(R.id.tvFaqAnswer1),
                "làm sao để hủy gói pro thanh toán nâng cấp"
            ),
            Triple(
                findViewById<View>(R.id.cardFaq2),
                findViewById<TextView>(R.id.tvFaqAnswer2),
                "tôi quên mật khẩu đăng nhập khôi phục tài khoản"
            ),
            Triple(
                findViewById<View>(R.id.cardFaq3),
                findViewById<TextView>(R.id.tvFaqAnswer3),
                "cách học offline ngoại tuyến tải bài học"
            )
        )

        faqItems.forEach { (card, answerView, _) ->
            card.setOnClickListener {
                answerView.visibility = if (answerView.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            }
        }

        tvViewAllFaq.setOnClickListener {
            val anyCollapsed = faqItems.any { it.second.visibility != View.VISIBLE }
            faqItems.forEach { (card, answerView, _) ->
                card.visibility = View.VISIBLE
                answerView.visibility = if (anyCollapsed) View.VISIBLE else View.GONE
            }
            tvViewAllFaq.text = if (anyCollapsed) "Thu gọn" else "Xem tất cả"
        }

        fun filterFaq(query: String, showToastIfEmpty: Boolean) {
            val q = query.trim().lowercase()
            var matchCount = 0
            faqItems.forEach { (card, _, keywords) ->
                val matches = q.isEmpty() || keywords.contains(q)
                card.visibility = if (matches) View.VISIBLE else View.GONE
                if (matches) matchCount++
            }
            if (showToastIfEmpty && matchCount == 0) {
                Toast.makeText(this, "Không tìm thấy câu hỏi phù hợp với \"$query\"", Toast.LENGTH_SHORT).show()
            }
        }

        btnSearchAction.setOnClickListener {
            filterFaq(edtSearchHelp.text.toString(), showToastIfEmpty = true)
        }

        edtSearchHelp.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterFaq(s?.toString() ?: "", showToastIfEmpty = false)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }
}