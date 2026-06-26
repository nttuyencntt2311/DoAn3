package com.example.doan3

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.doan3.databinding.ActivityMainBinding
import com.example.doan3.ui.home.HomeFragment
// Import sẵn hoso để dùng ở dưới
import com.example.doan3.ui.profile.hoso
import com.example.doan3.ui.profile.KhamPhaActivity


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
         * Sử dụng đúng ID từ menu nãy: btnTrangChu, btnKhamPha, btnTroLyAI, btnHoSo, btnTaiKhoan
         */

        // 1. Nút Trang chủ
        binding.btnTrangChu.setOnClickListener {
            replaceFragment(HomeFragment())
        }

        // 2. Nút Khám phá
        binding.btnKhamPha.setOnClickListener {
            // Khởi tạo Intent trỏ tới đúng tên class là KhamPhaActivity
            val intent = Intent(this, KhamPhaActivity::class.java)

            // Lệnh yêu cầu Android mở trang đó lên
            startActivity(intent)
        }

        // 3. Nút Trợ lý AI
        binding.btnTroLyAI.setOnClickListener {
            // Thay thế bằng Fragment Trợ lý AI (nếu có)
            // replaceFragment(AiFragment())
        }

        // 4. Nút Hồ sơ (Mở Activity hoso như bạn yêu cầu)
        binding.btnHoSo.setOnClickListener {
            val intent = Intent(this, hoso::class.java)
            startActivity(intent)
        }

        // 5. Nút Tài khoản
        binding.btnTaiKhoan.setOnClickListener {
            // Xử lý mở màn hình Tài khoản (nếu có)
            // val intent = Intent(this, TaiKhoanActivity::class.java)
            // startActivity(intent)
        }
    }

    // Hàm hỗ trợ đổi Fragment mượt mà trong Activity
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment) // ID này phải khớp với FragmentContainerView trong activity_main.xml
            .commit()
    }
}