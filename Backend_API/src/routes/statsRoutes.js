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

    // 1. Tạo mảng dữ liệu biểu đồ theo bộ lọc range: this_week | last_week | month (múi giờ VN UTC+7)
    const range = String(req.query.range || 'this_week');
    const histories = await HistoryModel.find({ userId }).sort({ createdAt: 1 });
    const VN_OFFSET_MS = 7 * 60 * 60 * 1000;
    const DAY_MS = 24 * 60 * 60 * 1000;

    const nowVn = new Date(Date.now() + VN_OFFSET_MS);
    const vnDayOfWeek = nowVn.getUTCDay(); // 0=CN, 1=T2..6=T7
    const daysSinceMonday = vnDayOfWeek === 0 ? 6 : vnDayOfWeek - 1;
    const todayStartVnMs = Date.UTC(nowVn.getUTCFullYear(), nowVn.getUTCMonth(), nowVn.getUTCDate());
    const thisWeekMondayVnMs = todayStartVnMs - daysSinceMonday * DAY_MS;

    let chartLabels = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'];
    let chartData = [0, 0, 0, 0, 0, 0, 0];

    if (range === 'month') {
      chartLabels = ['Tuần 1', 'Tuần 2', 'Tuần 3', 'Tuần 4'];
      chartData = [0, 0, 0, 0];
      const curYear = nowVn.getUTCFullYear();
      const curMonth = nowVn.getUTCMonth();

      histories.forEach((h) => {
        const createdMs = new Date(h.createdAt).getTime();
        if (Number.isNaN(createdMs)) return;
        const vnDate = new Date(createdMs + VN_OFFSET_MS);
        if (vnDate.getUTCFullYear() !== curYear || vnDate.getUTCMonth() !== curMonth) return;
        const dayOfMonth = vnDate.getUTCDate(); // 1..31
        const weekIdx = Math.min(3, Math.floor((dayOfMonth - 1) / 7));
        chartData[weekIdx] = Math.min(100, chartData[weekIdx] + (h.totalQuestions || 0) * 3);
      });
    } else {
      const startVnMs = range === 'last_week' ? thisWeekMondayVnMs - 7 * DAY_MS : thisWeekMondayVnMs;
      const endVnMs = startVnMs + 7 * DAY_MS;

      histories.forEach((h) => {
        const createdMs = new Date(h.createdAt).getTime();
        if (Number.isNaN(createdMs)) return;
        const hVnMs = createdMs + VN_OFFSET_MS;
        if (hVnMs < startVnMs || hVnMs >= endVnMs) return;
        const vnDate = new Date(hVnMs);
        let dayIndex = vnDate.getUTCDay() - 1;
        if (dayIndex === -1) dayIndex = 6;
        chartData[dayIndex] = Math.min(100, chartData[dayIndex] + (h.totalQuestions || 0) * 5);
      });
    }

    // 2. TÍNH TOÁN TIẾN ĐỘ TỪ VỰNG CÁ NHÂN HÓA THEO user.vocabProgress
    const totalWordsCount = await VocabularyModel.countDocuments();

    // Đếm số từ đã thành thạo (>= 80) trong tiến độ riêng của học viên hiện tại
    const userProgress = user.vocabProgress || [];
    const masteredCount = userProgress.filter(v => (v.proficiency || 0) >= 80).length;

    // Tính phần trăm thành thạo cá nhân
    const masteryPercent = totalWordsCount > 0 ? Math.round((masteredCount / totalWordsCount) * 100) : 0;

    // Tính độ chính xác thực tế từ lịch sử làm Quiz (fallback sang % thành thạo nếu chưa làm quiz)
    let totalQuizScore = 0;
    let totalQuizQuestions = 0;
    histories.forEach(h => {
      totalQuizScore += Number(h.score) || 0;
      totalQuizQuestions += Number(h.totalQuestions) || 0;
    });
    const realAccuracy = totalQuizQuestions > 0
      ? Math.min(100, Math.round((totalQuizScore / totalQuizQuestions) * 100))
      : masteryPercent;

    // Chặn hiển thị số giờ bất thường nếu DB chưa chạy script dọn dữ liệu cũ
    const rawMinutes = Number(user.timeLearnedMinutes) || 0;
    const safeMinutes = rawMinutes > 100000 ? 0 : Math.max(0, rawMinutes);

    // 3. TRẢ VỀ JSON CHO ANDROID
    res.status(200).json({
      stats: {
        streakDays: user.streakDays || 0,
        totalHours: Math.round(safeMinutes / 60),
        totalWords: masteredCount,
        accuracyPercent: realAccuracy,
        chartData: chartData,
        chartLabels: chartLabels,
        chartRange: range
      },
      memories: (() => {
        const list = [];
        list.push({
          date: "Hôm nay",
          title: `🔥 Chuỗi ${user.streakDays || 0} ngày học liên tiếp`,
          description: `Bạn đang ở Cấp ${user.level || 1} với ${(user.xp || 0).toLocaleString()} XP tích lũy.`,
          xp: user.xp || 0,
        });
        if (masteredCount > 0) {
          list.push({
            date: "Thành tích từ vựng",
            title: `📚 Chinh phục ${masteredCount} từ vựng thành thạo`,
            description: `Độ phủ vốn từ đạt ${masteryPercent}% trên tổng kho ${totalWordsCount} từ.`,
            xp: masteredCount * 5,
          });
        }
        const recentQuizzes = histories.slice(-3).reverse();
        recentQuizzes.forEach((h) => {
          const dStr = new Date(new Date(h.createdAt).getTime() + VN_OFFSET_MS)
            .toISOString()
            .split("T")[0]
            .split("-")
            .reverse()
            .join("/");
          const pct = h.totalQuestions > 0 ? Math.round(((h.score || 0) / h.totalQuestions) * 100) : 0;
          list.push({
            date: dStr,
            title: `🎯 Hoàn thành: ${h.topic || "Bài luyện tập Quiz"}`,
            description: `Đạt ${h.score || 0}/${h.totalQuestions || 0} câu đúng (${pct}%).`,
            xp: (h.score || 0) * 10,
          });
        });
        return list;
      })()
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