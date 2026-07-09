const { GoogleGenerativeAI } = require("@google/generative-ai");
const genAI = new GoogleGenerativeAI(process.env.GEMINI_API_KEY);

exports.generateChatReply = async (userMessage) => {
    // Sửa lại model thành gemini-1.5-flash
    const model = genAI.getGenerativeModel({
      model: "gemini-2.5-flash",
      systemInstruction: "Bạn là một giáo viên tiếng Anh nhiệt tình. Nhiệm vụ của bạn là giải thích từ vựng, ngữ pháp cho người Việt. Hãy trả lời ngắn gọn, thân thiện và luôn có ví dụ minh họa."
    });

    const result = await model.generateContent(userMessage);
    return result.response.text(); 
};