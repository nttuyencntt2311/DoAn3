package com.example.doan3.ui.profile

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.example.doan3.MainActivity
import com.example.doan3.R
import com.example.doan3.ui.ai.ChatAIActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TaiKhoanFragment : Fragment() {

    private var tvUserFullName: TextView? = null
    private var tvUserEmail: TextView? = null
    private var tvCurrentLanguage: TextView? = null
    private var switchNotifications: SwitchCompat? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.taikhoan, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvUserFullName = view.findViewById(R.id.tvUserFullName)
        tvUserEmail = view.findViewById(R.id.tvUserEmail)
        tvCurrentLanguage = view.findViewById(R.id.tvCurrentLanguage)
        switchNotifications = view.findViewById(R.id.switchNotificationsAccount)

        // Khi nằm trong tab Bottom Nav của MainActivity, bấm Back sẽ về Trang chủ
        view.findViewById<ImageView>(R.id.btnBack)?.setOnClickListener {
            (activity as? MainActivity)?.openHomeTab()
        }

        view.findViewById<View>(R.id.btnPersonalInfo)?.setOnClickListener {
            startActivity(Intent(requireContext(), ThongTinCaNhanActivity::class.java))
        }

        view.findViewById<View>(R.id.btnChangePassword)?.setOnClickListener {
            startActivity(Intent(requireContext(), DoiMatKhauActivity::class.java))
        }

        view.findViewById<View>(R.id.btnTerms)?.setOnClickListener {
            startActivity(Intent(requireContext(), DieuKhoanActivity::class.java))
        }

        setupNotificationSwitch()
        setupLanguageSelector(view)

        view.findViewById<View>(R.id.btnLogout)?.setOnClickListener {
            val sharedPreferences = requireContext().getSharedPreferences("DoAn3Prefs", Context.MODE_PRIVATE)
            val refreshToken = sharedPreferences.getString("REFRESH_TOKEN", "") ?: ""

            if (refreshToken.isNotEmpty()) {
                val request = LogoutRequest(refreshToken)
                RetrofitClient.getInstance(requireContext()).logoutUser(request)
                    .enqueue(object : Callback<Void> {
                        override fun onResponse(call: Call<Void>, response: Response<Void>) {
                            thucHienDangXuatCucBo()
                        }

                        override fun onFailure(call: Call<Void>, t: Throwable) {
                            thucHienDangXuatCucBo()
                        }
                    })
            } else {
                thucHienDangXuatCucBo()
            }
        }
    }

    private fun setupNotificationSwitch() {
        val prefs = requireContext().getSharedPreferences("DoAn3Prefs", Context.MODE_PRIVATE)
        val isNotifEnabled = prefs.getBoolean("PREF_NOTIFICATIONS", true)

        switchNotifications?.isChecked = isNotifEnabled
        updateSwitchColor(isNotifEnabled)

        switchNotifications?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("PREF_NOTIFICATIONS", isChecked).apply()
            updateSwitchColor(isChecked)
            val status = if (isChecked) "Đã bật" else "Đã tắt"
            Toast.makeText(requireContext(), "$status thông báo nhắc học", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateSwitchColor(isChecked: Boolean) {
        val color = if (isChecked) Color.parseColor("#2563EB") else Color.parseColor("#E2E8F0")
        switchNotifications?.trackTintList = ColorStateList.valueOf(color)
    }

    private fun setupLanguageSelector(view: View) {
        val prefs = requireContext().getSharedPreferences("DoAn3Prefs", Context.MODE_PRIVATE)
        val savedLang = prefs.getString("APP_LANGUAGE", "Tiếng Việt") ?: "Tiếng Việt"
        tvCurrentLanguage?.text = savedLang

        view.findViewById<View>(R.id.btnLanguage)?.setOnClickListener {
            val options = arrayOf("Tiếng Việt", "English")
            val currentIdx = options.indexOf(tvCurrentLanguage?.text?.toString()).coerceAtLeast(0)
            AlertDialog.Builder(requireContext())
                .setTitle("Chọn ngôn ngữ ứng dụng")
                .setSingleChoiceItems(options, currentIdx) { dialog, which ->
                    val chosen = options[which]
                    prefs.edit().putString("APP_LANGUAGE", chosen).apply()
                    tvCurrentLanguage?.text = chosen
                    Toast.makeText(requireContext(), "Đã chọn ngôn ngữ: $chosen", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .setNegativeButton("Đóng", null)
                .show()
        }
    }

    private fun thucHienDangXuatCucBo() {
        val ctx = context ?: return
        ctx.getSharedPreferences("DoAn3Prefs", Context.MODE_PRIVATE).edit().clear().apply()
        ctx.getSharedPreferences("StudyData", Context.MODE_PRIVATE).edit().clear().apply()
        RetrofitClient.token = ""
        ChatAIActivity.clearActiveSession()

        Toast.makeText(ctx, "Đã đăng xuất tài khoản an toàn!", Toast.LENGTH_SHORT).show()

        val intent = Intent(ctx, dangnhap::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        activity?.finish()
    }

    override fun onResume() {
        super.onResume()
        loadAccountData()
    }

    private fun loadAccountData() {
        val ctx = context ?: return
        RetrofitClient.getInstance(ctx).getProfile().enqueue(object : Callback<GetProfileResponse> {
            override fun onResponse(call: Call<GetProfileResponse>, response: Response<GetProfileResponse>) {
                if (!isAdded || view == null) return
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()?.user
                    if (user != null) {
                        tvUserEmail?.text = user.email
                        tvUserFullName?.text = user.name
                    }
                } else {
                    tvUserEmail?.text = "Lỗi tải email"
                    tvUserFullName?.text = "Lỗi tải thông tin"
                }
            }

            override fun onFailure(call: Call<GetProfileResponse>, t: Throwable) {
                if (!isAdded || view == null) return
                tvUserEmail?.text = "Mất kết nối"
                tvUserFullName?.text = "Mất kết nối"
            }
        })
    }
}