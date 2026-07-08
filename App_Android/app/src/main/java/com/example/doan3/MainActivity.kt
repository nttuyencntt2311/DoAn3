package com.example.doan3

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.doan3.databinding.ActivityMainBinding
import com.example.doan3.ui.home.HomeFragment
import com.example.doan3.ui.profile.hoso
import com.example.doan3.ui.profile.KhamPhaActivity
import com.example.doan3.ui.profile.TroLyAIActivity
import com.example.doan3.ui.profile.TaoQuizActivity
import com.example.doan3.ui.profile.TaiKhoanActivity
// ĐÂY CHÍNH LÀ DÒNG LỆNH GIÚP HẾT LỖI GẠCH ĐỎ:
import com.example.doan3.ui.statistics.StatisticsFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Mặc định ban đầu hiển thị trang HomeFragment
        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
        }

        // 1. Nút Trang chủ
        binding.btnTrangChu.setOnClickListener {
            replaceFragment(HomeFragment())
        }

        // 2. Nút Khám phá
        binding.btnKhamPha.setOnClickListener {
            val intent = Intent(this, KhamPhaActivity::class.java)
            startActivity(intent)
        }

        // 3. Nút Trợ lý AI
        binding.btnTroLyAI.setOnClickListener {
            val intent = Intent(this, TroLyAIActivity::class.java)
            startActivity(intent)
        }

        // 4. Nút Hồ sơ
        binding.btnHoSo.setOnClickListener {
            val intent = Intent(this, hoso::class.java)
            startActivity(intent)
        }

        // 5. Nút Tài khoản
        binding.btnTaiKhoan.setOnClickListener {
            val intent = Intent(this, TaiKhoanActivity::class.java)
            startActivity(intent)
        }

        // Nhận tín hiệu từ màn hình Hồ sơ gửi về
        val openStatistics = intent.getBooleanExtra("OPEN_STATISTICS", false)
        if (openStatistics) {
            // Tự động chuyển đổi giao diện sang màn hình Thống kê
            supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, StatisticsFragment())
                .commit()
        }
    }

    // Hàm hỗ trợ đổi Fragment mượt mà trong Activity
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()
    }
}