const Roadmap = require('../models/Roadmap');

// 🚀 1. Hàm lấy lộ trình của người dùng
exports.getRoadmap = async (req, res) => {
    try {
        const { userId } = req.params;
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
        const { userId, target, currentLevel, freeTime, expectation } = req.body;

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
            { upsert: true, new: true }
        );

        res.status(201).json(updatedRoadmap);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
};