const UserModel = require("../models/User");

/**
 * Middleware kiểm tra quyền Quản trị viên (Admin).
 * Luôn sử dụng SAU middleware verifyToken.
 */
const requireAdmin = async (req, res, next) => {
  try {
    const userId = req.user?.userId || req.user?.id;
    if (!userId) {
      return res.status(401).json({ error: "Chưa xác thực người dùng!" });
    }

    const user = await UserModel.findById(userId).select("role status");
    if (!user) {
      return res.status(404).json({ error: "Không tìm thấy tài khoản!" });
    }

    if (user.status === "Banned") {
      return res.status(403).json({ error: "Tài khoản của bạn đã bị khóa!" });
    }

    if (user.role !== "admin") {
      return res.status(403).json({ error: "Truy cập bị từ chối! Yêu cầu quyền Quản trị viên (Admin)." });
    }

    next();
  } catch (error) {
    console.error("Lỗi kiểm tra quyền Admin:", error.message);
    res.status(500).json({ error: "Lỗi máy chủ khi xác thực phân quyền!" });
  }
};

module.exports = requireAdmin;
