const jwt = require('jsonwebtoken');

const verifyToken = (req, res, next) => {
  // Lấy token từ header 'Authorization'
  const authHeader = req.header('Authorization');
  if (!authHeader) return res.status(401).json({ error: "Từ chối truy cập! Không có token." });

  // Token thường có dạng: "Bearer <chuỗi_token>", chúng ta cần cắt chuỗi lấy phần token
  const token = authHeader.split(" ")[1];
  if (!token) return res.status(401).json({ error: "Định dạng token không hợp lệ!" });

// authMiddleware.js - SỬA LẠI ĐOẠN CATCH
try {
  const verified = jwt.verify(token, process.env.JWT_SECRET || "ChuoiBiMatTamThoi");
  req.user = verified; 
  next();
} catch (err) {
  // BẮT BUỘC phải trả về 401 để kích hoạt TokenAuthenticator phía Android
  res.status(401).json({ error: "Token không hợp lệ hoặc đã hết hạn!" }); 
}

};

module.exports = verifyToken;