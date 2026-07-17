const { GoogleGenerativeAI } = require("@google/generative-ai");
const Groq = require("groq-sdk"); // Import thêm thư viện Groq

// Khởi tạo Key (Nhớ thêm GROQ_API_KEY vào file .env nhé)
const genAI = new GoogleGenerativeAI(process.env.GEMINI_API_KEY);
const groq = new Groq({ apiKey: process.env.GROQ_API_KEY });

// 🚀 FIX LỖI 500 TẠI ĐÂY: Export genAI ra để aiController có thể tái sử dụng
exports.genAI = genAI;

// THÊM THAM SỐ chatHistory VÀO ĐÂY (Mặc định là mảng rỗng nếu không truyền vào)
exports.generateChatReply = async (userMessage, chatHistory = []) => {
    const systemInstruction = `
Bạn là một AI Giáo viên tiếng Anh xuất sắc, linh hoạt và thực dụng cho người Việt.
Nhiệm vụ của bạn là hỗ trợ học viên trực tiếp trên ứng dụng di động.

Hãy ĐIỀU CHỈNH ĐỘ DÀI VÀ CHI TIẾT của câu trả lời dựa trên CÁCH HỌC VIÊN HỎI:

⚡ TRƯỜNG HỢP 1: HỎI NHANH / TRA TỪ (Học viên chỉ gõ 1 từ, hoặc hỏi ngắn gọn "nghĩa của từ X")
-> Phải trả lời SIÊU NGẮN GỌN trong đúng 3 dòng:
- Dòng 1: **[Từ vựng]** (Phiên âm) -> Nghĩa tiếng Việt.
- Dòng 2: Loại từ + Giải thích thêm cực ngắn (nếu có đa nghĩa).
- Dòng 3: *Ví dụ:* 1 câu tiếng Anh ngắn (kèm dịch nghĩa tiếng Việt).

📖 TRƯỜNG HỢP 2: YÊU CẦU CHI TIẾT (Học viên có các từ "giải thích chi tiết", "phân biệt", "cách dùng", hoặc hỏi ngữ pháp phức tạp)
-> Phải giải thích CẶN KẼ, CÓ CHIỀU SÂU nhưng TRÌNH BÀY DỄ NHÌN:
- Cấu trúc: Dùng các gạch đầu dòng (-), in đậm (**) để phân chia ý. 
- Nội dung: Giải thích quy tắc, sắc thái ý nghĩa (trang trọng/suồng sã), hoặc ngữ cảnh sử dụng.
- Phân biệt: Chủ động so sánh với các từ/cấu trúc dễ nhầm lẫn nếu thấy cần thiết.
- Ví dụ: Cung cấp ít nhất 2-3 câu ví dụ đa dạng (kèm dịch nghĩa).

🚫 QUY TẮC BẮT BUỘC CHUNG:
- Tuyệt đối KHÔNG chào hỏi rườm rà (Không viết: "Chào bạn", "Tuyệt quá", "Tôi sẽ giúp bạn..."). Đập ngay vào trọng tâm.
- Không viết những đoạn văn bản dài liền mạch. Bắt buộc ngắt dòng ngắn để tối ưu việc đọc trên màn hình điện thoại nhỏ.
`;

    // ==========================================
    // TẦNG 1: DÀN TRẬN GOOGLE GEMINI (Đã sửa lại tên model chuẩn)
    // ==========================================
    const modelsToTry = [
        "gemini-2.5-flash",
        "gemini-2.5-pro",
        "gemini-flash-latest",
        "gemini-3.5-flash"
    ];

    for (let i = 0; i < modelsToTry.length; i++) {
        const currentModel = modelsToTry[i];
        try {
            console.log(`🤖 Đang thử Gemini: ${currentModel}...`);
            const model = genAI.getGenerativeModel({
                model: currentModel,
                systemInstruction: systemInstruction
            });

            // CHUẨN BỊ LỊCH SỬ CHO GEMINI (Map từ format DB sang format Gemini)
            const geminiHistory = chatHistory.map(msg => ({
                role: msg.role === 'ai' ? 'model' : 'user', // Gemini chỉ nhận 'model' hoặc 'user'
                parts: [{ text: msg.text }]
            }));

            // Sử dụng startChat thay vì generateContent để duy trì mạch hội thoại
            const chat = model.startChat({
                history: geminiHistory
            });

            // Gửi câu hỏi mới nhất vào phiên chat vừa khởi tạo
            const result = await chat.sendMessage(userMessage);
            console.log(`✅ Thành công rực rỡ với: ${currentModel}`);
            return result.response.text(); // Trả kết quả và thoát hoàn toàn hàm

        } catch (error) {
            console.warn(`❌ Model ${currentModel} đang bận.`);
            if (i !== modelsToTry.length - 1) {
                console.log("🔄 Chuyển sang model Gemini dự phòng tiếp theo...");
            }
        }
    }

    // ==========================================
    // TẦNG 2: KÍCH HOẠT GROQ (Nếu Tầng 1 thất bại hoàn toàn)
    // ==========================================
    console.log("⚠️ TOÀN BỘ SERVER GEMINI ĐÃ SẬP! Kích hoạt máy chủ dự phòng Groq (Llama 3)...");
    
    try {
        // CHUẨN BỊ MẢNG TIN NHẮN CHO GROQ
        const groqMessages = [
            { role: "system", content: systemInstruction }
        ];

        // Đẩy lịch sử cũ vào mảng (Groq nhận 'assistant' thay vì 'ai' hoặc 'model')
        chatHistory.forEach(msg => {
            groqMessages.push({
                role: msg.role === 'ai' ? 'assistant' : 'user',
                content: msg.text
            });
        });

        // Cuối cùng đẩy câu hỏi mới nhất của User vào
        groqMessages.push({ role: "user", content: userMessage });

        const chatCompletion = await groq.chat.completions.create({
            messages: groqMessages,
            model: "llama3-8b-8192", // Model nhẹ, siêu tốc độ của Llama 3
        });

        console.log("✅ Groq Llama 3 đã cứu giá thành công!");
        return chatCompletion.choices[0].message.content;

    } catch (groqError) {
        console.error("❌ Cả Google và Groq đều sập!");
        throw new Error("Hiện tại toàn bộ hệ thống AI đều đang quá tải. Bạn thử lại sau vài phút nhé!");
    }
};

// ==========================================
// TẠO LỘ TRÌNH BẰNG AI (JSON FORMAT)
// ==========================================
exports.generateRoadmapJSON = async (target, currentLevel, freeTime) => {
    const prompt = `
Bạn là một chuyên gia thiết kế lộ trình học Tiếng Anh.
Học viên có thông tin sau:
- Mục tiêu: ${target}
- Trình độ hiện tại: ${currentLevel}
- Thời gian rảnh: ${freeTime}

Hãy tạo một lộ trình học tập gồm 4-5 chặng (steps). 
BẮT BUỘC trả về ĐÚNG định dạng JSON dưới đây, không kèm text giải thích, không dùng markdown (\`\`\`json):
{
  "motivationMessage": "Một câu động viên ngắn gọn, truyền cảm hứng dựa trên mục tiêu của họ",
  "steps": [
    {
      "title": "Tên chặng học (ngắn gọn)",
      "description": "Mô tả ngắn gọn nội dung chặng này",
      "maxProgress": <số lượng từ vựng cần học, là số nguyên từ 20 đến 100>
    }
  ]
}
`;

    try {
        // Dùng Gemini model với tính năng responseMimeType tối ưu cho JSON
        const model = genAI.getGenerativeModel({ 
            model: "gemini-2.5-flash",
            generationConfig: { responseMimeType: "application/json" } 
        });
        
        const result = await model.generateContent(prompt);
        let responseText = result.response.text();

        // Xử lý loại bỏ markdown \`\`\`json ... \`\`\` nếu Gemini lỡ sinh ra (Phòng hờ thêm)
        responseText = responseText.replace(/\`\`\`json/g, '').replace(/\`\`\`/g, '').trim();
        
        return JSON.parse(responseText); // Ép về Object JS
    } catch (error) {
        console.error("Lỗi khi Gen Roadmap từ Gemini:", error);
        throw new Error("Không thể tạo lộ trình từ AI");
    }
};