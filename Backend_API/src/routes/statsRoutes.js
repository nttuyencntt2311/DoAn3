// File: routes/statsRoutes.js
const express = require('express');
const UserModel = require('../models/User');
const HistoryModel = require('../models/History');
const VocabularyModel = require('../models/Vocabulary'); 
const verifyToken = require('../middleware/authMiddleware');
const requireAdmin = require('../middleware/requireAdmin');
const xaiService = require('../service/xaiService');

const router = express.Router();

// =====================================
// API 1: LẤY THỐNG KÊ CHI TIẾT HỌC TẬP (Dành cho App Mobile)
// =====================================
router.get('/learning', verifyToken, async (req, res) => {
  try {
    const userId = req.user.userId;

    const user = await UserModel.findById(userId);
    if (!user) return res.status(404).json({ error: "Không tìm thấy người dùng" });

    // 1. Tạo mảng dữ liệu biểu đồ 7 ngày qua từ bảng History
    const histories = await HistoryModel.find({ userId }).sort({ createdAt: 1 });
    let chartData = [0, 0, 0, 0, 0, 0, 0]; 
    histories.forEach(h => {
      const date = new Date(h.createdAt);
      let dayIndex = date.getDay() - 1; // 0 là CN, 1 là T2...
      if (dayIndex === -1) dayIndex = 6; // Đổi CN xuống cuối mảng (Index 6)
      
      chartData[dayIndex] += (h.totalQuestions || 0) * 5; 
      if (chartData[dayIndex] > 100) chartData[dayIndex] = 100; // Tối đa 100% cột
    });

    // 2. TÍNH TOÁN TIẾN ĐỘ TỪ VỰNG CÁ NHÂN HÓA THEO user.vocabProgress
    const totalWordsCount = await VocabularyModel.countDocuments();

    // Đếm số từ đã thành thạo (>= 80) trong tiến độ riêng của học viên hiện tại
    const userProgress = user.vocabProgress || [];
    const masteredCount = userProgress.filter(v => (v.proficiency || 0) >= 80).length;

    // Tính phần trăm thành thạo cá nhân
    const masteryPercent = totalWordsCount > 0 ? Math.round((masteredCount / totalWordsCount) * 100) : 0;

    // 3. TRẢ VỀ JSON CHO ANDROID
    res.status(200).json({
      stats: {
        streakDays: user.streakDays || 0,
        totalHours: Math.round((user.timeLearnedMinutes || 0) / 60), 
        totalWords: totalWordsCount,          
        accuracyPercent: masteryPercent,      
        chartData: chartData
      },
      memories: [
        {
          date: "Hôm nay",
          title: `🔥 Chuỗi ${user.streakDays || 0} ngày rực cháy`,
          description: `Bạn đã duy trì thói quen học tiếng Anh liên tiếp chuẩn chỉ!`,
          xp: user.xp || 0
        }
      ]
    });

  } catch (error) {
    console.error("Lỗi lấy thống kê:", error);
    res.status(500).json({ error: "Lỗi hệ thống khi lấy thống kê học tập!" });
  }
});


// =====================================
// API 2: LẤY TỔNG QUAN HỆ THỐNG (Dành cho Web Admin)
// =====================================
router.get('/dashboard', verifyToken, requireAdmin, async (req, res) => {
  try {
    // 1. Đếm tổng số từ vựng trong hệ thống
    const totalVocab = await VocabularyModel.countDocuments();

    // 2. Đếm tổng số học viên trong hệ thống
    const totalUsers = await UserModel.countDocuments();

    // 3. Lấy 4 người dùng đăng ký mới nhất làm danh sách "Hoạt động gần đây"
    const newestUsers = await UserModel.find()
      .sort({ createdAt: -1 }) // -1 là xếp ngày mới nhất lên đầu
      .limit(4);

    const recentActivities = newestUsers.map(user => ({
      id: user._id,
      user: user.name || "Học viên mới",
      action: "vừa đăng ký tài khoản mới",
      time: new Date(user.createdAt).toLocaleDateString('vi-VN') 
    }));

    // Gửi JSON về cho Frontend React
    res.status(200).json({
      totalVocab,
      totalUsers,
      recentActivities
    });

  } catch (error) {
    console.error("Lỗi lấy dữ liệu Dashboard:", error);
    res.status(500).json({ message: "Lỗi Server khi lấy dữ liệu Dashboard" });
  }
});

// =====================================
// API 3: GLOBAL XAI ANALYTICS DASHBOARD (Dành cho Web Admin)
// =====================================
router.get('/xai-analytics', verifyToken, requireAdmin, async (req, res) => {
  try {
    const students = await UserModel.find({ role: { $ne: 'admin' } })
      .select('name email xp level quizzesCount timeLearnedMinutes lastStudyDate vocabProgress')
      .limit(100);

    const batchPayload = students.map((u) => {
      const attempted = (u.quizzesCount || 0) * 5;
      const progress = u.vocabProgress || [];
      const mastered = progress.filter((p) => (p.proficiency || 0) >= 80).length;
      const histAcc = progress.length > 0
        ? Math.max(0.1, Math.min(0.95, mastered / progress.length))
        : (u.level ? Math.min(0.2 + u.level * 0.1, 0.85) : 0.45);
      const avgTimeMs = u.timeLearnedMinutes > 0 && attempted > 0
        ? Math.min(120000, Math.max(5000, Math.round((u.timeLearnedMinutes * 60 * 1000) / attempted)))
        : 23000;
      const timeDiffSec = u.lastStudyDate
        ? Math.max(0, (Date.now() - new Date(u.lastStudyDate).getTime()) / 1000)
        : 86400 * 3;

      return {
        user_id: u._id.toString(),
        name: u.name || 'Học viên',
        email: u.email || '',
        user_history_acc: Number(histAcc.toFixed(2)),
        user_questions_attempted: attempted,
        question_difficulty: 0.5,
        rolling_acc_5: Number(histAcc.toFixed(2)),
        user_avg_elapsed_time: avgTimeMs,
        time_diff: timeDiffSec,
        elapsed_time: avgTimeMs,
      };
    });

    const [globalXai, riskAnalysis] = await Promise.all([
      xaiService.getGlobalXai(),
      xaiService.getBatchRiskAnalysis(batchPayload),
    ]);

    res.status(200).json({
      serviceOnline: Boolean(globalXai && riskAnalysis),
      globalImportance: globalXai?.global_importance || [
        { feature: 'question_difficulty', vi_description: 'Độ khó câu hỏi (0: Dễ → 1: Khó)', importance_percent: 34.2 },
        { feature: 'user_history_acc', vi_description: 'Tỉ lệ làm đúng lịch sử', importance_percent: 26.8 },
        { feature: 'rolling_acc_5', vi_description: 'Phong độ 5 câu gần nhất', importance_percent: 16.5 },
        { feature: 'elapsed_time', vi_description: 'Thời gian suy nghĩ bài này (ms)', importance_percent: 9.4 },
        { feature: 'log_time_diff', vi_description: 'Khoảng cách giữa các phiên học', importance_percent: 6.7 },
        { feature: 'user_avg_elapsed_time', vi_description: 'Tốc độ suy nghĩ trung bình (ms)', importance_percent: 4.1 },
        { feature: 'user_questions_attempted', vi_description: 'Tổng số câu hỏi đã luyện tập', importance_percent: 2.3 },
      ],
      summary: {
        studentsAnalyzed: riskAnalysis?.students_analyzed ?? batchPayload.length,
        atRiskCount: riskAnalysis?.at_risk_count ?? 0,
        avgPredictedAccuracy: riskAnalysis?.avg_predicted_accuracy ?? 0,
      },
      atRiskStudents: riskAnalysis?.students || [],
    });
  } catch (error) {
    console.error('Lỗi lấy dữ liệu Global XAI:', error);
    res.status(500).json({ message: 'Lỗi Server khi tổng hợp dữ liệu XAI Dashboard' });
  }
});

module.exports = router;