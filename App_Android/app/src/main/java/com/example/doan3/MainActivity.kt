package com.example.doan3

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.doan3.databinding.ActivityMainBinding
import com.example.doan3.ui.home.HomeFragment

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

        // Bắt sự kiện chuyển đổi giữa các tab ở thanh menu dưới cùng (Bottom Navigation)
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    replaceFragment(HomeFragment())
                    true
                }
                // Sau này bạn làm tiếp các trang Khám phá, AI, Hồ sơ thì thêm vào đây:
                /*
                R.id.nav_explore -> {
                    replaceFragment(ExploreFragment())
                    true
                }
                */
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