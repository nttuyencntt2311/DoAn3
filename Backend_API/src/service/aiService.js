const { GoogleGenerativeAI } = require("@google/generative-ai");
const Groq = require("groq-sdk"); // Import thêm thư viện Groq

// Khởi tạo Key (Nhớ thêm GROQ_API_KEY vào file .env nhé)
const genAI = new GoogleGenerativeAI(process.env.GEMINI_API_KEY || "");
const groq = process.env.GROQ_API_KEY
  ? new Groq({ apiKey: process.env.GROQ_API_KEY })
  : null;

// 🚀 FIX LỖI 500 TẠI ĐÂY: Export genAI ra để aiController có thể tái sử dụng
exports.genAI = genAI;

// THÊM THAM SỐ chatHistory VÀO ĐÂY (Mặc định là mảng rỗng nếu không truyền vào)
exports.generateChatReply = async (userMessage, chatHistory = [], xaiContext = null) => {
    const xaiBlock = xaiContext ? `
HỒ SƠ HỌC TẬP & XAI THỰC TẾ CỦA HỌC VIÊN HIỆN TẠI (chỉ dùng số liệu này, không bịa thêm):
- Học viên: ${xaiContext.userName} (Cấp ${xaiContext.level}, chuỗi ${xaiContext.streakDays} ngày)
- Độ chính xác lịch sử: ${xaiContext.accuracyPercent}% | Tốc độ suy nghĩ TB: ${xaiContext.avgThinkingSec} giây/câu
- Các từ vựng học viên đang hay sai / dễ quên: ${(xaiContext.weakWords || []).join(", ") || "Chưa có"}
${xaiContext.predictedAccuracy !== null ? `- Xác suất thành thạo XAI dự kiến: ${xaiContext.predictedAccuracy}%` : ""}
${(xaiContext.negativeFactors || []).length > 0 ? `- Yếu tố rủi ro XAI: ${xaiContext.negativeFactors.join("; ")}` : ""}
${xaiContext.counterfactual ? `- Gợi ý cải thiện What-If: ${xaiContext.counterfactual}` : ""}
` : "";

    const systemInstruction = `
Bạn là Gia sư Tiếng Anh AI thông minh (LexiVT XAI) hỗ trợ học viên Việt Nam trên điện thoại.
${xaiBlock}
Hãy hiểu ĐÚNG Ý ĐỊNH của học viên (KỂ CẢ KHI HỌ GÕ TIẾNG VIỆT KHÔNG DẤU như "thi qua khu" = Thì quá khứ, "hien tai don" = Thì hiện tại đơn, "cau bi dong" = Câu bị động, "cau dieu kien" = Câu điều kiện...):

📘 TRƯỜNG HỢP 1: HỎI NGỮ PHÁP / CHỦ ĐIỂM TIẾNG ANH (ví dụ: "thi qua khu", "hiện tại hoàn thành", "phân biệt a/an/the", "câu bị động"...)
-> Giải thích rõ ràng, dễ hiểu trên điện thoại bằng gạch đầu dòng ngắn:
- **Công thức:** Khẳng định / Phủ định / Nghi vấn (ngắn gọn).
- **Cách dùng & Dấu hiệu nhận biết:** 2–3 ý cốt lõi nhất.
- **Ví dụ:** 2 câu ví dụ kèm dịch tiếng Việt. Nếu học viên có danh sách *từ vựng hay sai / dễ quên* ở trên, hãy chủ động lồng 1 từ vào ví dụ một cách tự nhiên và thêm 1 dòng nhỏ: *💡 Ôn kèm từ bạn hay nhầm: **[từ]***.

⚡ TRƯỜNG HỢP 2: TRA TỪ VỰNG TIẾNG ANH (CHỈ áp dụng khi học viên gõ từ/cụm từ tiếng Anh hoặc hỏi rõ "nghĩa của từ X")
-> Trả lời gọn 3 dòng:
- **[Từ vựng]** /phiên âm/ → Nghĩa tiếng Việt.
- Loại từ + giải thích sắc thái cực ngắn.
- *Ví dụ:* 1 câu tiếng Anh ngắn (kèm dịch tiếng Việt).

🧠 TRƯỜNG HỢP 3: HỎI VỀ NĂNG LỰC, ĐIỂM YẾU, HOẶC GỢI Ý ÔN TẬP (ví dụ: "mình đang yếu gì?", "hôm nay nên học gì?", "đánh giá giúp mình")
-> Dựa trực tiếp vào HỒ SƠ HỌC TẬP & XAI ở trên để phân tích ngắn gọn, thân thiện (không dùng thuật ngữ kỹ thuật như 'SHAP') và đưa ra 1–2 việc cần làm ngay.

🚫 QUY TẮC BẮT BUỘC:
- KHÔNG chào hỏi rườm rà, đi thẳng vào trọng tâm.
- Ngắt dòng ngắn, dùng **in đậm** và gạch đầu dòng để dễ đọc trên màn hình điện thoại.
`;

    // ==========================================
    // TẦNG 1: DÀN TRẬN GOOGLE GEMINI (Đã sửa lại tên model chuẩn)
    // ==========================================
    const modelsToTry = [
        "gemini-2.5-flash",
        "gemini-2.5-pro",
        "gemini-2.0-flash"
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
    
    if (!groq) {
        throw new Error("Hệ thống Gemini đang bận và máy chủ dự phòng Groq chưa được cấu hình (GROQ_API_KEY).");
    }

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

        const groqModels = [
            process.env.GROQ_MODEL || "llama-3.1-8b-instant",
            "llama-3.3-70b-versatile"
        ];
        for (const groqModel of groqModels) {
            try {
                const chatCompletion = await groq.chat.completions.create({
                    messages: groqMessages,
                    model: groqModel,
                });
                console.log(`✅ Groq (${groqModel}) đã cứu giá thành công!`);
                return chatCompletion.choices[0].message.content;
            } catch (mErr) {
                console.warn(`⚠️ Groq model ${groqModel} thất bại:`, mErr.message);
            }
        }
        throw new Error("Tất cả model Groq dự phòng đều không phản hồi.");

    } catch (groqError) {
        console.error("❌ Cả Google và Groq đều sập!");
        throw new Error("Hiện tại toàn bộ hệ thống AI đều đang quá tải. Bạn thử lại sau vài phút nhé!");
    }
};

// ==========================================
// TẠO LỘ TRÌNH BẰNG AI (JSON FORMAT)
// ==========================================
exports.generateRoadmapJSON = async (target, currentLevel, freeTime, xaiContext = null) => {
    const xaiBlock = xaiContext ? `
HỒ SƠ HÀNH VI & PHÂN TÍCH XAI (SHAP) THỰC TẾ CỦA HỌC VIÊN:
- Tên học viên: ${xaiContext.userName || "Học viên"} (Cấp ${xaiContext.level || 1}, Chuỗi ${xaiContext.streakDays || 0} ngày liên tiếp)
- Mong muốn cụ thể của học viên: ${xaiContext.expectation || "Không có ghi chú thêm"}
- Xác suất làm đúng dự kiến hiện tại: ${xaiContext.predictedAccuracy}%
- Các chủ đề yếu cần ưu tiên: ${(xaiContext.weakCategories || []).join(", ") || "Chưa có dữ liệu"}
- Các từ vựng dễ quên / hay sai gần đây: ${(xaiContext.weakWords || []).slice(0, 8).join(", ") || "Chưa có"}
- Top yếu tố rủi ro từ SHAP: ${(xaiContext.negativeFactors || []).join("; ") || "Chưa phát hiện rủi ro lớn"}
` : "";

    const prompt = `
Bạn là một chuyên gia thiết kế lộ trình học Tiếng Anh ứng dụng Explainable AI (EdTech XAI).
Học viên có thông tin sau:
- Mục tiêu: ${target}
- Trình độ hiện tại: ${currentLevel}
- Thời gian rảnh: ${freeTime}
${xaiBlock}
Hãy tạo một lộ trình học tập cá nhân hóa gồm 4 chặng (steps) đi sâu ĐÚNG VÀO CHỦ ĐỀ/MỤC TIÊU "${target}" (TUYỆT ĐỐI không dùng các từ chung chung vô nghĩa như "Tất cả" hay "Tổng hợp" trong tên chặng):
- Chặng 1: Từ vựng nền tảng cốt lõi của chủ đề "${target}".
- Chặng 2: Cụm từ & tình huống ngữ cảnh thực tế của chủ đề "${target}".
- Chặng 3: Vá các từ vựng dễ quên & khắc phục trực tiếp rủi ro SHAP/Ebbinghaus của học viên.
- Chặng 4: Kiểm tra tổng hợp (Mastery Test) & phản xạ nâng cao cho chủ đề "${target}".
BẮT BUỘC trả về ĐÚNG định dạng JSON dưới đây, không kèm text giải thích, không dùng markdown (\`\`\`json):
{
  "motivationMessage": "Một câu động viên ngắn gọn, truyền cảm hứng dựa trên mục tiêu và hồ sơ hành vi của họ",
  "steps": [
    {
      "title": "Tên chặng học (ngắn gọn)",
      "description": "Mô tả ngắn gọn nội dung chặng này",
      "xaiReason": "Giải thích ngắn 1 câu (<= 25 từ) vì sao AI gợi ý chặng này dựa trên hồ sơ XAI/điểm yếu của học viên",
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
// ==========================================
// SHAP-TO-TEXT CÓ KIỂM SOÁT (GROUNDED PEDAGOGICAL FEEDBACK)
// ==========================================
exports.generateGroundedXaiFeedback = async ({
    predictedAccuracy,
    detailedShap = [],
    counterfactualSummary = "",
    wrongWords = []
}) => {
    const topShap = (detailedShap || []).slice(0, 4).map(
        (s) => `- ${s.vi_description}: giá trị=${s.value}, SHAP=${s.shap_score > 0 ? "+" : ""}${s.shap_score}`
    ).join("\n");

    const fallbackText = counterfactualSummary
        ? `Xác suất thành thạo dự kiến của bạn là ${predictedAccuracy}%. ${counterfactualSummary}`
        : `Xác suất thành thạo dự kiến của bạn là ${predictedAccuracy}%. Hãy ôn kỹ các từ còn nhầm và giữ tốc độ đọc đề ổn định.`;

    if (!process.env.GEMINI_API_KEY) {
        return fallbackText;
    }

    const prompt = `
Bạn là gia sư tiếng Anh giải thích kết quả phân tích XAI (SHAP) cho học viên Việt Nam.
CHỈ ĐƯỢC DỰA TRÊN SỐ LIỆU THỰC TẾ DƯỚI ĐÂY (tuyệt đối không bịa thêm chỉ số khác):
- Xác suất làm đúng dự kiến: ${predictedAccuracy}%
- Chi tiết đóng góp SHAP (dương = giúp làm đúng, âm = kéo điểm xuống):
${topShap || "- Không có chi tiết SHAP"}
- Gợi ý phản thực (What-If): ${counterfactualSummary || "Không có"}
- Từ vựng vừa làm sai: ${(wrongWords || []).join(", ") || "Không có"}

Hãy viết ĐÚNG 2-3 câu tiếng Việt tự nhiên, ấm áp, dễ hiểu (không dùng thuật ngữ kỹ thuật như 'SHAP' hay tên biến tiếng Anh) giải thích lý do kết quả và 1 hành động cụ thể nên làm ngay. Không chào hỏi rườm rà.
`;

    try {
        const model = genAI.getGenerativeModel({ model: "gemini-2.5-flash" });
        const result = await model.generateContent(prompt);
        const text = result.response.text().trim();
        return text || fallbackText;
    } catch (err) {
        console.warn("⚠️ Fallback SHAP-to-Text:", err.message);
        return fallbackText;
    }
};
