// Chạy lệnh: node scripts/createAdmin.js
// Công dụng: Tạo mới hoặc nâng cấp tài khoản admin@lexivt.com lên quyền Admin trong MongoDB
require("dotenv").config();
const mongoose = require("mongoose");
const bcrypt = require("bcryptjs");
const UserModel = require("../models/User");

async function createOrPromoteAdmin() {
  try {
    await mongoose.connect(process.env.MONGODB_CONNECTIONSTRING_STRING);

    const adminEmail = process.env.ADMIN_EMAIL || "admin@lexivt.com";
    const adminPassword = process.env.ADMIN_PASSWORD || "Admin@123456";

    let admin = await UserModel.findOne({ email: adminEmail });
    if (admin) {
      admin.role = "admin";
      admin.status = "Active";
      await admin.save();
      console.log(`✅ Đã nâng cấp tài khoản ${adminEmail} lên quyền Admin!`);
    } else {
      const salt = await bcrypt.genSalt(10);
      const hashedPassword = await bcrypt.hash(adminPassword, salt);
      admin = new UserModel({
        name: "LexiVT Admin",
        email: adminEmail,
        password: hashedPassword,
        role: "admin",
        status: "Active",
        level: 1,
      });
      await admin.save();
      console.log(`✅ Đã tạo tài khoản Admin mới: ${adminEmail}`);
    }
  } catch (err) {
    console.error("❌ Lỗi tạo Admin:", err.message);
  } finally {
    await mongoose.disconnect();
  }
}

createOrPromoteAdmin();
