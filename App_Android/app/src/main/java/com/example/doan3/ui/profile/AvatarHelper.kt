package com.example.doan3.ui.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.view.Gravity
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

object AvatarHelper {

    data class AvatarOption(
        val emoji: String,
        val bgHex: String,
        val label: String,
        val requiredLevel: Int
    )

    val AVATARS = listOf(
        AvatarOption("🦊", "#FFEDD5", "Cáo thông minh", 1),
        AvatarOption("🐱", "#F1F5F9", "Mèo chăm chỉ", 1),
        AvatarOption("🐼", "#E0E7FF", "Gấu trúc điềm tĩnh", 2),
        AvatarOption("🦉", "#EDE9FE", "Cú học thuật", 3),
        AvatarOption("🦁", "#FEF3C7", "Sư tử bản lĩnh", 5),
        AvatarOption("🎓", "#DCFCE7", "Học giả IELTS", 7),
        AvatarOption("🚀", "#DBEAFE", "Phi hành gia", 9),
        AvatarOption("👑", "#FCE7F3", "Thần đồng ngoại ngữ", 12)
    )

    private fun prefKey(context: Context): String {
        val prefs = context.getSharedPreferences("DoAn3Prefs", Context.MODE_PRIVATE)
        val uid = prefs.getString("USER_ID", "") ?: ""
        return "USER_AVATAR_INDEX_$uid"
    }

    fun saveUserLevel(context: Context, level: Int) {
        val prefs = context.getSharedPreferences("DoAn3Prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("USER_CURRENT_LEVEL", level.coerceAtLeast(1)).apply()
    }

    fun getUserLevel(context: Context): Int {
        val prefs = context.getSharedPreferences("DoAn3Prefs", Context.MODE_PRIVATE)
        return prefs.getInt("USER_CURRENT_LEVEL", 1).coerceAtLeast(1)
    }

    fun getSelectedIndex(context: Context): Int {
        val prefs = context.getSharedPreferences("DoAn3Prefs", Context.MODE_PRIVATE)
        return prefs.getInt(prefKey(context), -1)
    }

    fun saveSelectedIndex(context: Context, index: Int) {
        val prefs = context.getSharedPreferences("DoAn3Prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt(prefKey(context), index).apply()
    }

    fun applyAvatarToImageView(context: Context, imageView: ImageView?) {
        if (imageView == null) return
        val idx = getSelectedIndex(context)
        if (idx !in AVATARS.indices) return
        val option = AVATARS[idx]
        imageView.imageTintList = null
        imageView.clearColorFilter()
        imageView.setPadding(0, 0, 0, 0)
        imageView.setImageDrawable(createAvatarDrawable(context, option, locked = false))
    }

    fun createAvatarDrawable(context: Context, option: AvatarOption, locked: Boolean = false): BitmapDrawable {
        val size = (96 * context.resources.displayMetrics.density).toInt().coerceAtLeast(96)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (locked) Color.parseColor("#E2E8F0") else Color.parseColor(option.bgHex)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, bgPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size * 0.50f
            textAlign = Paint.Align.CENTER
            alpha = if (locked) 110 else 255
        }
        val yPos = (canvas.height / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(if (locked) "🔒" else option.emoji, size / 2f, yPos, textPaint)

        return BitmapDrawable(context.resources, bitmap)
    }

    fun showAvatarPickerDialog(context: Context, onSelected: (Int) -> Unit) {
        val density = context.resources.displayMetrics.density
        fun Int.dp(): Int = (this * density).toInt()
        val userLevel = getUserLevel(context)

        val grid = GridLayout(context).apply {
            columnCount = 4
            setPadding(16.dp(), 16.dp(), 16.dp(), 8.dp())
        }

        val dialog = AlertDialog.Builder(context)
            .setTitle("Kho Avatar (Bạn đang ở Cấp $userLevel) ✨")
            .setMessage("Tăng cấp bằng XP để mở khóa thêm Avatar hiếm!")
            .setView(grid)
            .setNegativeButton("Đóng", null)
            .create()

        val currentIdx = getSelectedIndex(context)
        AVATARS.forEachIndexed { idx, opt ->
            val isUnlocked = userLevel >= opt.requiredLevel
            val cell = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(6.dp(), 8.dp(), 6.dp(), 8.dp())
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                }
            }
            val iv = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(56.dp(), 56.dp())
                setImageDrawable(createAvatarDrawable(context, opt, locked = !isUnlocked))
            }
            val tv = TextView(context).apply {
                text = when {
                    !isUnlocked -> "🔒 Cấp ${opt.requiredLevel}"
                    idx == currentIdx -> "✓ Đang dùng"
                    else -> "Cấp ${opt.requiredLevel}"
                }
                textSize = 11f
                gravity = Gravity.CENTER
                setTextColor(
                    when {
                        !isUnlocked -> Color.parseColor("#94A3B8")
                        idx == currentIdx -> Color.parseColor("#2563EB")
                        else -> Color.parseColor("#334155")
                    }
                )
                setPadding(0, 4.dp(), 0, 0)
            }
            cell.addView(iv)
            cell.addView(tv)
            cell.setOnClickListener {
                if (!isUnlocked) {
                    SoundHapticHelper.playWrong(it)
                    Toast.makeText(
                        context,
                        "🔒 Đạt Cấp ${opt.requiredLevel} để mở khóa '${opt.label}'! (Bạn đang ở Cấp $userLevel)",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    SoundHapticHelper.playCorrect(it)
                    saveSelectedIndex(context, idx)
                    onSelected(idx)
                    dialog.dismiss()
                }
            }
            grid.addView(cell)
        }

        dialog.show()
    }
}
