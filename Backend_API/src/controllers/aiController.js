const aiService = require('../service/aiService'); // Giữ nguyên đường dẫn cũ của bạn
const ChatSession = require('../models/ChatSession'); 
const ChatMessage = require('../models/ChatMessage'); 
const Roadmap = require('../models/Roadmap');
const User = require('../models/User'); // 🚀 Đưa lên đầu file cho chuẩn cấu trúc

// ==========================================
// 1. TẠO PHIÊN CHAT MỚI (Mở phòng chat)
// ==========================================
exports.createChatSession = async (req, res) => {
    try {
        const { userId, chatType, firstMessage } = req.body;
        
        // Cắt 20 ký tự đầu của câu hỏi đầu tiên để làm Tiêu đề tự động
        const title = firstMessage ? firstMessage.substring(0, 20) + '...' : 'Cuộc trò chuyện mới';

        const newSession = new ChatSession({ userId, chatType, title });
        await newSession.save();

        res.status(201).json({ sessionId: newSession._id, title: newSession.title });
    } catch (error) {
        console.error("Lỗi tạo phiên chat:", error);
        res.status(500).json({ error: 'Lỗi khi tạo phiên chat trên hệ thống!' });
    }
};

// ==========================================
// 2. GỬI TIN NHẮN & LƯU DB (Đã sửa lỗi Mất Trí Nhớ)
// ==========================================
exports.chatWithAI = async (req, res) => {
  try {
    const { sessionId, message } = req.body;
    
    if (!message) {
      return res.status(400).json({ error: "Tin nhắn không được để trống!" });
    }

    // Mảng chứa ngữ cảnh cuộc trò chuyện để nạp cho AI
    let chatHistory = [];

    // GIAI ĐOẠN 1: Nếu có phòng chat -> Lấy lịch sử cũ TRƯỚC khi lưu tin nhắn mới
    if (sessionId) {
        // Lấy 10 tin nhắn gần nhất để làm context (Tránh lấy quá nhiều gây tràn Token của AI)
        // Sắp xếp theo createdAt: -1 để lấy các tin nhắn mới nhất trước
        const rawHistory = await ChatMessage.find({ sessionId })
                                            .sort({ createdAt: -1 })
                                            .limit(10);
        
        // Đảo ngược mảng lại để lịch sử có thứ tự thời gian chuẩn: Cũ -> Mới
        chatHistory = rawHistory.reverse();

        // Sau khi đã lấy lịch sử xong, tiến hành lưu tin nhắn mới của User vào DB
        const userMsg = new ChatMessage({ sessionId, role: 'user', text: message });
        await userMsg.save();

        // Đẩy phòng chat này lên đầu danh sách
        await ChatSession.findByIdAndUpdate(sessionId, { updatedAt: new Date() });
    }

    // GIAI ĐOẠN 2: Gọi AI từ service (Truyền thêm chatHistory để AI có trí nhớ)
    const aiReply = await aiService.generateChatReply(message, chatHistory);

    // GIAI ĐOẠN 3: Nhận kết quả từ AI -> Lưu vào DB
    if (sessionId) {
        const aiMsg = new ChatMessage({ sessionId, role: 'ai', text: aiReply });
        await aiMsg.save();
    }

    // Trả kết quả về cho điện thoại Android hiển thị
    res.status(200).json({ reply: aiReply });
    
  } catch (error) {
    console.error("Lỗi chi tiết từ Google Gemini/Groq:", error);
    res.status(500).json({ error: `Lỗi từ AI: ${error.message}` });
  }
};

// ==========================================
// 3. LẤY LẠI LỊCH SỬ TIN NHẮN CŨ
// ==========================================
exports.getChatHistory = async (req, res) => {
    try {
        const { sessionId } = req.params;
        
        // Lấy tất cả tin nhắn thuộc về phòng chat này, sắp xếp cũ nhất -> mới nhất (1)
        const messages = await ChatMessage.find({ sessionId }).sort({ createdAt: 1 });
        
        res.status(200).json({ messages });
    } catch (error) {
        console.error("Lỗi lấy lịch sử:", error);
        res.status(500).json({ error: 'Lỗi khi lấy lịch sử chat' });
    }
};

// ==========================================
// 4. LẤY DANH SÁCH CÁC PHÒNG CHAT CỦA USER
// ==========================================
exports.getUserChatSessions = async (req, res) => {
    try {
        const { userId } = req.params;
        // Lấy tất cả phòng chat của user này, cái nào mới nhắn (updatedAt) thì xếp lên đầu
        const sessions = await ChatSession.find({ userId }).sort({ updatedAt: -1 });
        res.status(200).json({ sessions });
    } catch (error) {
        console.error("Lỗi lấy danh sách phòng chat:", error);
        res.status(500).json({ error: 'Lỗi máy chủ khi lấy danh sách phòng chat!' });
    }
};

// ==========================================
// 5. TẠO LỘ TRÌNH MỚI BẰNG AI
// ==========================================
exports.generateRoadmap = async (req, res) => {
    try {
        // 1. Lấy ID chuẩn xác từ Token đăng nhập
        const userId = req.user.id || req.user.userId || req.user._id;

        const { target, currentLevel, freeTime, expectation } = req.body;

        const aiRoadmap = await aiService.generateRoadmapJSON(target, currentLevel, freeTime);

        // 3. Chuẩn bị câu động viên
        const customMotivation = expectation 
            ? `🎯 Mong muốn: "${expectation}". ${aiRoadmap.motivationMessage}` 
            : aiRoadmap.motivationMessage;

        // 4. Map lại danh sách chặng học cho chuẩn Schema
        const steps = aiRoadmap.steps.map((s, index) => ({
            id: (index + 1).toString(), // Gắn ID 1, 2, 3, 4 cho từng chặng
            title: s.title,
            description: s.description,
            status: index === 0 ? "CURRENT" : "LOCKED", // Chặng đầu tiên mặc định đang học
            currentProgress: 0,
            maxProgress: s.maxProgress || 30
        }));

        // 5. Lưu vào MongoDB (Tự động cập nhật lộ trình cũ nếu có)
        const updatedRoadmap = await Roadmap.findOneAndUpdate(
            { userId: userId },
            { 
                overallProgressPercent: 0, 
                motivationMessage: customMotivation, 
                steps: steps 
            },
            { upsert: true, returnDocument: 'after' } 
        );

        // 6. Trả kết quả về cho Android
        res.status(200).json({
            userId: updatedRoadmap.userId,
            overallProgressPercent: updatedRoadmap.overallProgressPercent,
            motivationMessage: updatedRoadmap.motivationMessage,
            steps: updatedRoadmap.steps
        });

    } catch (error) {
        console.error("Lỗi khi gen Roadmap:", error);
        res.status(500).json({ error: "Lỗi hệ thống khi khởi tạo lộ trình!" });
    }
};

// ==========================================
// 6. LẤY LỘ TRÌNH HIỆN TẠI CỦA USER
// ==========================================
exports.getMyRoadmap = async (req, res) => {
    try {
        // Lấy ID chuẩn xác từ Token
        const userId = req.user.id || req.user.userId || req.user._id;
        
        const roadmap = await Roadmap.findOne({ userId: userId });
        
        // Nếu user mới tinh chưa có lộ trình, trả về dữ liệu mặc định tránh crash App
        if (!roadmap) {
            return res.status(200).json({
                userId: userId,
                overallProgressPercent: 0,
                motivationMessage: "Bạn chưa có lộ trình. Hãy để AI thiết kế cho bạn nhé!",
                steps: []
            });
        }

        // TỐI ƯU HÓA: Map lại y hệt cấu trúc Kotlin cần (lọc bỏ _id rác của MongoDB để không văng app)
        res.status(200).json({
            userId: roadmap.userId,
            overallProgressPercent: roadmap.overallProgressPercent,
            motivationMessage: roadmap.motivationMessage,
            steps: roadmap.steps.map(s => ({
                id: s.id, 
                title: s.title,
                description: s.description,
                status: s.status,
                currentProgress: s.currentProgress,
                maxProgress: s.maxProgress
            }))
        });
    } catch (error) {
        console.error("Lỗi khi lấy Roadmap:", error);
        res.status(500).json({ error: error.message });
    }
};

// ==========================================
// 7. AI SINH BÀI TẬP TRẮC NGHIỆM THEO CHẶNG HỌC
// ==========================================
exports.generateStepQuiz = async (req, res) => {
    try {
        // 🚀 BỔ SUNG: Nhận thêm questionCount từ Android (Mặc định là 5 nếu không truyền)
        const { stepTitle, stepDesc, questionCount } = req.query;
        const numOfQuestions = questionCount ? parseInt(questionCount) : 5;

        if (!stepTitle) {
            return res.status(400).json({ error: "Thiếu chủ đề chặng học (stepTitle)!" });
        }
        
        // 🚀 BỔ SUNG: Sửa lại Prompt để thay số lượng câu hỏi linh hoạt
        const prompt = `
Bạn là một chuyên gia khảo thí ngôn ngữ và Giám khảo IELTS/Tiếng Anh giao tiếp cấp độ B1 đến C1.
Nhiệm vụ của bạn là tạo ra ${numOfQuestions} câu hỏi trắc nghiệm (Multiple Choice) CHẤT LƯỢNG CAO, THỬ THÁCH VÀ MANG TÍNH ĐÁNH GIÁ, dựa trên chủ đề: "${stepTitle}" (Mô tả: ${stepDesc}).

⚠️ YÊU CẦU CHUYÊN MÔN KHẮT KHE (TUYỆT ĐỐI TUÂN THỦ):
1. KHÔNG HỎI KIẾN THỨC HIỂN NHIÊN, TRẺ CON (Ví dụ: tuyệt đối không hỏi bảng chữ cái, đếm số, màu sắc cơ bản).
2. TRỌNG TÂM: Kiểm tra "Từ vựng trong ngữ cảnh" (Vocabulary in context), "Cụm từ cố định" (Collocations), "Thành ngữ" (Idioms), hoặc Phrasal verbs.
3. ĐÁP ÁN NHIỄU (Distractors): 3 đáp án sai phải cực kỳ thông minh, có vẻ hợp lý, có thể đúng về ngữ pháp nhưng sai về ngữ nghĩa, hoặc ngược lại. Tránh các đáp án sai quá lộ liễu.
4. CÂU HỎI: Phải là một câu văn tiếng Anh hoàn chỉnh (dài từ 10-20 từ), có chứa một khoảng trống "___" để điền từ. Không dùng câu hỏi dạng định nghĩa.

BẮT BUỘC trả về định dạng JSON thuần, cấu trúc là một mảng [] chứa các đối tượng có thuộc tính sau:
[
  {
    "question": "Câu văn tiếng Anh chất lượng cao, có khoảng trống ___",
    "options": ["Đáp án nhiễu 1", "Đáp án đúng", "Đáp án nhiễu 2", "Đáp án nhiễu 3"],
    "correctAnswer": "Viết chính xác nội dung của đáp án đúng",
    "pronounceText": "Từ vựng chính yếu xuất hiện trong đáp án đúng để luyện phát âm",
    "hint": "Dịch nghĩa tiếng Việt của cả câu hỏi để giúp học viên hiểu ngữ cảnh"
  }
]
`;

        const model = aiService.genAI?.getGenerativeModel 
            ? aiService.genAI.getGenerativeModel({ 
                model: "gemini-2.5-flash",
                generationConfig: { responseMimeType: "application/json" }
              })
            : null;

        if (!model) {
            return res.status(500).json({ error: "Không tìm thấy cấu hình Google AI!" });
        }

        const result = await model.generateContent(prompt);
        let responseText = result.response.text();

        // Dùng Regex tìm mảng JSON chuẩn xác bất chấp AI sinh ra chữ rác gì
        const jsonMatch = responseText.match(/\[[\s\S]*\]/);
        if (!jsonMatch) {
            throw new Error("AI không trả về định dạng mảng JSON hợp lệ!");
        }

        const quizData = JSON.parse(jsonMatch[0]);
        res.status(200).json(quizData);

    } catch (error) {
        console.error("Lỗi khi sinh câu hỏi Quiz:", error);
        res.status(500).json({ error: "Lỗi hệ thống khi tải câu hỏi trắc nghiệm!" });
    }
};

// ==========================================
// 8. CẬP NHẬT TRẠNG THÁI HOÀN THÀNH CHẶNG HỌC & CỘNG ĐIỂM
// ==========================================
exports.completeStep = async (req, res) => {
    try {
        const userId = req.user.id || req.user.userId || req.user._id;
        const { stepId, score, totalQuestions } = req.body; 

        const roadmap = await Roadmap.findOne({ userId });
        if (!roadmap) return res.status(404).json({ error: "Không tìm thấy lộ trình!" });

        // 1. Tìm vị trí của chặng người dùng học xong
        const stepIndex = roadmap.steps.findIndex(s => s.id === stepId);
        if (stepIndex !== -1) {
            // Đổi chặng hiện tại thành ĐH (COMPLETED)
            roadmap.steps[stepIndex].status = 'COMPLETED';
            roadmap.steps[stepIndex].currentProgress = roadmap.steps[stepIndex].maxProgress;

            // Mở khóa chặng tiếp theo (CURRENT)
            if (stepIndex + 1 < roadmap.steps.length) {
                roadmap.steps[stepIndex + 1].status = 'CURRENT';
            }

            // Tính lại % tiến độ của cả lộ trình
            const completedCount = roadmap.steps.filter(s => s.status === 'COMPLETED').length;
            roadmap.overallProgressPercent = Math.round((completedCount / roadmap.steps.length) * 100);

            await roadmap.save();
        }

       // CỘNG ĐIỂM (XP), TÍNH CHUỖI NGÀY VÀ XỬ LÝ MỤC TIÊU TUẦN CHO USER
        const scoreEarned = parseInt(score) || 0;
        const xpEarned = 50 + (scoreEarned * 10);

        try {
            const user = await User.findById(userId);
            if (user) {
                // 1. CỘNG XP VÀ TÍNH LEVEL
                user.xp = (user.xp || 0) + xpEarned;
                user.level = Math.floor(user.xp / 100) + 1;

                // 2. TÍNH TOÁN CHUỖI NGÀY HỌC (STREAK DAYS) - VỪA BỔ SUNG
                const getVNFormatDate = (date) => {
                    const vnTime = new Date(date.getTime() + (7 * 60 * 60 * 1000));
                    return vnTime.toISOString().split('T')[0];
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
                user.lastStudyDate = new Date(); // Cập nhật ngày học chuỗi

                // 3. LOGIC KIỂM TRA MỤC TIÊU TUẦN
                const now = new Date();
                const lastDate = user.lastLessonDate || new Date(0);
                
                const isSameWeek = (now - lastDate) / (1000 * 60 * 60 * 24) < 7 && now.getDay() >= lastDate.getDay();

                if (!isSameWeek) {
                    user.weeklyLessonsCompleted = 1;
                    user.isWeeklyRewardClaimed = false;
                } else {
                    if (user.weeklyLessonsCompleted < 5) {
                        user.weeklyLessonsCompleted += 1;
                    }
                }
                user.lastLessonDate = now; // Cập nhật ngày làm bài tuần
                
                await user.save();
            }
        } catch (err) {
            console.log("Lỗi trong lúc cộng XP, Streak và xử lý mục tiêu tuần:", err.message);
        }

        res.status(200).json({ 
            message: "Đã cập nhật chặng học, cộng điểm và tính chuỗi ngày thành công!", 
            earnedXp: xpEarned 
        });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
};

// ==========================================
// 9. NHẬN QUÀ MỤC TIÊU TUẦN (NEW)
// ==========================================
exports.claimWeeklyReward = async (req, res) => {
    try {
        const userId = req.user.id || req.user.userId || req.user._id;
        const user = await User.findById(userId);

        if (!user) {
            return res.status(404).json({ error: "Không tìm thấy người dùng!" });
        }

        if (user.weeklyLessonsCompleted < 5) {
            return res.status(400).json({ error: "Bạn chưa hoàn thành đủ 5 bài học tuần này!" });
        }

        if (user.isWeeklyRewardClaimed) {
            return res.status(400).json({ error: "Bạn đã nhận rương quà của tuần này rồi!" });
        }

        // Đủ điều kiện -> Nhận 500 XP siêu to khổng lồ
        const REWARD_XP = 500;
        user.xp += REWARD_XP;
        user.level = Math.floor(user.xp / 100) + 1;
        user.isWeeklyRewardClaimed = true; // Khóa lại không cho nhận nữa

        await user.save();

        res.status(200).json({ 
            message: "Nhận quà thành công!", 
            rewardXp: REWARD_XP,
            currentXp: user.xp,
            currentLevel: user.level
        });

    } catch (error) {
        console.error("Lỗi khi nhận quà tuần:", error);
        res.status(500).json({ error: "Lỗi hệ thống khi nhận quà!" });
    }
};


