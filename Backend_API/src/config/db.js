const mongoose = require('mongoose');

const connectDB = async () => {
  try {
    // Gọi biến môi trường từ file .env
    const mongoURI = process.env.MONGODB_CONNECTIONSTRING_STRING;
    
    await mongoose.connect(mongoURI);
    
    console.log('🎉 Kết nối cơ sở dữ liệu MongoDB thành công!');
  } catch (error) {
    console.error('❌ Kết nối MongoDB thất bại:', error.message);
    process.exit(1);
  }
};

module.exports = connectDB;