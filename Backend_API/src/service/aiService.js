const { GoogleGenerativeAI } = require("@google/generative-ai");
const Groq = require("groq-sdk"); // Import thêm thư viện Groq

// Khởi tạo Key (Nhớ thêm GROQ_API_KEY vào file .env nhé)
const genAI = new GoogleGenerativeAI(process.env.GEMINI_API_KEY);
const groq = new Groq({ apiKey: process.env.GROQ_API_KEY });

exports.generateChatReply = async (userMessage) => {
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
    // TẦNG 1: DÀN TRẬN GOOGLE GEMINI
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

            const result = await model.generateContent(userMessage);
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
        const chatCompletion = await groq.chat.completions.create({
            messages: [
                { role: "system", content: systemInstruction },
                { role: "user", content: userMessage }
            ],
            model: "llama3-8b-8192", // Model nhẹ, siêu tốc độ của Llama 3
        });

        console.log("✅ Groq Llama 3 đã cứu giá thành công!");
        return chatCompletion.choices[0].message.content;

    } catch (groqError) {
        console.error("❌ Cả Google và Groq đều sập!");
        throw new Error("Hiện tại toàn bộ hệ thống AI đều đang quá tải. Bạn thử lại sau vài phút nhé!");
    }
};