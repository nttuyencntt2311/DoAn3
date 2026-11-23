package com.example.doan3.ui.profile

import android.media.AudioManager
import android.media.ToneGenerator
import android.view.HapticFeedbackConstants
import android.view.View

object SoundHapticHelper {

    fun playCorrect(view: View? = null) {
        view?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        playTone(ToneGenerator.TONE_PROP_ACK, 120)
    }

    fun playWrong(view: View? = null) {
        view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        playTone(ToneGenerator.TONE_PROP_NACK, 180)
    }

    fun playTap(view: View? = null) {
        view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    private fun playTone(toneType: Int, durationMs: Int) {
        try {
            val tg = ToneGenerator(AudioManager.STREAM_MUSIC, 65)
            tg.startTone(toneType, durationMs)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try { tg.release() } catch (_: Exception) {}
            }, durationMs + 80L)
        } catch (_: Exception) {
            // Bỏ qua nếu máy đang tắt âm hoặc không hỗ trợ ToneGenerator
        }
    }
}
