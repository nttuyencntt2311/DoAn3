package com.example.doan3

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.doan3.databinding.ActivityMainBinding
import com.example.doan3.ui.home.HomeFragment
import com.example.doan3.ui.profile.hoso
import com.example.doan3.ui.profile.KhamPhaActivity // Code của Tuyền
import com.example.doan3.ui.profile.TroLyAIActivity // Code của Vân
import com.example.doan3.ui.profile.TaoQuizActivity
import com.example.doan3.ui.profile.TaiKhoanActivity

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

        /* * BẮT SỰ KIỆN CHO THANH MENU CUSTOM BẰNG LINEARLAYOUT
         * Sử dụng đúng ID từ menu: btnTrangChu, btnKhamPha, btnTroLyAI, btnHoSo, btnTaiKhoan
         */

        // 1. Nút Trang chủ
        binding.btnTrangChu.setOnClickListener {
            replaceFragment(HomeFragment())
        }

        // 2. Nút Khám phá (Đã gộp code của Tuyền)
        binding.btnKhamPha.setOnClickListener {
            val intent = Intent(this, KhamPhaActivity::class.java)
            startActivity(intent)
        }

        // 3. Nút Trợ lý AI (Đã gộp code của Vân)
        binding.btnTroLyAI.setOnClickListener {
            val intent = Intent(this, TroLyAIActivity::class.java)
            startActivity(intent)
        }

        // 4. Nút Hồ sơ
        binding.btnHoSo.setOnClickListener {
            val intent = Intent(this, hoso::class.java)
            startActivity(intent)
        }

        // 5. Nút Tài khoản (ĐÃ MỞ KHÓA: Kết nối sang TaiKhoanActivity)
        binding.btnTaiKhoan.setOnClickListener {
            val intent = Intent(this, TaiKhoanActivity::class.java)
            startActivity(intent)
        }
    }

    // Hàm hỗ trợ đổi Fragment mượt mà trong Activity
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()
    }
}