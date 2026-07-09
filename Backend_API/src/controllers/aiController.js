const aiService = require('../service/aiService'); // Chú ý đường dẫn thư mục service của bạn nhé

exports.chatWithAI = async (req, res) => {
  try {
    const { message } = req.body;
    if (!message) {
      return res.status(400).json({ error: "Tin nhắn không được để trống!" });
    }

    const aiReply = await aiService.generateChatReply(message);
    res.status(200).json({ reply: aiReply });
    
  } catch (error) {
    // In lỗi ra terminal
    console.error("Lỗi chi tiết từ Google Gemini:", error);
    
    // GỬI THẲNG LỖI THẬT VỀ APP ANDROID ĐỂ BẠN DỄ ĐỌC
    res.status(500).json({ 
        error: `Lỗi từ Gemini: ${error.message}` 
    });
  }
};