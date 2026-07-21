const Roadmap = require('../models/Roadmap');
const UserModel = require('../models/User'); // 🚀 [BỔ SUNG]: Import Model User để cập nhật Streak & XP

// 🚀 1. Hàm lấy lộ trình của người dùng
exports.getRoadmap = async (req, res) => {
    try {
        // Lấy userId từ params hoặc từ Token nếu có
        const userId = req.params.userId || req.user?.userId;
        const roadmap = await Roadmap.findOne({ userId });
        
        if (!roadmap) {
            return res.status(404).json({ message: "Chưa tạo lộ trình học cho người dùng này!" });
        }
        res.status(200).json(roadmap);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
};

// 🚀 2. Hàm sinh lộ trình mới từ Form AI
exports.generateRoadmap = async (req, res) => {
    try {
        const { target, currentLevel, freeTime, expectation } = req.body;
        // Ưu tiên lấy userId từ token (req.user), nếu không có mới lấy từ req.body
        const userId = req.user?.userId || req.body.userId;

        // Logic tự sinh lộ trình cá nhân hóa dựa trên input của người dùng:
        const customMotivation = expectation 
            ? `🎯 Mong muốn riêng: "${expectation}". AI đã tùy biến lộ trình học tập tối ưu!`
            : `🚀 Lộ trình thiết kế riêng cho mục tiêu ${target} của bạn.`;

        const steps = [
            { id: "1", title: `Chặng 1: Nền tảng ${currentLevel}`, description: `Học từ vựng nền móng phục vụ ${target}.`, status: "CURRENT", currentProgress: 0, maxProgress: 30 },
            { id: "2", title: "Chặng 2: Giao tiếp thực hành", description: `Tập trung các tình huống thực tế (${freeTime}).`, status: "LOCKED", currentProgress: 0, maxProgress: 40 },
            { id: "3", title: "Chặng 3: Chuyên sâu kỳ vọng", description: "Bổ sung kỹ năng theo mong muốn đặc biệt của bạn.", status: "LOCKED", currentProgress: 0, maxProgress: 50 }
        ];

        // Cập nhật đè lên Roadmap cũ (hoặc tạo mới nếu chưa có)
        const updatedRoadmap = await Roadmap.findOneAndUpdate(
    { userId },
    { overallProgressPercent: 0, motivationMessage: customMotivation, steps },
    { upsert: true, returnDocument: 'after' } // <-- Đã đổi 'new: true' thành 'returnDocument: 'after''
);

        res.status(201).json(updatedRoadmap);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
};

// 🚀 3. [BỔ SUNG MỚI]: Hàm hoàn thành chặng học (Cập nhật XP, Chuỗi ngày & Mở khóa chặng kế tiếp)
exports.completeStep = async (req, res) => {
    try {
        const { stepId, score, totalQuestions } = req.body;
        const userId = req.user?.userId || req.user?.id || req.body.userId;

        // --- 1. CẬP NHẬT THÔNG TIN NGƯỜI DÙNG (USER) ---
        const user = await UserModel.findById(userId);
        if (!user) {
            return res.status(404).json({ message: "Không tìm thấy thông tin người dùng!" });
        }

        // Tính toán XP nhận được
        const earnedXp = 50 + (parseInt(score || 0) * 10);
        user.xp = (user.xp || 0) + earnedXp;

        // Tự động tính toán Level (Ví dụ: Cứ 100 XP tăng 1 Cấp)
        const currentCalculatedLevel = Math.floor(user.xp / 100);
        if (currentCalculatedLevel > (user.level || 0)) {
            user.level = currentCalculatedLevel;
        }

        // --- TÍNH TOÁN CHUỖI NGÀY HỌC (STREAK DAYS) ---
        const getVNFormatDate = (date) => {
            const vnTime = new Date(date.getTime() + (7 * 60 * 60 * 1000));
            return vnTime.toISOString().split('T')[0];
        };

        const todayVNStr = getVNFormatDate(new Date());

        // Nếu chưa từng học hoặc streakDays đang = 0 -> Set về 1
        if (!user.lastStudyDate || !user.streakDays || user.streakDays === 0) {
            user.streakDays = 1;
        } else {
            const lastStudyVNStr = getVNFormatDate(user.lastStudyDate);
            const todayDate = new Date(todayVNStr);
            const lastStudyDate = new Date(lastStudyVNStr);

            const diffTime = todayDate - lastStudyDate;
            const diffDays = Math.round(diffTime / (1000 * 60 * 60 * 24));

           if (diffDays === 1) {
            // Hôm qua học, hôm nay học tiếp -> Tăng 1 ngày
            user.streakDays += 1;
        } else if (diffDays > 1 || diffDays < 0) {
            // Bỏ học trên 1 ngày HOẶC thời gian bị lỗi lùi về quá khứ -> Reset về 1
            user.streakDays = 1;
        }
            // diffDays === 0 (Học tiếp chặng khác trong ngày) -> Giữ nguyên streakDays
        }

        user.lastStudyDate = new Date();
        await user.save();

        // --- 2. CẬP NHẬT LỘ TRÌNH HỌC (ROADMAP) ---
        const roadmap = await Roadmap.findOne({ userId });
        if (roadmap && roadmap.steps && roadmap.steps.length > 0) {
            const stepIndex = roadmap.steps.findIndex(s => s.id === String(stepId));

            if (stepIndex !== -1) {
                // Đánh dấu chặng hiện tại là COMPLETED
                roadmap.steps[stepIndex].status = "COMPLETED";
                roadmap.steps[stepIndex].currentProgress = roadmap.steps[stepIndex].maxProgress;

                // Mở khóa chặng kế tiếp (Chuyển từ LOCKED sang CURRENT)
                if (stepIndex + 1 < roadmap.steps.length) {
                    if (roadmap.steps[stepIndex + 1].status === "LOCKED") {
                        roadmap.steps[stepIndex + 1].status = "CURRENT";
                    }
                }

                // Tính lại % tiến độ tổng quan lộ trình
                const completedCount = roadmap.steps.filter(s => s.status === "COMPLETED").length;
                roadmap.overallProgressPercent = Math.round((completedCount / roadmap.steps.length) * 100);

                await roadmap.save();
            }
        }

        return res.status(200).json({
            message: "Hoàn thành chặng học thành công!",
            earnedXp: earnedXp,
            streakDays: user.streakDays,
            level: user.level,
            roadmap: roadmap
        });

    } catch (error) {
        console.error("Lỗi khi hoàn thành chặng lộ trình:", error);
        res.status(500).json({ error: error.message });
    }
};