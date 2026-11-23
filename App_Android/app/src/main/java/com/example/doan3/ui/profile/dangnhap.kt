package com.example.doan3.ui.profile

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.doan3.MainActivity
import com.example.doan3.R
import com.example.doan3.ui.ai.ChatAIActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class dangnhap : AppCompatActivity() {

    private val floatingAnimators = mutableListOf<ObjectAnimator>()
    private val mainHandler = Handler(Looper.getMainLooper())

    private val googleAccountPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val email = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)?.trim() ?: ""
            if (email.isNotEmpty()) {
                performGoogleLogin(email, email.substringBefore("@"))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.dangnhap)

        val sharedPreferences = getSharedPreferences("DoAn3Prefs", MODE_PRIVATE)
        val savedToken = sharedPreferences.getString("JWT_TOKEN", "") ?: ""

        // Chạy hiệu ứng Splash Intro mượt mà trước khi vào Trang chủ hoặc mở Form Đăng nhập
        playSplashIntro(hasValidSession = savedToken.isNotEmpty(), savedToken = savedToken)

        val tvRegister = findViewById<TextView>(R.id.tvRegister)
        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)
        tvForgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }

        val btnGoogleLogin = findViewById<MaterialButton>(R.id.btnGoogleLogin)
        btnGoogleLogin.setOnClickListener {
            launchGoogleAccountChooser()
        }

        val edtEmailLogin = findViewById<TextInputEditText>(R.id.edtEmailLogin)
        val edtPasswordLogin = findViewById<TextInputEditText>(R.id.edtPasswordLogin)
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)
        val loginSheetCard = findViewById<View>(R.id.loginSheetCard)

        btnLogin.setOnClickListener {
            val email = edtEmailLogin.text.toString().trim()
            val password = edtPasswordLogin.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                loginSheetCard.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake))
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnLogin.isEnabled = false
            btnLogin.text = "Đang đăng nhập..."

            val request = LoginRequest(email, password)
            RetrofitClient.getInstance(this).loginUser(request).enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    if (isFinishing || isDestroyed) return
                    btnLogin.isEnabled = true
                    btnLogin.text = "Đăng nhập"

                    if (response.isSuccessful && response.body() != null) {
                        val loginData = response.body()
                        val token = loginData?.token ?: ""
                        val userId = loginData?.user?.id ?: ""
                        val userName = loginData?.user?.name ?: ""
                        val refreshToken = loginData?.refreshToken ?: ""

                        sharedPreferences.edit().apply {
                            putString("JWT_TOKEN", token)
                            putString("USER_ID", userId)
                            putString("REFRESH_TOKEN", refreshToken)
                            putString("USER_NAME", userName)
                            apply()
                        }

                        RetrofitClient.token = token
                        ChatAIActivity.clearActiveSession()

                        Toast.makeText(this@dangnhap, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                        val isOnboardingDone = sharedPreferences.getBoolean("ONBOARDING_DONE_$userId", false)
                        val targetClass = if (isOnboardingDone) MainActivity::class.java else OnboardingActivity::class.java
                        val intent = Intent(this@dangnhap, targetClass).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                        finish()
                    } else {
                        loginSheetCard.startAnimation(AnimationUtils.loadAnimation(this@dangnhap, R.anim.shake))
                        val msg = when (response.code()) {
                            400, 401 -> "Sai email hoặc mật khẩu!"
                            500 -> "Máy chủ đang gặp sự cố, vui lòng thử lại sau."
                            else -> "Đăng nhập thất bại (Mã lỗi: ${response.code()})"
                        }
                        Toast.makeText(this@dangnhap, msg, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    if (isFinishing || isDestroyed) return
                    btnLogin.isEnabled = true
                    btnLogin.text = "Đăng nhập"
                    Toast.makeText(this@dangnhap, "Lỗi kết nối: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
        }
    }

    /** Hiệu ứng Intro thương hiệu lúc mới mở app -> tự vào MainActivity nếu đã đăng nhập, hoặc trượt mở Form Đăng nhập */
    private fun playSplashIntro(hasValidSession: Boolean, savedToken: String) {
        val splashOverlay = findViewById<View>(R.id.splashOverlay)
        val splashLogoCard = findViewById<View>(R.id.splashLogoCard)
        val splashPulseRing = findViewById<View>(R.id.splashPulseRing)
        val splashTitle = findViewById<View>(R.id.splashTitle)
        val splashSubtitle = findViewById<View>(R.id.splashSubtitle)
        val loginSheetCard = findViewById<View>(R.id.loginSheetCard)
        val heroCenterBrand = findViewById<View>(R.id.heroCenterBrand)

        // Chuẩn bị trạng thái ẩn ban đầu để tạo chuyển động mượt
        splashLogoCard.scaleX = 0.5f
        splashLogoCard.scaleY = 0.5f
        splashLogoCard.alpha = 0f
        splashTitle.alpha = 0f
        splashTitle.translationY = 30f
        splashSubtitle.alpha = 0f
        splashSubtitle.translationY = 30f

        if (!hasValidSession) {
            loginSheetCard.translationY = 260f
            loginSheetCard.alpha = 0f
            heroCenterBrand.alpha = 0f
        }

        // 1. Logo bật nảy đàn hồi
        splashLogoCard.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(500L)
            .setInterpolator(OvershootInterpolator(1.6f))
            .start()

        // Vòng sáng lan tỏa nhịp thở
        splashPulseRing.animate()
            .scaleX(1.35f)
            .scaleY(1.35f)
            .alpha(0.15f)
            .setDuration(900L)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // 2. Tên LexiVT & slogan trượt nhẹ lên
        splashTitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(180L)
            .setDuration(420L)
            .setInterpolator(DecelerateInterpolator())
            .start()

        splashSubtitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(320L)
            .setDuration(420L)
            .setInterpolator(DecelerateInterpolator())
            .start()

        val holdDuration = if (hasValidSession) 850L else 1250L
        mainHandler.postDelayed({
            if (isFinishing || isDestroyed) return@postDelayed

            if (hasValidSession) {
                RetrofitClient.token = savedToken
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            } else {
                // Mờ dần Splash Overlay và trượt mượt thẻ Form Đăng nhập từ dưới lên
                splashOverlay.animate()
                    .alpha(0f)
                    .setDuration(380L)
                    .withEndAction { splashOverlay.visibility = View.GONE }
                    .start()

                heroCenterBrand.animate()
                    .alpha(1f)
                    .setDuration(450L)
                    .start()

                loginSheetCard.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(480L)
                    .setInterpolator(DecelerateInterpolator(1.8f))
                    .start()

                startHeroFloatingAnimations()
            }
        }, holdDuration)
    }

    /** Tạo chuyển động bồng bềnh nhẹ nhàng cho các chip từ vựng AI trên Hero Header */
    private fun startHeroFloatingAnimations() {
        val pills = listOf(
            Triple(findViewById<View>(R.id.pillFloat1), -12f, 2200L),
            Triple(findViewById<View>(R.id.pillFloat2), 14f, 2600L),
            Triple(findViewById<View>(R.id.pillFloat3), -10f, 2400L)
        )
        pills.forEach { (view, deltaY, duration) ->
            if (view != null) {
                val anim = ObjectAnimator.ofFloat(view, "translationY", 0f, deltaY).apply {
                    this.duration = duration
                    repeatMode = ValueAnimator.REVERSE
                    repeatCount = ValueAnimator.INFINITE
                    interpolator = AccelerateDecelerateInterpolator()
                    start()
                }
                floatingAnimators.add(anim)
            }
        }
    }

    private fun showForgotPasswordDialog() {
        val emailInput = EditText(this).apply {
            hint = "Nhập email đã đăng ký tài khoản"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(this)
            .setTitle("Quên mật khẩu?")
            .setMessage("Nhập email của bạn để nhận mã OTP 6 số khôi phục mật khẩu.")
            .setView(emailInput)
            .setPositiveButton("Gửi mã OTP") { _, _ ->
                val email = emailInput.text.toString().trim()
                if (email.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập địa chỉ email!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                Toast.makeText(this, "Đang gửi mã OTP...", Toast.LENGTH_SHORT).show()
                RetrofitClient.getInstance(this).forgotPassword(ForgotPasswordRequest(email))
                    .enqueue(object : Callback<ForgotPasswordResponse> {
                        override fun onResponse(call: Call<ForgotPasswordResponse>, response: Response<ForgotPasswordResponse>) {
                            if (response.isSuccessful && response.body() != null) {
                                val demoOtp = response.body()?.demoOtp
                                val hintMsg = if (!demoOtp.isNullOrBlank()) {
                                    "Mã OTP của bạn là: $demoOtp (chế độ demo)"
                                } else {
                                    response.body()?.message ?: "Đã gửi mã OTP tới email của bạn!"
                                }
                                showResetPasswordOtpDialog(email, hintMsg, demoOtp)
                            } else {
                                Toast.makeText(this@dangnhap, "Email không tồn tại trong hệ thống!", Toast.LENGTH_LONG).show()
                            }
                        }

                        override fun onFailure(call: Call<ForgotPasswordResponse>, t: Throwable) {
                            Toast.makeText(this@dangnhap, "Lỗi kết nối máy chủ: ${t.message}", Toast.LENGTH_LONG).show()
                        }
                    })
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showResetPasswordOtpDialog(email: String, infoMsg: String, prefillOtp: String?) {
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 24, 48, 16)
        }
        val edtOtp = EditText(this).apply {
            hint = "Nhập mã OTP 6 số"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            if (!prefillOtp.isNullOrBlank()) setText(prefillOtp)
        }
        val edtNewPass = EditText(this).apply {
            hint = "Mật khẩu mới (tối thiểu 6 ký tự)"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        container.addView(edtOtp)
        container.addView(edtNewPass)

        AlertDialog.Builder(this)
            .setTitle("Đặt lại mật khẩu 🔐")
            .setMessage(infoMsg)
            .setView(container)
            .setPositiveButton("Xác nhận đổi") { _, _ ->
                val otp = edtOtp.text.toString().trim()
                val newPass = edtNewPass.text.toString().trim()
                if (otp.length != 6 || newPass.length < 6) {
                    Toast.makeText(this, "Vui lòng nhập đủ OTP 6 số và mật khẩu >= 6 ký tự!", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }
                RetrofitClient.getInstance(this).resetPassword(ResetPasswordRequest(email, otp, newPass))
                    .enqueue(object : Callback<ResetPasswordResponse> {
                        override fun onResponse(call: Call<ResetPasswordResponse>, response: Response<ResetPasswordResponse>) {
                            if (response.isSuccessful) {
                                SoundHapticHelper.playCorrect()
                                Toast.makeText(this@dangnhap, "✅ Đặt lại mật khẩu thành công! Hãy đăng nhập lại.", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(this@dangnhap, "Mã OTP không đúng hoặc đã hết hạn!", Toast.LENGTH_LONG).show()
                            }
                        }

                        override fun onFailure(call: Call<ResetPasswordResponse>, t: Throwable) {
                            Toast.makeText(this@dangnhap, "Lỗi mạng: ${t.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun launchGoogleAccountChooser() {
        try {
            val intent = AccountManager.newChooseAccountIntent(
                null, null, arrayOf("com.google"), null, null, null, null
            )
            googleAccountPickerLauncher.launch(intent)
        } catch (_: Exception) {
            showManualGoogleDialog()
        }
    }

    private fun showManualGoogleDialog() {
        val edtGoogleEmail = EditText(this).apply {
            hint = "Nhập địa chỉ Gmail của bạn (VD: ten@gmail.com)"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setPadding(48, 32, 48, 32)
        }
        AlertDialog.Builder(this)
            .setTitle("Tiếp tục với Google")
            .setMessage("Chọn hoặc nhập Gmail để đăng nhập nhanh vào LexiVT:")
            .setView(edtGoogleEmail)
            .setPositiveButton("Tiếp tục") { _, _ ->
                val email = edtGoogleEmail.text.toString().trim()
                if (email.contains("@")) {
                    performGoogleLogin(email, email.substringBefore("@"))
                } else {
                    Toast.makeText(this, "Vui lòng nhập Gmail hợp lệ!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun performGoogleLogin(email: String, displayName: String) {
        Toast.makeText(this, "Đang xác thực tài khoản Google ($email)...", Toast.LENGTH_SHORT).show()
        val sharedPreferences = getSharedPreferences("DoAn3Prefs", MODE_PRIVATE)
        RetrofitClient.getInstance(this).googleLogin(GoogleLoginRequest(email, displayName))
            .enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        val loginData = response.body()
                        val token = loginData?.token ?: ""
                        val userId = loginData?.user?.id ?: ""
                        val userName = loginData?.user?.name ?: displayName
                        val refreshToken = loginData?.refreshToken ?: ""

                        sharedPreferences.edit().apply {
                            putString("JWT_TOKEN", token)
                            putString("USER_ID", userId)
                            putString("REFRESH_TOKEN", refreshToken)
                            putString("USER_NAME", userName)
                            putString("USER_EMAIL", loginData?.user?.email ?: email)
                            apply()
                        }
                        RetrofitClient.token = token
                        ChatAIActivity.clearActiveSession()
                        SoundHapticHelper.playCorrect()

                        val isOnboardingDone = sharedPreferences.getBoolean("ONBOARDING_DONE_$userId", false)
                        val targetClass = if (isOnboardingDone) MainActivity::class.java else OnboardingActivity::class.java
                        val intent = Intent(this@dangnhap, targetClass).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@dangnhap, "Đăng nhập Google thất bại (${response.code()})", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    Toast.makeText(this@dangnhap, "Lỗi kết nối máy chủ: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
    }
}
