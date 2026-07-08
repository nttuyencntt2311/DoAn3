const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const UserModel = require('../models/User'); 
const verifyToken = require('../middleware/authMiddleware'); // Nhúng middleware bảo mật vào đây

const router = express.Router();

// =====================================
// 1. API: ĐĂNG KÝ TÀI KHOẢN
// =====================================
router.post('/register', async (req, res) => {
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

    const newUser = new UserModel({ name, email, password: hashedPassword, gender: gender || "Nam" });
    await newUser.save();

    res.status(201).json({ message: "Đăng ký tài khoản thành công!" });
  } catch (error) {
    res.status(500).json({ error: "Lỗi máy chủ hệ thống" });
  }
});

// =====================================
// 2. API: ĐĂNG NHẬP
// =====================================
router.post('/login', async (req, res) => {
  try {
    const { email, password } = req.body;
    const user = await UserModel.findOne({ email });
    if (!user) return res.status(400).json({ error: "Sai email hoặc mật khẩu!" });

    const isMatch = await bcrypt.compare(password, user.password);
    if (!isMatch) return res.status(400).json({ error: "Sai email hoặc mật khẩu!" });

    const token = jwt.sign(
      { userId: user._id, email: user.email }, 
      process.env.JWT_SECRET || "ChuoiBiMatTamThoi", 
      { expiresIn: '30d' }
    );

    res.status(200).json({
      message: "Đăng nhập thành công!",
      token: token,
      user: { id: user._id, name: user.name, email: user.email }
    });
  } catch (error) {
    res.status(500).json({ error: "Lỗi hệ thống" });
  }
});

// =====================================
// 3. API: LẤY THÔNG TIN CÁ NHÂN (GET) - ĐÃ BẢO MẬT
// =====================================
router.get('/profile', verifyToken, async (req, res) => {
  try {
    // Lấy ID trực tiếp từ Token đã được giải mã ở middleware, tuyệt đối bảo mật
    const user = await UserModel.findById(req.user.userId).select('-password');
    if (!user) return res.status(404).json({ error: "Không tìm thấy người dùng" });
    
    res.status(200).json({ user });
  } catch (error) {
    res.status(500).json({ error: "Lỗi hệ thống" });
  }
});

// =====================================
// 4. API: CẬP NHẬT THÔNG TIN CÁ NHÂN (PUT) - ĐÃ BẢO MẬT
// =====================================
router.put('/profile', verifyToken, async (req, res) => {
  try {
    const { name, gender, birthDate } = req.body;
    
    // Tìm và cập nhật dựa trên userId bóc tách từ Token
    const updatedUser = await UserModel.findByIdAndUpdate(
      req.user.userId,
      { name, gender, birthDate },
      { new: true }
    ).select('-password');
    
    if (!updatedUser) return res.status(404).json({ error: "Không tìm thấy người dùng" });
    
    res.status(200).json({ message: "Cập nhật thành công!", user: updatedUser });
  } catch (error) {
    res.status(500).json({ error: "Lỗi hệ thống" });
  }

  
});

// =====================================
// 5. API: LẤY CHỈ SỐ GAMIFICATION (HOME/HỒ SƠ)
// =====================================
router.get('/stats', verifyToken, async (req, res) => {
  try {
    // Tìm user và chỉ lấy các trường cần thiết cho Trang chủ & Hồ sơ
    const user = await UserModel.findById(req.user.userId)
      .select('name xp level streakDays wordsLearned timeLearnedMinutes');
      
    if (!user) return res.status(404).json({ error: "Không tìm thấy người dùng" });
    
    res.status(200).json({ stats: user });
  } catch (error) {
    res.status(500).json({ error: "Lỗi hệ thống" });
  }
});
module.exports = router;