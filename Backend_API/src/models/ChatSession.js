const mongoose = require('mongoose');

const chatSessionSchema = new mongoose.Schema({
    userId: { 
        type: mongoose.Schema.Types.ObjectId, 
        ref: 'User', // Trỏ tới bảng User của bạn
        required: true 
    },
    title: { 
        type: String, 
        default: 'Cuộc trò chuyện mới' 
    },
    chatType: { 
        type: String, 
        enum: ['GIAI_THICH', 'TAO_QUIZ', 'LO_TRINH', 'TRO_CHUYEN', 'AI_ASSISTANT'],
        default: 'TRO_CHUYEN'
    }
}, { 
    timestamps: true // Tự động tạo createdAt và updatedAt
});

module.exports = mongoose.model('ChatSession', chatSessionSchema);