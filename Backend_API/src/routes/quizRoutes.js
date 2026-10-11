// File: routes/quizRoutes.js
const express = require("express");
const HistoryModel = require("../models/History");
const UserModel = require("../models/User");
const Vocabulary = require("../models/Vocabulary"); 
const verifyToken = require("../middleware/authMiddleware");
const axios = require("axios");
const router = express.Router();

// =====================================
// API: NỘP KẾT QUẢ BÀI QUIZ / BÀI HỌC
// =====================================
router.post("/submit", verifyToken, async (req, res) => {
  try {
    const {
      topic,
      score,
      totalQuestions,
      earnedXp,
      learnedWords,
      timeSpentMinutes,
      wrongWords, // Lấy từ vựng sai từ body
      difficulty,
      avg_time
    } = req.body;
    const userId = req.user.userId;

    // =========================================================================
    // 1. LƯU LỊCH SỬ LÀM BÀI VÀO BẢNG HISTORY
    // =========================================================================
    const newHistory = new HistoryModel({
      userId: userId,
      topic: topic || "Bài học chung",
      score: score || 0,
      totalQuestions: totalQuestions || 0,
    });
    await newHistory.save();

    // =========================================================================
    // 2. CỘNG DỒN CHỈ SỐ VÀO BẢNG USER (GAMIFICATION)
    // =========================================================================
    const user = await UserModel.findById(userId);
    if (!user) {
      return res.status(404).json({ error: "Không tìm thấy người dùng!" });
    }

    user.xp += earnedXp || 0;
    user.wordsLearned += learnedWords || 0;
    user.timeLearnedMinutes += timeSpentMinutes || 0;

    // TỰ ĐỘNG TĂNG LEVEL DỰA TRÊN XP
    const currentCalculatedLevel = Math.floor(user.xp / 100);
    let isLevelUp = false;
    if (currentCalculatedLevel > user.level) {
      user.level = currentCalculatedLevel;
      isLevelUp = true;
    }

    // TĂNG TỔNG SỐ BÀI QUIZ
    if (user.quizzesCount === undefined || user.quizzesCount === null) {
      user.quizzesCount = 0;
    }
    user.quizzesCount += 1;

    // =========================================================================
    // 3. BẮT LỖI TỪ VỰNG SAI ĐỂ LÀM DỮ LIỆU CHO SỔ TAY TIÊN TRI
    // =========================================================================
    if (wrongWords && wrongWords.length > 0) {
      const lowercaseWrongWords = wrongWords.map((w) => w.toLowerCase().trim());
      
      const vocabularies = await Vocabulary.find({
        word: { $in: lowercaseWrongWords },
      });

      if (!user.vocabProgress) user.vocabProgress = [];

      // Quét từng từ sai, nếu từ nào không có trong DB thì tạo khẩn cấp!
      for (let wrongWord of lowercaseWrongWords) {
        let vocab = vocabularies.find(v => v.word === wrongWord);
        
        // ĐÃ NÂNG CẤP: Dịch khi từ mới tinh HOẶC từ đã bị lưu nhầm nghĩa cũ
        if (!vocab || vocab.meaning === "Từ vựng AI sinh ra" || vocab.meaning === "Từ vựng mới") {
          let vietnameseMeaning = "Từ vựng mới";
          try {
            // Gọi API dịch thuật miễn phí của Google
            const translateUrl = `https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=vi&dt=t&q=${encodeURIComponent(wrongWord)}`;
            const transRes = await axios.get(translateUrl);
            
            // Bóc tách kết quả trả về để lấy nghĩa Tiếng Việt
            if (transRes.data && transRes.data[0] && transRes.data[0][0]) {
              vietnameseMeaning = transRes.data[0][0][0]; 
            }
          } catch (err) {
            console.error("Không thể tự động dịch nghĩa:", err.message);
          }

          if (!vocab) {
            // Trương hợp 1: Từ hoàn toàn chưa có trong Database
            vocab = new Vocabulary({
              word: wrongWord,
              meaning: vietnameseMeaning, // Lưu nghĩa chuẩn xác
              proficiency: 0
            });
            await vocab.save();
          } else {
            // Trường hợp 2: Từ đã có nhưng bị lỗi nghĩa cũ -> Cập nhật lại nghĩa mới!
            vocab.meaning = vietnameseMeaning;
            await vocab.save();
          }
        }

        // Cập nhật điểm số và số lần sai cho user
        const vocabIndex = user.vocabProgress.findIndex(
          (v) =>
            (v.wordId && v.wordId.toString() === vocab._id.toString()) ||
            (v.word && v.word.toString() === vocab._id.toString())
        );

        if (vocabIndex > -1) {
          user.vocabProgress[vocabIndex].proficiency = 0; 
          user.vocabProgress[vocabIndex].mistakeCount =
            (user.vocabProgress[vocabIndex].mistakeCount || 0) + 1;
          user.vocabProgress[vocabIndex].lastReviewedDate = new Date();
        } else {
          user.vocabProgress.push({
            word: vocab._id,
            wordId: vocab._id,
            proficiency: 0,
            mistakeCount: 1,
            reviewCount: 1,
            lastReviewedDate: new Date(),
          });
        }
      }
      user.markModified("vocabProgress");
    }

    // =========================================================================
    // 4. TÍNH TOÁN STREAKDAYS
    // =========================================================================
    // [QUAN TRỌNG]: Lưu lại ngày học cũ trước khi ghi đè để lát nữa tính time_diff cho AI
    const oldLastStudyDate = user.lastStudyDate;

    const getVNFormatDate = (date) => {
      const vnTime = new Date(date.getTime() + 7 * 60 * 60 * 1000);
      return vnTime.toISOString().split("T")[0];
    };

    const todayVNStr = getVNFormatDate(new Date());

    if (!user.lastStudyDate || !user.streakDays || user.streakDays === 0) {
      user.streakDays = 1;
    } else {
      const lastStudyVNStr = getVNFormatDate(user.lastStudyDate);

      const todayDate = new Date(todayVNStr);
      const lastStudyDate = new Date(lastStudyVNStr);

      const diffTime = todayDate - lastStudyDate;
      const diffDays = Math.round(diffTime / (1000 * 60 * 60 * 24));

      if (diffDays === 1) {
        user.streakDays += 1;
      } else if (diffDays > 1) {
        user.streakDays = 1;
      }
    }

    user.lastStudyDate = new Date();
    
    // LƯU LẠI THAY ĐỔI VÀO DATABASE
    await user.save();

    // =========================================================================
    // 5. GỌI "BỘ NÃO AI" ĐỂ LẬP BỆNH ÁN HỌC TẬP (BẢN CHỐT V2) & ÉP KIỂU ANDROID
    // =========================================================================
    let aiReport = null;
    
    // Tự động tính toán số liệu từ request của frontend để gửi cho AI
    const accuracy = totalQuestions > 0 ? score / totalQuestions : 0.5;

    // Tính thời gian trung bình (mili-giây) trên 1 câu hỏi
    const avgTimeMs =
      timeSpentMinutes && totalQuestions > 0
        ? Math.round((timeSpentMinutes * 60 * 1000) / totalQuestions)
        : 45000; // Mặc định 45s nếu không có dữ liệu

    try {
      // Tính khoảng cách thời gian nghỉ (time_diff) tính bằng giây
      const lastStudyTime = oldLastStudyDate ? new Date(oldLastStudyDate).getTime() : new Date().getTime();
      const currentTime = new Date().getTime();
      const timeDiffSeconds = Math.max(0, (currentTime - lastStudyTime) / 1000);

      // Cấu trúc Data chuẩn 7 biến gửi sang Python XGBoost
      const studentData = {
        user_history_acc: Number(accuracy.toFixed(2)),
        user_questions_attempted: user.quizzesCount * (totalQuestions || 10),
        question_difficulty: difficulty || 0.5, // XGBoost hoạt động tốt với dải scale 0.1 - 0.9
        user_avg_elapsed_time: avg_time || avgTimeMs,
        
        // 3 Biến mới cho bản chốt
        rolling_acc_5: Number(accuracy.toFixed(2)), // Tạm lấy tỉ lệ hiện tại làm phong độ
        time_diff: timeDiffSeconds,
        elapsed_time: avgTimeMs // Giả định thời gian làm câu hiện tại bằng mức trung bình
      };

      // Bắn dữ liệu sang Python qua Docker (Cổng 8000)
      const aiResponse = await axios.post(
        "http://localhost:8000/predict-weakness",
        studentData
      );
      aiReport = aiResponse.data;
    } catch (aiError) {
      console.error(
        "⚠️ AI Server không phản hồi, dùng báo cáo dự phòng!",
        aiError.message
      );
    }

    // 🚀 BƯỚC QUAN TRỌNG: Thông dịch dữ liệu để UI Android có thể vẽ được
    if (!aiReport) {
        aiReport = {
            predicted_accuracy: accuracy * 100,
            is_struggling: wrongWords && wrongWords.length > 0,
            xai_explanation: { positive_factors: [], negative_factors: [] }
        };
    }

    if (aiReport && aiReport.xai_explanation) {
        if (!aiReport.xai_explanation.positive_factors) aiReport.xai_explanation.positive_factors = [];
        if (!aiReport.xai_explanation.negative_factors) aiReport.xai_explanation.negative_factors = [];

        // 1. Chèn thêm số Mili-giây để Android cắt ra bằng Regex (Mức: ...)
        if (avgTimeMs > 25000) {
            aiReport.xai_explanation.negative_factors.unshift(`(Mức: ${avgTimeMs}) Thời gian suy nghĩ trung bình khá chậm`);
        } else {
            aiReport.xai_explanation.positive_factors.unshift(`(Mức: ${avgTimeMs}) Tốc độ phản xạ cực kỳ nhạy bén`);
        }

        // 2. Chèn danh sách từ sai (Nếu có)
        if (wrongWords && wrongWords.length > 0) {
            aiReport.xai_explanation.negative_factors.push(`Còn nhầm lẫn ở ${wrongWords.length} từ vựng: ${wrongWords.join(", ")}`);
        } else {
            aiReport.xai_explanation.positive_factors.push(`Không phát hiện rủi ro, phong độ đang rất xuất sắc!`);
        }
    }

    // =========================================================================
    // 6. TRẢ KẾT QUẢ VỀ CHO FRONTEND ANDROID
    // =========================================================================
    res.status(200).json({
      message: "Nộp kết quả bài quiz thành công!",
      isLevelUp: isLevelUp,
      updatedStats: {
        xp: user.xp,
        level: user.level,
        streakDays: user.streakDays,
        wordsLearned: user.wordsLearned,
        timeLearnedMinutes: user.timeLearnedMinutes,
        quizzesCount: user.quizzesCount,
      },
      ai_report: aiReport // Đính kèm Bệnh án phân tích XAI trả về cho app hiển thị
    });
  } catch (error) {
    console.error("Lỗi nộp bài:", error);
    res.status(500).json({ error: "Lỗi hệ thống khi xử lý nộp bài quiz!" });
  }
});

module.exports = router;