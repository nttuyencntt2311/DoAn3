const aiService = require("../service/aiService");
const ChatSession = require("../models/ChatSession");
const ChatMessage = require("../models/ChatMessage");
const Roadmap = require("../models/Roadmap");
const User = require("../models/User");
const Vocabulary = require("../models/Vocabulary");
const xaiService = require("../service/xaiService");
const History = require("../models/History");

// ==========================================
// MOCK DATA: PHAO CỨU SINH KHI DEMO BỊ LỖI MẠNG / QUÁ TẢI API
// ==========================================
const MOCK_QUIZ_FALLBACK = [
  {
    question:
      "The manager decided to ___ the meeting until next week due to the CEO's absence.",
    options: ["put off", "call off", "take over", "bring up"],
    correctAnswer: "put off",
    pronounceText: "put off",
    hint: "Quản lý quyết định hoãn cuộc họp cho đến tuần sau do sự vắng mặt của CEO.",
    ai_advice: "Cụm từ nào đi với 'off' mang ý nghĩa là trì hoãn?",
    is_high_risk: false,
    reward_multiplier: 1,
  },
  {
    question:
      "Despite the heavy rain, the outdoor concert went ___ as planned.",
    options: ["ahead", "forward", "through", "over"],
    correctAnswer: "ahead",
    pronounceText: "ahead",
    hint: "Mặc dù trời mưa to, buổi hòa nhạc ngoài trời vẫn diễn ra như dự kiến.",
    ai_advice: "Cụm 'go ___' có nghĩa là tiến hành, tiếp tục diễn ra.",
    is_high_risk: true,
    reward_multiplier: 3,
    challenge_message:
      "🔥 THỬ THÁCH DEMO XAI: Câu này khá dễ nhầm lẫn. Trả lời đúng nhận x3 XP!",
  },
  {
    question:
      "It's crucial for the team to ___ the root cause of the system failure.",
    options: ["figure out", "make up", "turn down", "look after"],
    correctAnswer: "figure out",
    pronounceText: "figure out",
    hint: "Điều quan trọng là đội ngũ phải tìm ra nguyên nhân gốc rễ của lỗi hệ thống.",
    ai_advice: "Động từ nào miêu tả hành động suy nghĩ để hiểu ra vấn đề?",
    is_high_risk: false,
    reward_multiplier: 1,
  },
  {
    question:
      "The new marketing strategy had a significant ___ on our overall sales this quarter.",
    options: ["impact", "result", "affect", "consequence"],
    correctAnswer: "impact",
    pronounceText: "impact",
    hint: "Chiến lược tiếp thị mới đã có tác động đáng kể đến tổng doanh số bán hàng quý này.",
    ai_advice: "Chú ý từ 'on' phía sau, danh từ nào thường đi kèm với 'on'?",
    is_high_risk: false,
    reward_multiplier: 1,
  },
  {
    question:
      "If you don't ___ to the new company policies quickly, you might struggle to fit in.",
    options: ["adapt", "adopt", "adjust", "accept"],
    correctAnswer: "adapt",
    pronounceText: "adapt",
    hint: "Nếu bạn không thích nghi với các chính sách mới của công ty nhanh chóng, bạn có thể sẽ gặp khó khăn để hòa nhập.",
    ai_advice:
      "Từ nào diễn tả việc thay đổi bản thân để phù hợp với môi trường?",
    is_high_risk: false,
    reward_multiplier: 1,
  },
];

// Hàm tiện ích: Tạm dừng (delay) dùng cho Retry
const delay = (ms) => new Promise((res) => setTimeout(res, ms));

// ==========================================
// 1. TẠO PHIÊN CHAT MỚI (Mở phòng chat — Bảo mật theo JWT)
// ==========================================
exports.createChatSession = async (req, res) => {
  try {
    const authUserId = req.user.userId || req.user.id || req.user._id;
    const { chatType, firstMessage } = req.body;
    const title = firstMessage
      ? firstMessage.substring(0, 20) + "..."
      : "Cuộc trò chuyện mới";
    const newSession = new ChatSession({ userId: authUserId, chatType, title });
    await newSession.save();
    res
      .status(201)
      .json({ sessionId: newSession._id, title: newSession.title });
  } catch (error) {
    console.error("Lỗi tạo phiên chat:", error);
    res.status(500).json({ error: "Lỗi khi tạo phiên chat trên hệ thống!" });
  }
};

// ==========================================
// 2. GỬI TIN NHẮN & LƯU DB (Chống IDOR)
// ==========================================
exports.chatWithAI = async (req, res) => {
  try {
    const authUserId = req.user.userId || req.user.id || req.user._id;
    const { sessionId, message } = req.body;
    if (!message)
      return res.status(400).json({ error: "Tin nhắn không được để trống!" });

    let chatHistory = [];
    if (sessionId) {
      const session = await ChatSession.findOne({ _id: sessionId, userId: authUserId });
      if (!session) {
        return res.status(403).json({ error: "Bạn không có quyền truy cập phiên chat này!" });
      }
      const rawHistory = await ChatMessage.find({ sessionId })
        .sort({ createdAt: -1 })
        .limit(10);
      chatHistory = rawHistory.reverse();
      const userMsg = new ChatMessage({
        sessionId,
        role: "user",
        text: message,
      });
      await userMsg.save();
      await ChatSession.findByIdAndUpdate(sessionId, { updatedAt: new Date() });
    }

    // Tổng hợp ngữ cảnh XAI của học viên để Gia sư Chat cá nhân hóa ví dụ & tư vấn
    let xaiContext = null;
    try {
      const [user, recentHistories] = await Promise.all([
        User.findById(authUserId).select("name level streakDays quizzesCount timeLearnedMinutes lastStudyDate vocabProgress"),
        History.find({ userId: String(authUserId) }).sort({ createdAt: -1 }).limit(10),
      ]);

      if (user) {
        const progress = user.vocabProgress || [];
        const weakEntries = [...progress]
          .filter((p) => (p.proficiency || 0) < 80 || (p.mistakeCount || 0) > 0)
          .sort((a, b) => (b.mistakeCount || 0) - (a.mistakeCount || 0) || (a.proficiency || 0) - (b.proficiency || 0))
          .slice(0, 6);

        const weakWords = [];
        for (const item of weakEntries) {
          let wordStr = "";
          if (item.word && !/^[0-9a-fA-F]{24}$/.test(String(item.word))) {
            wordStr = String(item.word).trim();
          } else if (item.wordId && /^[0-9a-fA-F]{24}$/.test(String(item.wordId))) {
            const vDoc = await Vocabulary.findById(item.wordId).select("word").catch(() => null);
            if (vDoc?.word) wordStr = vDoc.word;
          }
          if (wordStr && !weakWords.includes(wordStr)) weakWords.push(wordStr);
        }

        let scoreSum = 0;
        let qSum = 0;
        let elapsedSum = 0;
        let elapsedCount = 0;
        for (const h of recentHistories) {
          scoreSum += Number(h.score) || 0;
          qSum += Number(h.totalQuestions) || 0;
          if (h.avgElapsedTimeMs > 0) {
            elapsedSum += Number(h.avgElapsedTimeMs);
            elapsedCount++;
          }
        }
        const histAcc = qSum > 0 ? scoreSum / qSum : 0.5;
        const avgElapsedMs = elapsedCount > 0 ? Math.round(elapsedSum / elapsedCount) : 23000;
        const timeDiffSec = user.lastStudyDate
          ? Math.max(0, (Date.now() - new Date(user.lastStudyDate).getTime()) / 1000)
          : 86400;

        let shapSummary = null;
        const asksPersonalXai = /yếu|năng lực|điểm|xai|shap|ôn gì|lộ trình|tư vấn|đánh giá|kết quả|tiến bộ|yeu|nang luc|on gi|tu van|danh gia/i.test(message);
        if (asksPersonalXai) {
          shapSummary = await xaiService.getWeaknessPrediction({
            historyAcc: Number(histAcc.toFixed(2)),
            totalQuestions: qSum || (user.quizzesCount || 0) * 5,
            questionDifficulty: 0.5,
            avgTime: avgElapsedMs,
            rollingAcc5: Number(histAcc.toFixed(2)),
            timeDiff: timeDiffSec,
            elapsedTime: avgElapsedMs,
          });
        }

        xaiContext = {
          userName: user.name || "Học viên",
          level: user.level || 1,
          streakDays: user.streakDays || 0,
          accuracyPercent: Math.round(histAcc * 100),
          avgThinkingSec: Number((avgElapsedMs / 1000).toFixed(1)),
          weakWords: weakWords.slice(0, 5),
          predictedAccuracy: shapSummary?.predicted_accuracy ?? null,
          negativeFactors: shapSummary?.xai_explanation?.negative_factors || [],
          counterfactual: shapSummary?.counterfactual?.summary_vi || "",
        };
      }
    } catch (ctxErr) {
      console.warn("⚠️ Bỏ qua lỗi nạp ngữ cảnh XAI cho Chat:", ctxErr.message);
    }

    const aiReply = await aiService.generateChatReply(message, chatHistory, xaiContext);

    if (sessionId) {
      const aiMsg = new ChatMessage({ sessionId, role: "ai", text: aiReply });
      await aiMsg.save();
    }
    res.status(200).json({ reply: aiReply });
  } catch (error) {
    console.error("Lỗi chi tiết từ Google Gemini/Groq:", error);
    res.status(500).json({ error: `Lỗi từ AI: ${error.message}` });
  }
};

// ==========================================
// 3. LẤY LẠI LỊCH SỬ TIN NHẮN CŨ (Chống IDOR)
// ==========================================
exports.getChatHistory = async (req, res) => {
  try {
    const authUserId = req.user.userId || req.user.id || req.user._id;
    const { sessionId } = req.params;
    const session = await ChatSession.findOne({ _id: sessionId, userId: authUserId });
    if (!session) {
      return res.status(403).json({ error: "Bạn không có quyền xem lịch sử phiên chat này!" });
    }
    const messages = await ChatMessage.find({ sessionId }).sort({
      createdAt: 1,
    });
    res.status(200).json({ messages });
  } catch (error) {
    console.error("Lỗi lấy lịch sử:", error);
    res.status(500).json({ error: "Lỗi khi lấy lịch sử chat" });
  }
};

// ==========================================
// 4. LẤY DANH SÁCH CÁC PHÒNG CHAT CỦA USER (Chống IDOR)
// ==========================================
exports.getUserChatSessions = async (req, res) => {
  try {
    const authUserId = String(req.user.userId || req.user.id || req.user._id);
    const { userId } = req.params;
    if (userId && String(userId) !== authUserId) {
      return res.status(403).json({ error: "Không được phép xem danh sách phòng chat của người khác!" });
    }
    const sessions = await ChatSession.find({ userId: authUserId }).sort({ updatedAt: -1 });
    res.status(200).json({ sessions });
  } catch (error) {
    console.error("Lỗi lấy danh sách phòng chat:", error);
    res
      .status(500)
      .json({ error: "Lỗi máy chủ khi lấy danh sách phòng chat!" });
  }
};

// ==========================================
// 5. TẠO LỘ TRÌNH MỚI BẰNG AI (ĐÃ CÓ PHAO CỨU SINH CHỐNG LỖI 503)
// ==========================================
exports.generateRoadmap = async (req, res) => {
  try {
    const userId = req.user.id || req.user.userId || req.user._id;
    const { target, currentLevel, freeTime, expectation } = req.body;

    // 🚀 TỔNG HỢP HỒ SƠ HÀNH VI & PHÂN TÍCH XAI (SHAP) TRƯỚC KHI SINH LỘ TRÌNH
    let xaiContext = null;
    try {
      const user = await User.findById(userId);
      if (user) {
        const weakEntries = (user.vocabProgress || []).filter((p) => (p.proficiency || 0) < 80);
        const weakIds = weakEntries.map((p) => p.wordId).filter((id) => id && id.toString().length === 24);
        const weakVocabs = weakIds.length > 0 ? await Vocabulary.find({ _id: { $in: weakIds } }).limit(20) : [];

        const weakWords = weakVocabs.map((v) => v.word);
        const IGNORED_CATS = new Set(["tất cả", "tat ca", "tổng hợp", "tong hop", "all", ""]);
        const catCounts = {};
        weakVocabs.forEach((v) => {
          const cat = (v.category || "").trim();
          if (cat && !IGNORED_CATS.has(cat.toLowerCase())) {
            catCounts[cat] = (catCounts[cat] || 0) + 1;
          }
        });
        const weakCategories = Object.entries(catCounts)
          .sort((a, b) => b[1] - a[1])
          .slice(0, 3)
          .map((e) => e[0]);

        const histAcc = user.level ? Math.min(0.2 + user.level * 0.1, 0.9) : 0.5;
        const attempted = (user.quizzesCount || 0) * 5;
        const avgTimeMs =
          user.timeLearnedMinutes > 0 && attempted > 0
            ? Math.min(120000, Math.max(5000, Math.round((user.timeLearnedMinutes * 60 * 1000) / attempted)))
            : 23000.0;
        const timeDiffSec = user.lastStudyDate
          ? Math.max(0, (Date.now() - new Date(user.lastStudyDate).getTime()) / 1000)
          : 0.0;

        const shapEval = await xaiService.getWeaknessPrediction({
          historyAcc: histAcc,
          totalQuestions: attempted,
          questionDifficulty: 0.5,
          avgTime: avgTimeMs,
          rollingAcc5: histAcc,
          timeDiff: timeDiffSec,
          elapsedTime: avgTimeMs,
        });

        xaiContext = {
          userName: user.name || "Học viên",
          level: user.level || 1,
          streakDays: user.streakDays || 0,
          expectation: (expectation || "").trim(),
          predictedAccuracy: shapEval?.predicted_accuracy ?? Math.round(histAcc * 100),
          weakCategories,
          weakWords,
          negativeFactors: shapEval?.xai_explanation?.negative_factors || [],
        };
      }
    } catch (ctxErr) {
      console.warn("⚠️ Không thể nạp ngữ cảnh XAI cho Roadmap:", ctxErr.message);
    }

    let aiRoadmap;
    try {
      // Gọi Gemini sinh lộ trình cá nhân hóa dựa trên hồ sơ XAI + SHAP
      aiRoadmap = await aiService.generateRoadmapJSON(
        target,
        currentLevel,
        freeTime,
        xaiContext,
      );
    } catch (aiError) {
      console.log(
        "⚠️ [AI QUÁ TẢI 503]: Đang kích hoạt Lộ trình dự phòng có giải thích XAI...",
      );
      const topWeakCat = xaiContext?.weakCategories?.[0] || target || "Giao tiếp";
      const topRisk = xaiContext?.negativeFactors?.[0] || "cần củng cố nền tảng từ vựng đều đặn";
      aiRoadmap = {
        motivationMessage:
          "🚀 Lộ trình XAI thiết kế riêng dựa trên hồ sơ hành vi học tập của bạn!",
        steps: [
          {
            title: `Chặng 1: Củng cố ${topWeakCat}`,
            description: `Ôn tập trọng tâm nhóm từ hay nhầm lẫn cho mục tiêu ${target || "Giao tiếp"}.`,
            xaiReason: `AI ưu tiên chặng này vì phân tích XAI ghi nhận: ${topRisk}.`,
            maxProgress: 30,
          },
          {
            title: "Chặng 2: Thực chiến ngữ cảnh",
            description: `Luyện tập phản xạ câu hỏi phù hợp quỹ thời gian (${freeTime || "30 phút/ngày"}).`,
            xaiReason: "Giúp ổn định tốc độ suy nghĩ và nâng phong độ 5 câu gần nhất.",
            maxProgress: 40,
          },
          {
            title: "Chặng 3: Chuyên sâu & Bứt phá",
            description: "Hoàn thiện kỹ năng nâng cao và kiểm tra tổng lực.",
            xaiReason: `Đưa xác suất làm đúng vượt ngưỡng 80% cho mục tiêu ${target || "đã chọn"}.`,
            maxProgress: 50,
          },
        ],
      };
    }

    const customMotivation = expectation
      ? `🎯 Mong muốn: "${expectation}". ${aiRoadmap.motivationMessage}`
      : aiRoadmap.motivationMessage;

    const steps = aiRoadmap.steps.map((s, index) => ({
      id: (index + 1).toString(),
      title: s.title,
      description: s.description,
      xaiReason: s.xaiReason || "Đề xuất dựa trên mục tiêu và tiến độ học tập cá nhân của bạn.",
      status: index === 0 ? "CURRENT" : "LOCKED",
      currentProgress: 0,
      maxProgress: s.maxProgress || 30,
    }));

    // Tự động gỡ unique index cũ trên userId (nếu còn sót từ schema cũ) để lưu được nhiều lộ trình
    try {
      await Roadmap.collection.dropIndex("userId_1");
    } catch (_) {}

    const cleanTopic = (target || "Lộ trình cá nhân").replace(/^Chủ đề:\s*/i, "").trim();

    // Đánh dấu các lộ trình cũ là không active (vẫn giữ nguyên tiến độ trong DB)
    await Roadmap.updateMany({ userId: userId }, { $set: { isActive: false } });

    const updatedRoadmap = await Roadmap.create({
      userId: userId,
      topic: cleanTopic,
      target: target || "",
      currentLevel: currentLevel || "",
      isActive: true,
      overallProgressPercent: 0,
      motivationMessage: customMotivation,
      steps: steps,
    });

    const allUserRoadmaps = await Roadmap.find({ userId: userId }).sort({ updatedAt: -1 }).limit(10);

    res.status(200).json({
      id: updatedRoadmap._id,
      userId: updatedRoadmap.userId,
      topic: updatedRoadmap.topic,
      overallProgressPercent: updatedRoadmap.overallProgressPercent,
      motivationMessage: updatedRoadmap.motivationMessage,
      steps: updatedRoadmap.steps,
      savedRoadmaps: allUserRoadmaps.map((r) => ({
        id: r._id,
        topic: r.topic || "Lộ trình cá nhân",
        overallProgressPercent: r.overallProgressPercent || 0,
        isActive: Boolean(r.isActive),
      })),
    });
  } catch (error) {
    console.error("Lỗi khi gen Roadmap:", error);
    res.status(500).json({ error: "Lỗi hệ thống khi khởi tạo lộ trình!" });
  }
};

// ==========================================
// 6. LẤY LỘ TRÌNH HIỆN TẠI CỦA USER
// ==========================================
function getWeeklyProgressFromUser(user) {
  if (!user) {
    return { weeklyLessonsCompleted: 0, weeklyGoalTarget: 5, isWeeklyRewardClaimed: false, streakDays: 0 };
  }
  const now = new Date();
  const lastDate = user.lastLessonDate ? new Date(user.lastLessonDate) : new Date(0);
  const diffDays = (now - lastDate) / (1000 * 60 * 60 * 24);
  const isSameWeek = diffDays < 7 && now.getDay() >= lastDate.getDay();
  return {
    weeklyLessonsCompleted: isSameWeek ? Math.min(5, user.weeklyLessonsCompleted || 0) : 0,
    weeklyGoalTarget: 5,
    isWeeklyRewardClaimed: isSameWeek ? Boolean(user.isWeeklyRewardClaimed) : false,
    streakDays: user.streakDays || 0,
  };
}

exports.getMyRoadmap = async (req, res) => {
  try {
    const userId = req.user.id || req.user.userId || req.user._id;
    const [allRoadmaps, user] = await Promise.all([
      Roadmap.find({ userId: userId }).sort({ isActive: -1, updatedAt: -1 }).limit(10),
      User.findById(userId).select("streakDays weeklyLessonsCompleted isWeeklyRewardClaimed lastLessonDate"),
    ]);
    const weeklyInfo = getWeeklyProgressFromUser(user);
    const roadmap = allRoadmaps.find((r) => r.isActive) || allRoadmaps[0] || null;

    if (!roadmap) {
      return res.status(200).json({
        userId: userId,
        topic: "",
        overallProgressPercent: 0,
        motivationMessage:
          "Bạn chưa có lộ trình. Hãy để AI thiết kế cho bạn nhé!",
        steps: [],
        savedRoadmaps: [],
        ...weeklyInfo,
      });
    }
    res.status(200).json({
      id: roadmap._id,
      userId: roadmap.userId,
      topic: roadmap.topic || "Lộ trình cá nhân",
      overallProgressPercent: roadmap.overallProgressPercent,
      motivationMessage: roadmap.motivationMessage,
      ...weeklyInfo,
      steps: roadmap.steps.map((s) => ({
        id: s.id,
        title: s.title,
        description: s.description,
        xaiReason: s.xaiReason || "",
        status: s.status,
        currentProgress: s.currentProgress,
        maxProgress: s.maxProgress,
      })),
      savedRoadmaps: allRoadmaps.map((r) => ({
        id: r._id,
        topic: r.topic || "Lộ trình cá nhân",
        overallProgressPercent: r.overallProgressPercent || 0,
        isActive: r._id.toString() === roadmap._id.toString(),
      })),
    });
  } catch (error) {
    console.error("Lỗi khi lấy Roadmap:", error);
    res.status(500).json({ error: error.message });
  }
};

exports.activateRoadmap = async (req, res) => {
  try {
    const userId = req.user.id || req.user.userId || req.user._id;
    const { roadmapId } = req.params;
    const targetMap = await Roadmap.findOne({ _id: roadmapId, userId });
    if (!targetMap) {
      return res.status(404).json({ error: "Không tìm thấy lộ trình đã lưu!" });
    }
    await Roadmap.updateMany({ userId }, { $set: { isActive: false } });
    targetMap.isActive = true;
    await targetMap.save();

    const allRoadmaps = await Roadmap.find({ userId }).sort({ isActive: -1, updatedAt: -1 }).limit(10);
    res.status(200).json({
      id: targetMap._id,
      userId: targetMap.userId,
      topic: targetMap.topic || "Lộ trình cá nhân",
      overallProgressPercent: targetMap.overallProgressPercent,
      motivationMessage: targetMap.motivationMessage,
      steps: targetMap.steps,
      savedRoadmaps: allRoadmaps.map((r) => ({
        id: r._id,
        topic: r.topic || "Lộ trình cá nhân",
        overallProgressPercent: r.overallProgressPercent || 0,
        isActive: r._id.toString() === targetMap._id.toString(),
      })),
    });
  } catch (error) {
    console.error("Lỗi activateRoadmap:", error);
    res.status(500).json({ error: "Không thể chuyển đổi lộ trình" });
  }
};

// ==========================================
// 7. AI SINH BÀI TẬP TRẮC NGHIỆM TỐI ƯU HÓA (KHÔNG BỊ LỖI 429)
// ==========================================
exports.generateStepQuiz = async (req, res) => {
  try {
    console.log("\n=== BẮT ĐẦU TIẾN TRÌNH TẠO QUIZ ===");

    const userId = req.user?.id || req.user?.userId || req.user?._id;
    if (!userId) throw new Error("Không lấy được ID người dùng từ Token!");

    const user = await User.findById(userId);
    const userHistoryAcc =
      user && user.level ? Math.min(0.2 + user.level * 0.1, 0.9) : 0.5;
    const userAttempted = user ? (user.quizzesCount || 0) * 5 : 0;
    console.log(
      `✅ [1/4] Đã nạp dữ liệu User. Tỉ lệ đúng: ${userHistoryAcc}, Đã làm: ${userAttempted}`,
    );

    const { stepTitle, stepDesc, questionCount } = req.query;
    const numOfQuestions = questionCount ? parseInt(questionCount) : 5;

    if (!stepTitle)
      return res
        .status(400)
        .json({ error: "Thiếu chủ đề chặng học (stepTitle)!" });

    // CẢI TIẾN 1: Yêu cầu Gemini sinh luôn "ai_advice" ngay từ đầu để tránh phải gọi API lặp lại
    const prompt = `
Bạn là một chuyên gia khảo thí ngôn ngữ và Giám khảo IELTS/Tiếng Anh giao tiếp cấp độ B1 đến C1.
Nhiệm vụ của bạn là tạo ra ${numOfQuestions} câu hỏi trắc nghiệm (Multiple Choice) CHẤT LƯỢNG CAO, dựa trên chủ đề: "${stepTitle}" (Mô tả: ${stepDesc}).

⚠️ YÊU CẦU CHUYÊN MÔN:
1. TRỌNG TÂM: Kiểm tra "Từ vựng trong ngữ cảnh", "Cụm từ cố định", "Thành ngữ".
2. ĐÁP ÁN NHIỄU (Distractors): 3 đáp án sai phải cực kỳ thông minh, có vẻ hợp lý.
3. CÂU HỎI: Câu tiếng Anh hoàn chỉnh (10-20 từ) chứa khoảng trống "___". Không hỏi trẻ con hiển nhiên.

BẮT BUỘC trả về định dạng mảng JSON thuần với cấu trúc:
[
  {
    "question": "Câu văn có khoảng trống ___",
    "options": ["Sai 1", "Đúng", "Sai 2", "Sai 3"],
    "correctAnswer": "Đáp án đúng",
    "pronounceText": "Từ vựng chính xuất hiện trong đáp án đúng",
    "hint": "Dịch nghĩa tiếng Việt nguyên câu",
    "ai_advice": "Đóng vai gia sư, viết 1 câu gợi ý/mẹo bằng tiếng Việt (dưới 15 chữ) giúp học viên tự suy luận ra đáp án, KHÔNG nói thẳng đáp án."
  }
]`;

    const jsonModel = aiService.genAI?.getGenerativeModel
      ? aiService.genAI.getGenerativeModel({
          model: "gemini-2.5-flash",
          generationConfig: { responseMimeType: "application/json" },
        })
      : null;

    if (!jsonModel) throw new Error("Không tìm thấy cấu hình Google AI!");

    console.log(
      `⏳ [2/4] Đang nhờ Gemini tạo ${numOfQuestions} câu hỏi (Kèm luôn gợi ý Gia sư)...`,
    );

    // CẢI TIẾN 2: Cơ chế Tự động thử lại (Retry)
    let result = null;
    let responseText = "";
    let maxRetries = 3;

    for (let attempt = 1; attempt <= maxRetries; attempt++) {
      try {
        result = await jsonModel.generateContent(prompt);
        responseText = result.response.text();
        break; // Nếu gọi thành công thì thoát vòng lặp ngay
      } catch (err) {
        if (
          (err.message.includes("503") || err.message.includes("429")) &&
          attempt < maxRetries
        ) {
          console.log(
            `[Gemini API] Quá tải. Đang thử lại lần ${attempt + 1}...`,
          );
          await delay(2000);
        } else {
          throw err; // Bắn lỗi ra ngoài để kích hoạt Mock Data
        }
      }
    }

    const jsonMatch = responseText.match(/\[[\s\S]*\]/);
    if (!jsonMatch)
      throw new Error("AI không trả về định dạng mảng JSON hợp lệ!");

    let quizData = JSON.parse(jsonMatch[0]);
    console.log(
      `✅ [3/4] Parse JSON thành công! Đã tạo được ${quizData.length} câu hỏi.`,
    );

 // ==========================================================
    // CẢI TIẾN 3: XAI chỉ xử lý logic số liệu, không gọi AI text nữa -> KHÔNG BAO GIỜ DÍNH LỖI 429
    // ==========================================================
    console.log(`⏳ [4/4] Bắt đầu gọi Python XAI phân tích độ khó...`);

    // Tính tốc độ suy nghĩ trung bình thực tế từ lịch sử user (tránh truyền 0 gây lỗi Out-of-Distribution)
    const realAvgTimeMs =
      user && user.timeLearnedMinutes > 0 && userAttempted > 0
        ? Math.min(120000, Math.max(5000, Math.round((user.timeLearnedMinutes * 60 * 1000) / userAttempted)))
        : 23000.0;
    const realTimeDiffSec =
      user && user.lastStudyDate
        ? Math.max(0, (Date.now() - new Date(user.lastStudyDate).getTime()) / 1000)
        : 0.0;

    for (let i = 0; i < quizData.length; i++) {
      let questionItem = quizData[i];
      // Độ khó chuẩn hóa [0.2 -> 0.9]: câu dài/phức tạp hơn -> độ khó cao hơn (khớp question_difficulty = 1 - pass_rate)
      const simulatedDifficulty = Number(
        Math.min(0.9, Math.max(0.2, questionItem.question.length / 120)).toFixed(2)
      );

      const studentDataForML = {
        historyAcc: userHistoryAcc,
        totalQuestions: userAttempted,
        questionDifficulty: simulatedDifficulty,
        avgTime: realAvgTimeMs,
        rollingAcc5: userHistoryAcc,
        timeDiff: realTimeDiffSec,
      };

      try {
        const mlPrediction = await xaiService.getPreQuizPrediction(studentDataForML);

        if (mlPrediction) {
          const predictedAcc = mlPrediction.predicted_accuracy;
          console.log(`- Câu ${i + 1}: Python đoán trúng ${predictedAcc}% | Khó khăn: ${mlPrediction.is_struggling}`);

          // Logic Thử thách nhân phẩm
          if (predictedAcc < 20.0) { // SỬA LẠI THÀNH 20.0 ĐỂ KHỚP LOGIC "CỰC KHÓ"
            quizData[i].is_high_risk = true;
            quizData[i].reward_multiplier = 3;
            quizData[i].challenge_message = `🔥 THỬ THÁCH XAI: AI đánh giá câu này quá giới hạn của bạn (Dự đoán: ${predictedAcc}%). Trả lời đúng nhận x3 XP!`;
          } else {
            quizData[i].is_high_risk = false;
            quizData[i].reward_multiplier = 1;
          }
        }
      } catch (xaiError) {
        console.log(`⚠️ Bỏ qua lỗi XAI ở câu ${i + 1}:`, xaiError.message);
        quizData[i].is_high_risk = false;
        quizData[i].reward_multiplier = 1;
      }
    }

    console.log("✅ HOÀN TẤT! Đang gửi dữ liệu về Android.");
    return res.status(200).json(quizData);
  } catch (error) {
    // CẢI TIẾN 4: KÍCH HOẠT PHAO CỨU SINH KHI LỖI (MOCK DATA)
    console.error("\n=================================");
    console.error(
      "❌ LỖI CHÍNH KHI TẠO QUIZ (ĐÃ KÍCH HOẠT MOCK DATA CHO DEMO):",
    );
    console.error(error.message);
    console.error("=================================\n");

    // Trả về bộ câu hỏi Mock xịn sò thay vì ném lỗi 500
    return res.status(200).json(MOCK_QUIZ_FALLBACK);
  }
};

// ==========================================
// 8. CẬP NHẬT TRẠNG THÁI HOÀN THÀNH CHẶNG HỌC & CỘNG ĐIỂM
// ==========================================
exports.completeStep = async (req, res) => {
  try {
    const userId = req.user.id || req.user.userId || req.user._id;
    const { stepId, score, totalQuestions } = req.body;

    const roadmap = (await Roadmap.findOne({ userId, isActive: true })) || (await Roadmap.findOne({ userId, "steps.id": stepId })) || (await Roadmap.findOne({ userId }));
    if (!roadmap)
      return res.status(404).json({ error: "Không tìm thấy lộ trình!" });

    const stepIndex = roadmap.steps.findIndex((s) => s.id === stepId);
    if (stepIndex !== -1) {
      roadmap.steps[stepIndex].status = "COMPLETED";
      roadmap.steps[stepIndex].currentProgress =
        roadmap.steps[stepIndex].maxProgress;

      if (stepIndex + 1 < roadmap.steps.length) {
        roadmap.steps[stepIndex + 1].status = "CURRENT";
      }
      const completedCount = roadmap.steps.filter(
        (s) => s.status === "COMPLETED",
      ).length;
      roadmap.overallProgressPercent = Math.round(
        (completedCount / roadmap.steps.length) * 100,
      );
      await roadmap.save();
    }

    const scoreEarned = parseInt(score) || 0;
    const totalQ = parseInt(totalQuestions) || 0;
    const completionBonus = totalQ > 0 ? Math.round((50 * scoreEarned) / totalQ) : (scoreEarned > 0 ? 50 : 0);
    const xpEarned = completionBonus + scoreEarned * 10;

    try {
      const user = await User.findById(userId);
      if (user) {
        user.xp = (user.xp || 0) + xpEarned;
        user.level = Math.floor(Math.sqrt(Math.max(0, user.xp) / 100)) + 1;

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
          } else if (diffDays > 1 || diffDays < 0) {
            user.streakDays = 1;
          }
        }
        user.lastStudyDate = new Date();

        const now = new Date();
        const lastDate = user.lastLessonDate || new Date(0);
        const isSameWeek =
          (now - lastDate) / (1000 * 60 * 60 * 24) < 7 &&
          now.getDay() >= lastDate.getDay();

        if (!isSameWeek) {
          user.weeklyLessonsCompleted = 1;
          user.isWeeklyRewardClaimed = false;
        } else {
          if (user.weeklyLessonsCompleted < 5) {
            user.weeklyLessonsCompleted += 1;
          }
        }
        user.lastLessonDate = now;
        await user.save();
      }
    } catch (err) {
      console.log(
        "Lỗi trong lúc cộng XP, Streak và xử lý mục tiêu tuần:",
        err.message,
      );
    }

    res.status(200).json({
      message:
        "Đã cập nhật chặng học, cộng điểm và tính chuỗi ngày thành công!",
      earnedXp: xpEarned,
    });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
};

// ==========================================
// 9. NHẬN QUÀ MỤC TIÊU TUẦN
// ==========================================
exports.claimWeeklyReward = async (req, res) => {
  try {
    const userId = req.user.id || req.user.userId || req.user._id;
    const user = await User.findById(userId);

    if (!user)
      return res.status(404).json({ error: "Không tìm thấy người dùng!" });
    if (user.weeklyLessonsCompleted < 5)
      return res
        .status(400)
        .json({ error: "Bạn chưa hoàn thành đủ 5 bài học tuần này!" });
    if (user.isWeeklyRewardClaimed)
      return res
        .status(400)
        .json({ error: "Bạn đã nhận rương quà của tuần này rồi!" });

    const REWARD_XP = 200;
    user.xp = (user.xp || 0) + REWARD_XP;
    user.level = Math.floor(Math.sqrt(Math.max(0, user.xp) / 100)) + 1;
    user.isWeeklyRewardClaimed = true;

    await user.save();

    res.status(200).json({
      message: "Nhận quà thành công!",
      rewardXp: REWARD_XP,
      currentXp: user.xp,
      currentLevel: user.level,
    });
  } catch (error) {
    console.error("Lỗi khi nhận quà tuần:", error);
    res.status(500).json({ error: "Lỗi hệ thống khi nhận quà!" });
  }
};

// xAI
// Hàm xử lý request dự đoán điểm yếu
exports.predictWeakness = async (req, res) => {
  try {
    // Lấy dữ liệu học viên từ request body (do Android gửi lên)
    const studentData = {
      historyAcc: req.body.historyAcc,
      totalQuestions: req.body.totalQuestions,
      questionDifficulty: req.body.questionDifficulty,
      avgTime: req.body.avgTime,
    };

    // Gọi service để bắn HTTP POST sang Python Microservice (cổng 8000)
    const predictionResult =
      await xaiService.getWeaknessPrediction(studentData);

    // Trả kết quả SHAP & XGBoost về cho client (Android)
    res.status(200).json({
      success: true,
      message: "Dự đoán thành công",
      data: predictionResult,
    });
  } catch (error) {
    console.error("Lỗi Predictive AI:", error.message);
    res.status(500).json({
      success: false,
      message: "Hệ thống AI dự đoán đang bận, vui lòng thử lại sau.",
      error: error.message,
    });
  }
};

exports.generatePostMortem = async (req, res) => {
  try {
    // Android sẽ gửi lên một mảng chứa dữ liệu của 10 câu hỏi vừa làm
    const { quizHistory } = req.body;

    if (
      !quizHistory ||
      !Array.isArray(quizHistory) ||
      quizHistory.length === 0
    ) {
      return res
        .status(400)
        .json({ success: false, message: "Thiếu dữ liệu lịch sử làm bài." });
    }

    // 1. Gọi API Python cho toàn bộ các câu hỏi cùng lúc (tiết kiệm thời gian)
    const shapPromises = quizHistory.map((studentData) =>
      xaiService.getWeaknessPrediction(studentData),
    );
    const results = await Promise.all(shapPromises);

    // 2. Khởi tạo biến để cộng dồn SHAP values
    let totalShap = {
      historyAcc: 0,
      questionDifficulty: 0,
      avgTime: 0,
    };
    let totalAvgTimeRaw = 0; // Thời gian làm bài thực tế trung bình

    // 3. Cộng dồn dữ liệu SHAP từ phản hồi chuẩn của FastAPI (xai_explanation.detailed_shap)
    let validCount = 0;
    results.forEach((resItem, index) => {
      const detailedShap = resItem?.xai_explanation?.detailed_shap;
      if (Array.isArray(detailedShap)) {
        const shapMap = {};
        detailedShap.forEach((item) => {
          shapMap[item.feature] = Number(item.shap_score) || 0;
        });

        totalShap.historyAcc += shapMap.user_history_acc || 0;
        totalShap.questionDifficulty += shapMap.question_difficulty || 0;
        totalShap.avgTime += (shapMap.user_avg_elapsed_time || 0) + (shapMap.elapsed_time || 0);
        validCount++;
      }

      totalAvgTimeRaw += Number(quizHistory[index]?.avgTime || quizHistory[index]?.elapsedTime || 0);
    });

    if (validCount === 0) {
      return res.status(503).json({
        success: false,
        message: "Dịch vụ Predictive AI không phản hồi hoặc không trả về dữ liệu SHAP hợp lệ.",
      });
    }

    const rawMeanTime = totalAvgTimeRaw / quizHistory.length;
    // Chuẩn hóa về giây nếu đầu vào là mili-giây (> 1000)
    const meanRealTime = rawMeanTime > 1000 ? rawMeanTime / 1000 : rawMeanTime;

    // 4. "Dịch" SHAP Value thành Bệnh án
    let strengths = [];
    let weaknesses = [];

    // Phân tích Điểm mạnh (SHAP Dương)
    if (totalShap.historyAcc > 0.5) {
      strengths.push({
        title: "Phong độ ổn định",
        desc: "Nền tảng kiến thức cũ của bạn đang là bệ phóng rất tốt cho bài test này.",
      });
    }
    if (totalShap.questionDifficulty > 0.5) {
      strengths.push({
        title: "Thợ săn câu khó",
        desc: "Bạn xử lý cực kỳ bản lĩnh những câu hỏi có độ khó cao.",
      });
    }
    if (strengths.length === 0)
      strengths.push({
        title: "Đang làm nóng",
        desc: "Bạn đang ở mức an toàn, hãy bứt phá hơn ở lần sau!",
      });

    // Phân tích Điểm yếu (SHAP Âm)
    if (totalShap.avgTime < -0.3) {
      if (meanRealTime < 10) {
        // Ví dụ: dưới 10 giây/câu là vội
        weaknesses.push({
          title: "Hội chứng vội vàng",
          desc: "Bạn mất khá nhiều điểm oan vì chốt đáp án quá nhanh. Hãy nán lại thêm vài giây!",
        });
      } else {
        weaknesses.push({
          title: "Thiếu dứt khoát",
          desc: "Phân tích cho thấy bạn thường sai khi ngâm một câu quá lâu. Lần sau hãy tin vào trực giác đầu tiên!",
        });
      }
    }

    if (totalShap.historyAcc < -0.5) {
      weaknesses.push({
        title: "Hổng kiến thức nền",
        desc: "XAI nhận thấy bạn đang chật vật vì quên kiến thức cũ. Hãy quay lại ôn tập chặng trước.",
      });
    }
    if (weaknesses.length === 0)
      weaknesses.push({
        title: "Không có điểm mù",
        desc: "Phong độ hoàn hảo, không phát hiện lỗi sai hệ thống nào!",
      });

    // 5. Trả kết quả về cho Android
    res.status(200).json({
      success: true,
      data: {
        strengths: strengths,
        weaknesses: weaknesses,
        raw_shap_totals: totalShap, // Trả về số liệu gốc nếu Android cần vẽ biểu đồ
      },
    });
  } catch (error) {
    console.error("Lỗi Post-Mortem AI:", error.message);
    res.status(500).json({ success: false, message: "Lỗi phân tích bệnh án." });
  }
};