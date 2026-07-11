const aiService = require('../service/aiService'); // Giữ nguyên đường dẫn cũ của bạn
const ChatSession = require('../models/ChatSession'); // Import file model bạn vừa tạo ở Bước 1
const ChatMessage = require('../models/ChatMessage'); 

// ==========================================
// 1. TẠO PHIÊN CHAT MỚI (Mở phòng chat)
// ==========================================
exports.createChatSession = async (req, res) => {
    try {
        const { userId, chatType, firstMessage } = req.body;
        
        // Cắt 20 ký tự đầu của câu hỏi đầu tiên để làm Tiêu đề tự động
        const title = firstMessage ? firstMessage.substring(0, 20) + '...' : 'Cuộc trò chuyện mới';

        const newSession = new ChatSession({ userId, chatType, title });
        await newSession.save();

        res.status(201).json({ sessionId: newSession._id, title: newSession.title });
    } catch (error) {
        console.error("Lỗi tạo phiên chat:", error);
        res.status(500).json({ error: 'Lỗi khi tạo phiên chat trên hệ thống!' });
    }
};

// ==========================================
// 2. GỬI TIN NHẮN & LƯU DB (Nâng cấp từ code cũ)
// ==========================================
exports.chatWithAI = async (req, res) => {
  try {
    // Bây giờ nhận thêm sessionId từ Android gửi lên (nếu có)
    const { sessionId, message } = req.body;
    
    if (!message) {
      return res.status(400).json({ error: "Tin nhắn không được để trống!" });
    }

    // GIAI ĐOẠN 1: Nếu Android có gửi ID phòng chat lên -> Lưu tin nhắn của User vào DB
    if (sessionId) {
        const userMsg = new ChatMessage({ sessionId, role: 'user', text: message });
        await userMsg.save();

        // Đẩy phòng chat này lên đầu danh sách bằng cách cập nhật thời gian
        await ChatSession.findByIdAndUpdate(sessionId, { updatedAt: new Date() });
    }

    // GIAI ĐOẠN 2: Gọi AI từ service (Vẫn giữ nguyên hàm gọi 2 tầng phòng thủ của bạn)
    const aiReply = await aiService.generateChatReply(message);

    // GIAI ĐOẠN 3: Nhận kết quả từ AI -> Lưu vào DB
    if (sessionId) {
        const aiMsg = new ChatMessage({ sessionId, role: 'ai', text: aiReply });
        await aiMsg.save();
    }

    // Trả kết quả về cho điện thoại
    res.status(200).json({ reply: aiReply });
    
  } catch (error) {
    console.error("Lỗi chi tiết từ Google Gemini/Groq:", error);
    res.status(500).json({ error: `Lỗi từ AI: ${error.message}` });
  }
};

// ==========================================
// 3. LẤY LẠI LỊCH SỬ TIN NHẮN CŨ
// ==========================================
exports.getChatHistory = async (req, res) => {
    try {
        const { sessionId } = req.params;
        
        // Lấy tất cả tin nhắn thuộc về phòng chat này, sắp xếp cũ nhất -> mới nhất (1)
        const messages = await ChatMessage.find({ sessionId }).sort({ createdAt: 1 });
        
        res.status(200).json({ messages });
    } catch (error) {
        console.error("Lỗi lấy lịch sử:", error);
        res.status(500).json({ error: 'Lỗi khi lấy lịch sử chat' });
    }
};

// ==========================================
// 4. LẤY DANH SÁCH CÁC PHÒNG CHAT CỦA USER
// ==========================================
exports.getUserChatSessions = async (req, res) => {
    try {
        const { userId } = req.params;
        // Lấy tất cả phòng chat của user này, cái nào mới nhắn (updatedAt) thì xếp lên đầu
        const sessions = await ChatSession.find({ userId }).sort({ updatedAt: -1 });
        res.status(200).json({ sessions });
    } catch (error) {
        console.error("Lỗi lấy danh sách phòng chat:", error);
        res.status(500).json({ error: 'Lỗi máy chủ khi lấy danh sách phòng chat!' });
    }
};