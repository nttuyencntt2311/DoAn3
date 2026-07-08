const jwt = require('jsonwebtoken');

const verifyToken = (req, res, next) => {
  // Lấy token từ header 'Authorization'
  const authHeader = req.header('Authorization');
  if (!authHeader) return res.status(401).json({ error: "Từ chối truy cập! Không có token." });

  // Token thường có dạng: "Bearer <chuỗi_token>", chúng ta cần cắt chuỗi lấy phần token
  const token = authHeader.split(" ")[1];
  if (!token) return res.status(401).json({ error: "Định dạng token không hợp lệ!" });

  try {
    // Tiến hành giải mã token bằng Chuỗi Bí Mật
    const verified = jwt.verify(token, process.env.JWT_SECRET || "ChuoiBiMatTamThoi");
    req.user = verified; // Lưu thông tin đã giải mã (userId, email) vào biến req để tầng sau dùng
    next(); // Cho phép chạy tiếp vào API chính
  } catch (err) {
    res.status(400).json({ error: "Token không hợp lệ hoặc đã hết hạn!" });
  }
};

module.exports = verifyToken;