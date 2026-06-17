package com.example.doan3

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.doan3.databinding.ActivityMainBinding
import com.example.doan3.ui.home.HomeFragment
import com.example.doan3.ui.profile.ProfileFragment

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

        // Bắt sự kiện chuyển đổi giữa các tab khi bấm vào Bottom Navigation
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    replaceFragment(HomeFragment())
                    true
                }
                R.id.nav_profile -> { 
                    replaceFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
        // Bên trong hàm onCreate, tìm đoạn setOnItemSelectedListener và cập nhật:
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    replaceFragment(HomeFragment())
                    true
                }
                R.id.nav_profile -> {
                    replaceFragment(ProfileFragment()) // Chuyển sang trang Hồ sơ
                    true
                }
                // Thêm các tab khác ở đây nếu có
                else -> false
            }
        }
    }

    // Hàm hỗ trợ đổi Fragment mượt mà
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()
    }
}