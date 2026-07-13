const mongoose = require('mongoose');

const chatMessageSchema = new mongoose.Schema({
    sessionId: { 
        type: mongoose.Schema.Types.ObjectId, 
        ref: 'ChatSession',
        required: true 
    },
    role: { 
        type: String, 
        enum: ['user', 'ai'], // Phân biệt ai là người gửi
        required: true 
    },
    text: { 
        type: String, 
        required: true 
    }
}, { 
    timestamps: true 
});

// Đánh Index để tối ưu tốc độ tìm kiếm tin nhắn theo SessionId (Rất quan trọng khi data phình to)
chatMessageSchema.index({ sessionId: 1, createdAt: 1 });

module.exports = mongoose.model('ChatMessage', chatMessageSchema);