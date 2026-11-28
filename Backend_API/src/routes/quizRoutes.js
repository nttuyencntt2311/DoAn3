// File: routes/quizRoutes.js
const express = require("express");
const HistoryModel = require("../models/History");
const UserModel = require("../models/User");
const Vocabulary = require("../models/Vocabulary"); 
const verifyToken = require("../middleware/authMiddleware");
const axios = require("axios");
const aiService = require("../service/aiService");
const xaiService = require("../service/xaiService");
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
      avg_time,
      avgElapsedTimeMs,
      questionLogs
    } = req.body;
    const userId = req.user.userId;

    // =========================================================================
    // 1. LƯU LỊCH SỬ LÀM BÀI VÀO BẢNG HISTORY
    // =========================================================================
    const safeLogs = Array.isArray(questionLogs) ? questionLogs : [];
    const computedLogAvgMs =
      safeLogs.length > 0
        ? Math.round(safeLogs.reduce((sum, q) => sum + (Number(q.elapsedTimeMs) || 0), 0) / safeLogs.length)
        : (Number(avgElapsedTimeMs) || 0);

    const newHistory = new HistoryModel({
      userId: userId,
      topic: topic || "Bài học chung",
      score: score || 0,
      totalQuestions: totalQuestions || 0,
      avgElapsedTimeMs: computedLogAvgMs,
      questionLogs: safeLogs,
    });
    await newHistory.save();

    // =========================================================================
    // 2. CỘNG DỒN CHỈ SỐ VÀO BẢNG USER (GAMIFICATION)
    // =========================================================================
    const user = await UserModel.findById(userId);
    if (!user) {
      return res.status(404).json({ error: "Không tìm thấy người dùng!" });
    }

    const safeEarnedXp = Math.min(Math.max(Math.round(Number(earnedXp) || 0), 0), 500);
    const safeMinutes = Math.min(Math.max(Math.round(Number(timeSpentMinutes) || 1), 1), 180);

    user.xp = (user.xp || 0) + safeEarnedXp;
    // Bảo vệ trường hợp DB cũ từng lưu nhầm timestamp (> 100,000 phút)
    const currentMinutes = (user.timeLearnedMinutes || 0) > 100000 ? 0 : (user.timeLearnedMinutes || 0);
    user.timeLearnedMinutes = currentMinutes + safeMinutes;

    // Đồng bộ wordsLearned theo số từ thực tế đã thuộc (>= 80) trong vocabProgress
    if (Array.isArray(user.vocabProgress) && user.vocabProgress.length > 0) {
      user.wordsLearned = user.vocabProgress.filter((v) => (v.proficiency || 0) >= 80).length;
    } else {
      user.wordsLearned = (user.wordsLearned || 0) + Math.max(0, Number(learnedWords) || 0);
    }

    // Công thức cấp độ lũy tiến: bắt đầu từ Cấp 1, tránh tăng cấp quá nhanh
    const currentCalculatedLevel = Math.floor(Math.sqrt(Math.max(0, user.xp) / 100)) + 1;
    const isLevelUp = currentCalculatedLevel > (user.level || 1);
    user.level = currentCalculatedLevel;

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
        
        // Tự động bổ sung cả nghĩa tiếng Việt lẫn phiên âm (phonetic) nếu từ mới hoặc bị thiếu
        const needsMeaning =
          !vocab ||
          !vocab.meaning ||
          vocab.meaning === "Từ vựng AI sinh ra" ||
          vocab.meaning === "Từ vựng mới";
        const needsPhonetic = !vocab || !vocab.phonetic || vocab.phonetic.trim() === "";

        if (needsMeaning || needsPhonetic) {
          let vietnameseMeaning = vocab?.meaning || "";
          let fetchedPhonetic = vocab?.phonetic || "";

          // 1. Lấy phiên âm IPA từ Dictionary API (ưu tiên cho từ đơn)
          if (needsPhonetic) {
            try {
              const firstToken = wrongWord.split(/\s+/)[0];
              const dictRes = await axios.get(
                `https://api.dictionaryapi.dev/api/v2/entries/en/${encodeURIComponent(wrongWord)}`,
                { timeout: 3500 }
              );
              const entry = Array.isArray(dictRes.data) ? dictRes.data[0] : null;
              if (entry) {
                fetchedPhonetic =
                  entry.phonetic ||
                  (entry.phonetics || []).find((p) => p && p.text)?.text ||
                  "";
              }
              if (!fetchedPhonetic && firstToken && firstToken !== wrongWord) {
                const dictRes2 = await axios.get(
                  `https://api.dictionaryapi.dev/api/v2/entries/en/${encodeURIComponent(firstToken)}`,
                  { timeout: 2500 }
                );
                const entry2 = Array.isArray(dictRes2.data) ? dictRes2.data[0] : null;
                fetchedPhonetic =
                  entry2?.phonetic ||
                  (entry2?.phonetics || []).find((p) => p && p.text)?.text ||
                  "";
              }
            } catch (_) {
              // Bỏ qua nếu cụm từ không có trong từ điển đơn
            }
          }

          // 2. Dịch nghĩa tiếng Việt (Google Translate -> fallback Gemini AI)
          if (needsMeaning) {
            try {
              const translateUrl = `https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=vi&dt=t&q=${encodeURIComponent(wrongWord)}`;
              const transRes = await axios.get(translateUrl, { timeout: 4000 });
              if (transRes.data && transRes.data[0] && transRes.data[0][0]) {
                const candidate = String(transRes.data[0][0][0] || "").trim();
                if (candidate && candidate.toLowerCase() !== wrongWord.toLowerCase()) {
                  vietnameseMeaning = candidate;
                }
              }
            } catch (err) {
              console.warn("Google Translate không phản hồi:", err.message);
            }

            if (
              (!vietnameseMeaning || vietnameseMeaning === "Từ vựng mới" || vietnameseMeaning === "Từ vựng AI sinh ra") &&
              aiService.genAI &&
              process.env.GEMINI_API_KEY
            ) {
              try {
                const model = aiService.genAI.getGenerativeModel({ model: "gemini-2.5-flash" });
                const prompt = `Dịch ngắn gọn (tối đa 8 từ) thuật ngữ tiếng Anh "${wrongWord}" sang tiếng Việt tự nhiên. Chỉ trả về đúng nghĩa tiếng Việt, không giải thích thêm.`;
                const aiRes = await model.generateContent(prompt);
                const translated = aiRes.response.text().replace(/["*`\n]/g, "").trim();
                if (translated) vietnameseMeaning = translated;
              } catch (_) {}
            }

            if (!vietnameseMeaning) {
              vietnameseMeaning = `Thuật ngữ: ${wrongWord}`;
            }
          }

          if (!vocab) {
            vocab = new Vocabulary({
              word: wrongWord,
              phonetic: fetchedPhonetic,
              meaning: vietnameseMeaning,
              category: "Ôn tập XAI",
              proficiency: 0,
            });
            await vocab.save();
          } else {
            if (needsMeaning) vocab.meaning = vietnameseMeaning;
            if (needsPhonetic && fetchedPhonetic) vocab.phonetic = fetchedPhonetic;
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
            word: vocab.word,
            wordId: vocab._id.toString(),
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

    // Ưu tiên thời gian thực tế từng câu từ questionLogs (Fine-grained Behavioral Logging)
    const avgTimeMs =
      computedLogAvgMs > 0
        ? computedLogAvgMs
        : (timeSpentMinutes && totalQuestions > 0
            ? Math.round((timeSpentMinutes * 60 * 1000) / totalQuestions)
            : 23000);

    try {
      // Tính khoảng cách thời gian nghỉ (time_diff) tính bằng giây
      const lastStudyTime = oldLastStudyDate ? new Date(oldLastStudyDate).getTime() : new Date().getTime();
      const currentTime = new Date().getTime();
      const timeDiffSeconds = Math.max(0, (currentTime - lastStudyTime) / 1000);

      // Chuẩn hóa độ khó từ thang 1-5 của Android sang thang [0.1 - 0.9] khớp kt1_fixed.ipynb (0: Dễ -> 1: Khó)
      // Thang Android: 1 (Rất dễ) -> 0.1, 2 (Dễ) -> 0.3, 3 (Trung bình) -> 0.5, 4 (Khó) -> 0.7, 5 (Rất khó) -> 0.9
      let normalizedDifficulty = 0.5;
      if (typeof difficulty === "number" && !Number.isNaN(difficulty)) {
        if (difficulty > 1) {
          normalizedDifficulty = Number(
            Math.max(0.1, Math.min(0.9, 0.1 + (difficulty - 1) * 0.2)).toFixed(2)
          );
        } else if (difficulty >= 0) {
          normalizedDifficulty = Number(
            Math.max(0.05, Math.min(0.95, difficulty)).toFixed(2)
          );
        }
      }

      // Tính user_history_acc, rolling_acc_5 và tổng số câu thực tế từ bảng History
      const pastHistories = await HistoryModel.find({ userId }).sort({ createdAt: -1 }).limit(50);
      let histScoreSum = 0;
      let histQuestionSum = 0;
      const recentQuestionResults = [];
      for (const h of pastHistories) {
        histScoreSum += Number(h.score) || 0;
        histQuestionSum += Number(h.totalQuestions) || 0;
        if (recentQuestionResults.length < 5 && Array.isArray(h.questionLogs)) {
          for (let qi = h.questionLogs.length - 1; qi >= 0 && recentQuestionResults.length < 5; qi--) {
            recentQuestionResults.push(h.questionLogs[qi].isCorrect ? 1 : 0);
          }
        }
      }
      const realHistoryAcc = histQuestionSum > 0 ? histScoreSum / histQuestionSum : accuracy;
      const realRollingAcc5 =
        recentQuestionResults.length > 0
          ? recentQuestionResults.reduce((a, b) => a + b, 0) / recentQuestionResults.length
          : accuracy;
      const realAttempted = histQuestionSum > 0 ? histQuestionSum : user.quizzesCount * (totalQuestions || 10);

      // Gọi Python Microservice qua xaiService (có Connection: close, timeout 8s & retry chống socket hang up)
      aiReport = await xaiService.getWeaknessPrediction({
        historyAcc: Number(realHistoryAcc.toFixed(2)),
        totalQuestions: realAttempted,
        questionDifficulty: normalizedDifficulty,
        avgTime: avg_time || avgTimeMs,
        rollingAcc5: Number(realRollingAcc5.toFixed(2)),
        timeDiff: timeDiffSeconds,
        elapsedTime: avgTimeMs,
      });
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

        // Chỉ bổ sung danh sách từ vựng cụ thể làm sai (giữ nguyên nguyên bản giải thích SHAP từ FastAPI)
        if (wrongWords && wrongWords.length > 0) {
            aiReport.xai_explanation.negative_factors.push(`Từ vựng cần ôn lại (${wrongWords.length} từ): ${wrongWords.join(", ")}`);
        } else if (aiReport.xai_explanation.positive_factors.length === 0) {
            aiReport.xai_explanation.positive_factors.push(`Không phát hiện rủi ro, phong độ đang rất ổn định!`);
        }
    }

    // Sinh lời khuyên sư phạm có kiểm soát từ số liệu SHAP + Counterfactual (SHAP-to-Text)
    try {
      aiReport.pedagogical_feedback = await aiService.generateGroundedXaiFeedback({
        predictedAccuracy: aiReport.predicted_accuracy,
        detailedShap: aiReport.xai_explanation?.detailed_shap || [],
        counterfactualSummary: aiReport.counterfactual?.summary_vi || "",
        wrongWords: wrongWords || [],
      });
    } catch (nlgErr) {
      console.warn("⚠️ Bỏ qua lỗi sinh SHAP-to-Text:", nlgErr.message);
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