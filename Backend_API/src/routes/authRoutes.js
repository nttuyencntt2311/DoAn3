const express = require("express");
const bcrypt = require("bcryptjs");
const jwt = require("jsonwebtoken");
const UserModel = require("../models/User");
const verifyToken = require("../middleware/authMiddleware"); // Nhúng middleware bảo mật vào đây

// === BỔ SUNG: Import hàm cập nhật Streak từ streakService ===
const { updateStreak } = require("../service/streakService");

const router = express.Router();

// =====================================
// 1. API: ĐĂNG KÝ TÀI KHOẢN
// =====================================
router.post("/register", async (req, res) => {
  try {
    const { name, email, password, gender } = req.body;
    if (!name || !email || !password) {
      return res.status(400).json({ error: "Vui lòng nhập đầy đủ thông tin!" });
    }

    const userExists = await UserModel.findOne({ email });
    if (userExists) {
      return res.status(400).json({ error: "Email này đã được sử dụng!" });
    }

    const salt = await bcrypt.genSalt(10);
    const hashedPassword = await bcrypt.hash(password, salt);

    const newUser = new UserModel({
      name,
      email,
      password: hashedPassword,
      gender: gender || "Nam",
    });
    await newUser.save();

    res.status(201).json({ message: "Đăng ký tài khoản thành công!" });
  } catch (error) {
    res.status(500).json({ error: "Lỗi máy chủ hệ thống" });
  }
});

// =====================================
// 2. API: ĐĂNG NHẬP
// =====================================
router.post("/login", async (req, res) => {
  try {
    const { email, password } = req.body;
    if (!email || !password) {
      return res
        .status(400)
        .json({ error: "Vui lòng nhập đầy đủ email và mật khẩu!" });
    }

    const user = await UserModel.findOne({ email });
    if (!user) {
      return res.status(400).json({ error: "Email không tồn tại!" });
    }

    if (user.status === "Banned") {
      return res.status(403).json({ error: "Tài khoản của bạn đã bị khóa bởi quản trị viên!" });
    }

    const isMatch = await bcrypt.compare(password, user.password);
    if (!isMatch) {
      return res.status(400).json({ error: "Mật khẩu không chính xác!" });
    }

    const userRole = user.role || "student";

    // 1. Tạo Access Token (Thời hạn 15 phút)
    const token = jwt.sign(
      { userId: user._id, role: userRole },
      process.env.JWT_SECRET || "your_jwt_secret_key",
      { expiresIn: "15m" },
    );

    // 2. Tạo Refresh Token (Thời hạn 30 ngày)
    const refreshToken = jwt.sign(
      { userId: user._id, role: userRole },
      process.env.REFRESH_SECRET || "your_refresh_secret_key",
      { expiresIn: "30d" },
    );

    // 3. Lưu Refresh Token vào Database
    user.refreshTokens.push(refreshToken);
    await user.save();

    res.status(200).json({
      message: "Đăng nhập thành công!",
      token: token,
      refreshToken: refreshToken,
      user: {
        id: user._id,
        name: user.name,
        email: user.email,
        role: userRole,
      },
    });
  } catch (error) {
    res.status(500).json({ error: "Lỗi máy chủ hệ thống" });
  }
});


// =====================================
// 2B. API: ĐĂNG NHẬP QUẢN TRỊ VIÊN (ADMIN WEB)
// =====================================
router.post("/admin-login", async (req, res) => {
  try {
    const { email, password } = req.body;
    if (!email || !password) {
      return res.status(400).json({ error: "Vui lòng nhập đầy đủ email và mật khẩu!" });
    }

    const user = await UserModel.findOne({ email });
    if (!user) {
      return res.status(401).json({ error: "Email hoặc mật khẩu không chính xác!" });
    }

    if (user.status === "Banned") {
      return res.status(403).json({ error: "Tài khoản này đã bị khóa!" });
    }

    if (user.role !== "admin") {
      return res.status(403).json({ error: "Tài khoản này không có quyền quản trị (Admin)!" });
    }

    const isMatch = await bcrypt.compare(password, user.password);
    if (!isMatch) {
      return res.status(401).json({ error: "Email hoặc mật khẩu không chính xác!" });
    }

    const token = jwt.sign(
      { userId: user._id, role: "admin" },
      process.env.JWT_SECRET || "your_jwt_secret_key",
      { expiresIn: "2h" },
    );

    const refreshToken = jwt.sign(
      { userId: user._id, role: "admin" },
      process.env.REFRESH_SECRET || "your_refresh_secret_key",
      { expiresIn: "7d" },
    );

    user.refreshTokens.push(refreshToken);
    await user.save();

    res.status(200).json({
      message: "Đăng nhập quản trị thành công!",
      token,
      refreshToken,
      user: {
        id: user._id,
        name: user.name,
        email: user.email,
        role: user.role,
      },
    });
  } catch (error) {
    console.error("Lỗi đăng nhập Admin:", error);
    res.status(500).json({ error: "Lỗi máy chủ hệ thống" });
  }
});

// =====================================
// 3. API: LẤY THÔNG TIN CÁ NHÂN
// =====================================
router.get("/profile", verifyToken, async (req, res) => {
  try {
    const user = await UserModel.findById(req.user.userId).select("-password");
    if (!user)
      return res.status(404).json({ error: "Không tìm thấy người dùng" });
    res.status(200).json({ user });
  } catch (error) {
    res.status(500).json({ error: "Lỗi hệ thống" });
  }
});

// =====================================
// 4. API: CẬP NHẬT THÔNG TIN CÁ NHÂN (PUT) - ĐÃ BẢO MẬT
// =====================================
router.put("/profile", verifyToken, async (req, res) => {
  try {
    const { name, gender, birthDate } = req.body;

    // Tìm và cập nhật dựa trên userId bóc tách từ Token
    const updatedUser = await UserModel.findByIdAndUpdate(
      req.user.userId,
      { name, gender, birthDate },
      { new: true },
    ).select("-password");

    if (!updatedUser)
      return res.status(404).json({ error: "Không tìm thấy người dùng" });

    res
      .status(200)
      .json({ message: "Cập nhật thành công!", user: updatedUser });
  } catch (error) {
    res.status(500).json({ error: "Lỗi hệ thống" });
  }
});

// =====================================
// 5. API: LẤY CHỈ SỐ GAMIFICATION (HOME/HỒ SƠ)
// =====================================
router.get("/stats", verifyToken, async (req, res) => {
  try {
    const user = await UserModel.findById(req.user.userId);
    if (!user)
      return res.status(404).json({ error: "Không tìm thấy người dùng" });

    // 🚀 TỰ ĐỘNG ĐẾM SỐ TỪ ĐÃ THUỘC (Điểm >= 80) TỪ MẢNG TIẾN ĐỘ
    let calculatedWordsLearned = user.wordsLearned || 0;
    if (user.vocabProgress && user.vocabProgress.length > 0) {
      calculatedWordsLearned = user.vocabProgress.filter(
        (p) => p.proficiency >= 80,
      ).length;
    }

    res.status(200).json({
      stats: {
        name: user.name,
        xp: user.xp || 0,
        level: Math.floor(Math.sqrt(Math.max(0, user.xp || 0) / 100)) + 1,
        streakDays: user.streakDays || 0,
        wordsLearned: calculatedWordsLearned,
        timeLearnedMinutes: (user.timeLearnedMinutes || 0) > 100000 ? 0 : Math.max(0, user.timeLearnedMinutes || 0),
        quizzesCount: user.quizzesCount || 0,
      },
    });
  } catch (error) {
    res.status(500).json({ error: "Lỗi hệ thống" });
  }
});
// =====================================
// 6. API: ĐỔI MẬT KHẨU
// =====================================
router.put("/change-password", verifyToken, async (req, res) => {
  try {
    const { currentPass, newPass } = req.body;

    // Tìm user đang đăng nhập
    const user = await UserModel.findById(req.user.userId);
    if (!user) {
      return res.status(404).json({ error: "Không tìm thấy người dùng!" });
    }

    // So sánh mật khẩu hiện tại (nhập vào) với mật khẩu đang lưu (mã hóa)
    const isMatch = await bcrypt.compare(currentPass, user.password);
    if (!isMatch) {
      return res
        .status(400)
        .json({ error: "Mật khẩu hiện tại không chính xác!" });
    }

    // Mã hóa mật khẩu mới
    const salt = await bcrypt.genSalt(10);
    const hashedPassword = await bcrypt.hash(newPass, salt);

    // Cập nhật vào Database
    user.password = hashedPassword;
    await user.save();

    res.status(200).json({ message: "Đổi mật khẩu thành công!" });
  } catch (error) {
    res.status(500).json({ error: "Lỗi hệ thống khi đổi mật khẩu" });
  }
});

// =====================================
// 7. API: LÀM MỚI TOKEN (REFRESH TOKEN)
// =====================================
router.post("/refresh-token", async (req, res) => {
  try {
    const { refreshToken } = req.body;
    if (!refreshToken) {
      return res.status(401).json({ error: "Thiếu Refresh Token!" });
    }

    const user = await UserModel.findOne({ refreshTokens: refreshToken });
    if (!user) {
      return res
        .status(403)
        .json({ error: "Refresh Token không hợp lệ hoặc đã bị đăng xuất!" });
    }

    jwt.verify(
      refreshToken,
      process.env.REFRESH_SECRET || "your_refresh_secret_key",
      (err, decoded) => {
        if (err) {
          return res
            .status(403)
            .json({
              error: "Refresh Token đã hết hạn, vui lòng đăng nhập lại!",
            });
        }

        const newAccessToken = jwt.sign(
          { userId: user._id, role: user.role || "student" },
          process.env.JWT_SECRET || "your_jwt_secret_key",
          { expiresIn: "15m" },
        );

        res.status(200).json({ token: newAccessToken });
      },
    );
  } catch (error) {
    res.status(500).json({ error: "Lỗi hệ thống khi làm mới token!" });
  }
});

// =====================================
// 8. API: ĐĂNG XUẤT (XÓA REFRESH TOKEN)
// =====================================
router.post("/logout", async (req, res) => {
  try {
    const { refreshToken } = req.body;
    if (!refreshToken) {
      return res
        .status(400)
        .json({ error: "Thiếu Refresh Token để đăng xuất!" });
    }

    const user = await UserModel.findOne({ refreshTokens: refreshToken });
    if (user) {
      user.refreshTokens = user.refreshTokens.filter(
        (token) => token !== refreshToken,
      );
      await user.save();
    }

    res.status(200).json({ message: "Đăng xuất thành công trên hệ thống!" });
  } catch (error) {
    res.status(500).json({ error: "Lỗi hệ thống khi đăng xuất!" });
  }
});

// ==========================================
// 9. API: LƯU TỪ VỰNG VÀO SỔ TAY (Từ code của bạn bè)
// ==========================================
router.post("/save-word", verifyToken, async (req, res) => {
  try {
    const userId = req.user.userId;
    const { wordId } = req.body;
    if (!wordId) {
      return res.status(400).json({ error: "Thiếu wordId cần lưu!" });
    }

    // Tìm User theo ID và nhét wordId vào mảng savedWords
    // Lệnh $addToSet giúp kiểm tra nếu từ đó lưu rồi thì không lưu trùng nữa
    // Chú ý: Đã đổi 'User' thành 'UserModel' để đồng bộ với các phần trên
    const updatedUser = await UserModel.findByIdAndUpdate(
      userId,
      { $addToSet: { savedWords: wordId } },
      { new: true },
    );

    if (!updatedUser) {
      return res.status(404).json({ error: "Không tìm thấy tài khoản User" });
    }

    // === BỔ SUNG: TỰ ĐỘNG CẬP NHẬT CHUỖI NGÀY HỌC (STREAK) ===
    // Khi lưu từ thành công, gọi hàm updateStreak để tăng số ngày học liên tiếp
    const streakUser = await updateStreak(userId);
    const currentStreakDays = streakUser ? streakUser.streakDays : (updatedUser.streakDays || 0);

    // Ẩn mật khẩu trước khi trả về client
    const safeUser = updatedUser.toObject();
    delete safeUser.password;

    res.status(200).json({
      message: "Lưu từ thành công!",
      user: safeUser,
      streakDays: currentStreakDays, // Trả về đúng số nguyên streakDays
    });
  } catch (error) {
    console.error("Lỗi hệ thống khi lưu từ:", error);
    res.status(500).json({ error: "Lỗi server Backend" });
  }
});

module.exports = router;
