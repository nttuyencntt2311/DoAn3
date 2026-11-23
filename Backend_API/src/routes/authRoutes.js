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

    // 1. Tạo Access Token (Mặc định 7 ngày, có thể cấu hình qua JWT_EXPIRES_IN)
    const token = jwt.sign(
      { userId: user._id, role: userRole },
      process.env.JWT_SECRET || "your_jwt_secret_key",
      { expiresIn: process.env.JWT_EXPIRES_IN || "7d" },
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
          { expiresIn: process.env.JWT_EXPIRES_IN || "7d" },
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


// =====================================
// 8. QUÊN MẬT KHẨU (GỬI MÃ OTP 6 SỐ) & ĐẶT LẠI MẬT KHẨU
// =====================================
const otpStore = new Map(); // email -> { otp, expiresAt }

router.post("/forgot-password", async (req, res) => {
  try {
    const email = (req.body.email || "").trim().toLowerCase();
    if (!email) {
      return res.status(400).json({ error: "Vui lòng nhập địa chỉ email!" });
    }

    const user = await UserModel.findOne({ email: new RegExp(`^${email.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")}$`, "i") });
    if (!user) {
      return res.status(404).json({ error: "Email này chưa được đăng ký trong hệ thống!" });
    }

    const otp = Math.floor(100000 + Math.random() * 900000).toString();
    otpStore.set(user.email.toLowerCase(), {
      otp,
      expiresAt: Date.now() + 10 * 60 * 1000, // Hết hạn sau 10 phút
    });

    let emailSent = false;
    if (process.env.EMAIL_USER && process.env.EMAIL_PASS) {
      try {
        const nodemailer = require("nodemailer");
        const transporter = nodemailer.createTransport({
          service: "gmail",
          auth: {
            user: process.env.EMAIL_USER,
            pass: process.env.EMAIL_PASS,
          },
        });
        await transporter.sendMail({
          from: `"LexiVT Support" <${process.env.EMAIL_USER}>`,
          to: user.email,
          subject: "Mã OTP khôi phục mật khẩu LexiVT",
          text: `Mã OTP khôi phục mật khẩu của bạn là: ${otp}. Mã có hiệu lực trong 10 phút.`,
        });
        emailSent = true;
      } catch (mailErr) {
        console.warn("Không gửi được email thật (dùng chế độ OTP console):", mailErr.message);
      }
    }

    console.log(`🔑 [LexiVT OTP] Email: ${user.email} | Mã OTP: ${otp} (hiệu lực 10 phút)`);

    res.status(200).json({
      message: emailSent
        ? "Đã gửi mã OTP 6 số tới email của bạn!"
        : "Đã tạo mã OTP 6 số (chế độ dev/demo)!",
      demoOtp: emailSent ? undefined : otp,
    });
  } catch (error) {
    console.error("Lỗi forgot-password:", error);
    res.status(500).json({ error: "Lỗi máy chủ khi tạo mã OTP" });
  }
});

router.post("/reset-password", async (req, res) => {
  try {
    const email = (req.body.email || "").trim().toLowerCase();
    const otp = (req.body.otp || "").trim();
    const newPassword = (req.body.newPassword || "").trim();

    if (!email || !otp || !newPassword) {
      return res.status(400).json({ error: "Vui lòng nhập đầy đủ email, mã OTP và mật khẩu mới!" });
    }
    if (newPassword.length < 6) {
      return res.status(400).json({ error: "Mật khẩu mới phải từ 6 ký tự trở lên!" });
    }

    const record = otpStore.get(email);
    if (!record || record.otp !== otp) {
      return res.status(400).json({ error: "Mã OTP không chính xác!" });
    }
    if (Date.now() > record.expiresAt) {
      otpStore.delete(email);
      return res.status(400).json({ error: "Mã OTP đã hết hạn. Vui lòng gửi lại mã mới!" });
    }

    const user = await UserModel.findOne({ email: new RegExp(`^${email.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")}$`, "i") });
    if (!user) {
      return res.status(404).json({ error: "Không tìm thấy tài khoản!" });
    }

    const salt = await bcrypt.genSalt(10);
    user.password = await bcrypt.hash(newPassword, salt);
    await user.save();
    otpStore.delete(email);

    res.status(200).json({ message: "Đặt lại mật khẩu thành công! Bạn có thể đăng nhập ngay." });
  } catch (error) {
    console.error("Lỗi reset-password:", error);
    res.status(500).json({ error: "Lỗi máy chủ khi đặt lại mật khẩu" });
  }
});


// =====================================
// 9. ĐĂNG NHẬP / ĐĂNG KÝ NHANH BẰNG GOOGLE
// =====================================
router.post("/google-login", async (req, res) => {
  try {
    const email = (req.body.email || "").trim().toLowerCase();
    const displayName = (req.body.name || "").trim();
    if (!email || !email.includes("@")) {
      return res.status(400).json({ error: "Email tài khoản Google không hợp lệ!" });
    }

    let user = await UserModel.findOne({ email: new RegExp(`^${email.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")}$`, "i") });
    if (!user) {
      const salt = await bcrypt.genSalt(10);
      const randomPass = await bcrypt.hash("google_oauth_" + Date.now() + "_" + Math.random(), salt);
      user = new UserModel({
        name: displayName || email.split("@")[0],
        email,
        password: randomPass,
        gender: "Nam",
      });
      await user.save();
    }

    if (user.status === "Banned") {
      return res.status(403).json({ error: "Tài khoản của bạn đã bị khóa bởi quản trị viên!" });
    }

    const userRole = user.role || "student";
    const token = jwt.sign(
      { userId: user._id, role: userRole },
      process.env.JWT_SECRET || "your_jwt_secret_key",
      { expiresIn: process.env.JWT_EXPIRES_IN || "7d" }
    );
    const refreshToken = jwt.sign(
      { userId: user._id, role: userRole },
      process.env.REFRESH_SECRET || "your_refresh_secret_key",
      { expiresIn: "30d" }
    );

    user.refreshTokens.push(refreshToken);
    await user.save();

    res.status(200).json({
      message: "Đăng nhập bằng Google thành công!",
      token,
      refreshToken,
      user: {
        id: user._id,
        name: user.name,
        email: user.email,
        role: userRole,
      },
    });
  } catch (error) {
    console.error("Lỗi google-login:", error);
    res.status(500).json({ error: "Lỗi máy chủ khi đăng nhập Google" });
  }
});

module.exports = router;


