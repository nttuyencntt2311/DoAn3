// Chạy 1 lần duy nhất để sửa dữ liệu bị lệch trong MongoDB:
//   node scripts/fixCorruptedStats.js
require("dotenv").config();
const mongoose = require("mongoose");
const connectDB = require("../config/db");
const User = require("../models/User");

const calculateLevel = (xp) => Math.floor(Math.sqrt(Math.max(0, Number(xp) || 0) / 100)) + 1;

(async () => {
  try {
    await connectDB();
    const users = await User.find();
    let updatedCount = 0;

    for (const user of users) {
      let changed = false;

      // 1. Sửa timeLearnedMinutes bị hỏng do lỗi timestamp (> 100,000 phút)
      if ((user.timeLearnedMinutes || 0) > 100000 || (user.timeLearnedMinutes || 0) < 0) {
        const estimatedMinutes = Math.max(0, (user.quizzesCount || 0) * 5);
        console.log(
          `[FIX TIME] ${user.name} (${user.email}): ${user.timeLearnedMinutes} -> ${estimatedMinutes} phút`
        );
        user.timeLearnedMinutes = estimatedMinutes;
        changed = true;
      }

      // 2. Tính lại level theo công thức lũy tiến mới (bắt đầu từ Cấp 1)
      const newLevel = calculateLevel(user.xp || 0);
      if (user.level !== newLevel) {
        console.log(`[FIX LEVEL] ${user.name}: Cấp ${user.level} -> Cấp ${newLevel} (${user.xp || 0} XP)`);
        user.level = newLevel;
        changed = true;
      }

      // 3. Đồng bộ wordsLearned theo số từ thực tế >= 80 trong vocabProgress
      if (Array.isArray(user.vocabProgress)) {
        const masteredCount = user.vocabProgress.filter((v) => (v.proficiency || 0) >= 80).length;
        if (user.wordsLearned !== masteredCount) {
          user.wordsLearned = masteredCount;
          changed = true;
        }
      }

      if (changed) {
        await user.save();
        updatedCount++;
      }
    }

    console.log(`Hoàn tất! Đã cập nhật ${updatedCount}/${users.length} tài khoản.`);
    await mongoose.connection.close();
    process.exit(0);
  } catch (err) {
    console.error("Lỗi khi sửa dữ liệu:", err);
    process.exit(1);
  }
})();
