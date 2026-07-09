const axios = require("axios");
const Vocabulary = require("../models/Vocabulary");

// 1. Hàm lấy danh sách tất cả từ vựng (GET)
exports.getAllVocabularies = async (req, res) => {
  try {
    const vocabs = await Vocabulary.find();
    res.status(200).json(vocabs);
  } catch (error) {
    res.status(500).json({ message: "Lỗi server", error: error.message });
  }
};

// 2. Hàm lấy từ vựng: Kiểm tra DB trước, không có thì gọi Dictionary API (GET)
exports.getOrFetchVocabulary = async (req, res) => {
  const { word } = req.params;

  try {
    // 1. Tìm trong DB
    let vocab = await Vocabulary.findOne({ word: word.toLowerCase() });

    if (vocab) {
      return res.status(200).json(vocab); // Tìm thấy trong DB
    }

    // 2. Không thấy, gọi Dictionary API
    const response = await axios.get(
      `https://api.dictionaryapi.dev/api/v2/entries/en/${word}`,
    );
    const data = response.data[0];

    // 3. Xử lý dữ liệu trả về để lưu vào DB
    const newVocab = new Vocabulary({
      word: data.word,
      phonetic: data.phonetic || "",
      meaning:
        data.meanings?.[0]?.definitions?.[0]?.definition ||
        "Chưa có định nghĩa",
      defEng: data.meanings?.[0]?.definitions?.[0]?.definition || "",
    });

    await newVocab.save();
    res.status(200).json(newVocab);
  } catch (error) {
    if (error.response && error.response.status === 404) {
      return res
        .status(404)
        .json({ message: "Từ vựng không tồn tại trong từ điển" });
    }
    res.status(500).json({
      message: "Không tìm thấy từ vựng hoặc lỗi server",
      error: error.message,
    });
  }
};

// 3. Hàm thêm từ vựng mới thủ công (POST)
exports.createVocab = async (req, res) => {
  try {
    const newVocab = new Vocabulary(req.body);
    await newVocab.save();
    res.status(201).json(newVocab);
  } catch (error) {
    res.status(400).json({ message: "Lỗi thêm từ vựng", error: error.message });
  }
};

// 4. Hàm cập nhật từ vựng (PUT)
exports.updateVocab = async (req, res) => {
  try {
    const updatedVocab = await Vocabulary.findByIdAndUpdate(
      req.params.id,
      req.body,
      { new: true },
    );
    res.status(200).json(updatedVocab);
  } catch (error) {
    res.status(400).json({ message: "Lỗi cập nhật", error: error.message });
  }
};

// 5. Hàm xóa từ vựng (DELETE)
exports.deleteVocab = async (req, res) => {
  try {
    await Vocabulary.findByIdAndDelete(req.params.id);
    res.status(200).json({ message: "Đã xóa từ vựng thành công" });
  } catch (error) {
    res.status(500).json({ message: "Lỗi khi xóa", error: error.message });
  }
};

// 🚀 6. Hàm CẬP NHẬT ĐIỂM SỐ từ thẻ Flashcard Android (MỚI THÊM)
exports.updateProficiency = async (req, res) => {
  try {
    const { id } = req.params;
    const { proficiency } = req.body;

    // Cập nhật điểm cho từ vựng có ID tương ứng
    const updatedVocab = await Vocabulary.findByIdAndUpdate(
      id,
      { proficiency: proficiency },
      { new: true },
    );

    if (!updatedVocab) {
      return res
        .status(404)
        .json({ message: "Không tìm thấy từ vựng này trong Database" });
    }

    res.status(200).json({
      message: "Cập nhật điểm thành công",
      data: updatedVocab,
    });
  } catch (error) {
    res
      .status(500)
      .json({ message: "Lỗi server khi cập nhật điểm", error: error.message });
  }
};

// 7. Hàm lấy Thống kê
exports.getVocabStats = async (req, res) => {
  try {
    const totalWords = await Vocabulary.countDocuments();
    const masteredWords = await Vocabulary.countDocuments({
      proficiency: { $gte: 80 },
    });
    const learningWords = await Vocabulary.countDocuments({
      proficiency: { $gt: 0, $lt: 80 },
    });
    const newWords = await Vocabulary.countDocuments({ proficiency: 0 });
    const masteryPercentage =
      totalWords === 0 ? 0 : Math.round((masteredWords / totalWords) * 100);

    res.status(200).json({
      totalWords,
      masteryPercentage,
      masteredWords,
      learningWords,
      newWords,
      newWordsThisWeek: 15,
    });
  } catch (error) {
    res
      .status(500)
      .json({ message: "Lỗi khi lấy thống kê", error: error.message });
  }
};

// 8. Hàm tạo dữ liệu tự động (Seeding)
exports.seedDefaultVocabulary = async (req, res) => {
  try {
    const { roadmapId } = req.body;

    if (!roadmapId) {
      return res.status(400).json({ message: "Vui lòng cung cấp roadmapId!" });
    }

    // 1. Dán toàn bộ mảng 100 từ vựng vào đây
    const wordList = [
        // --- CHỦ ĐỀ 1: HỢP ĐỒNG & KINH DOANH ---
        { word: "contract", meaning: "Hợp đồng", phonetic: "/ˈkɑːn.trækt/" },
        { word: "agreement", meaning: "Thỏa thuận", phonetic: "/əˈɡriː.mənt/" },
        { word: "signature", meaning: "Chữ ký", phonetic: "/ˈsɪɡ.nə.tʃɚ/" },
        { word: "negotiate", meaning: "Đàm phán", phonetic: "/nəˈɡoʊ.ʃi.eɪt/" },
        { word: "condition", meaning: "Điều kiện", phonetic: "/kənˈdɪʃ.ən/" },
        { word: "guarantee", meaning: "Bảo hành, đảm bảo", phonetic: "/ˌɡer.ənˈtiː/" },
        { word: "obligate", meaning: "Bắt buộc", phonetic: "/ˈɑː.blə.ɡeɪt/" },
        { word: "provision", meaning: "Điều khoản", phonetic: "/prəˈvɪʒ.ən/" },
        { word: "resolve", meaning: "Giải quyết", phonetic: "/rɪˈzɑːlv/" },
        { word: "specific", meaning: "Cụ thể, đặc thù", phonetic: "/spəˈsɪf.ɪk/" },
        { word: "cancel", meaning: "Hủy bỏ", phonetic: "/ˈkæn.səl/" },
        { word: "establish", meaning: "Thành lập", phonetic: "/ɪˈstæb.lɪʃ/" },
        { word: "party", meaning: "Bên (trong hợp đồng)", phonetic: "/ˈpɑːr.t̬i/" },
        { word: "binding", meaning: "Ràng buộc", phonetic: "/ˈbaɪn.dɪŋ/" },
        { word: "breach", meaning: "Vi phạm", phonetic: "/briːtʃ/" },
        { word: "clause", meaning: "Điều khoản", phonetic: "/klɑːz/" },
        { word: "commit", meaning: "Cam kết", phonetic: "/kəˈmɪt/" },
        { word: "comply", meaning: "Tuân thủ", phonetic: "/kəmˈplaɪ/" },
        { word: "draft", meaning: "Bản nháp", phonetic: "/dræft/" },
        { word: "valid", meaning: "Có hiệu lực", phonetic: "/ˈvæl.ɪd/" },

        // --- CHỦ ĐỀ 2: MÔI TRƯỜNG VĂN PHÒNG ---
        { word: "colleague", meaning: "Đồng nghiệp", phonetic: "/ˈkɑː.liːɡ/" },
        { word: "manager", meaning: "Người quản lý", phonetic: "/ˈmæn.ə.dʒɚ/" },
        { word: "meeting", meaning: "Cuộc họp", phonetic: "/ˈmiː.t̬ɪŋ/" },
        { word: "agenda", meaning: "Chương trình nghị sự", phonetic: "/əˈdʒen.də/" },
        { word: "deadline", meaning: "Hạn chót", phonetic: "/ˈded.laɪn/" },
        { word: "document", meaning: "Tài liệu", phonetic: "/ˈdɑː.kjə.mənt/" },
        { word: "schedule", meaning: "Lịch trình", phonetic: "/ˈskedʒ.uːl/" },
        { word: "workplace", meaning: "Nơi làm việc", phonetic: "/ˈwɝːk.pleɪs/" },
        { word: "promote", meaning: "Thăng chức", phonetic: "/prəˈmoʊt/" },
        { word: "evaluate", meaning: "Đánh giá", phonetic: "/ɪˈvæl.ju.eɪt/" },
        { word: "attend", meaning: "Tham gia", phonetic: "/əˈtend/" },
        { word: "department", meaning: "Phòng ban", phonetic: "/dɪˈpɑːrt.mənt/" },
        { word: "efficiency", meaning: "Hiệu quả", phonetic: "/ɪˈfɪʃ.ən.si/" },
        { word: "flexible", meaning: "Linh hoạt", phonetic: "/ˈflek.sə.bəl/" },
        { word: "overtime", meaning: "Làm thêm giờ", phonetic: "/ˈoʊ.vɚ.taɪm/" },
        { word: "submit", meaning: "Nộp", phonetic: "/səbˈmɪt/" },
        { word: "urgent", meaning: "Khẩn cấp", phonetic: "/ˈɝː.dʒənt/" },
        { word: "assignment", meaning: "Nhiệm vụ", phonetic: "/əˈsaɪn.mənt/" },
        { word: "supervisor", meaning: "Người giám sát", phonetic: "/ˈsuː.pɚ.vaɪ.zɚ/" },
        { word: "presentation", meaning: "Bài thuyết trình", phonetic: "/ˌprez.ənˈteɪ.ʃən/" },

        // --- CHỦ ĐỀ 3: HỆ THỐNG THÔNG TIN & CÔNG NGHỆ ---
        { word: "developer", meaning: "Lập trình viên", phonetic: "/dɪˈvel.ə.pɚ/" },
        { word: "database", meaning: "Cơ sở dữ liệu", phonetic: "/ˈdeɪ.t̬ə.beɪs/" },
        { word: "software", meaning: "Phần mềm", phonetic: "/ˈsɑːft.wer/" },
        { word: "hardware", meaning: "Phần cứng", phonetic: "/ˈhɑːrd.wer/" },
        { word: "network", meaning: "Mạng lưới", phonetic: "/ˈnet.wɝːk/" },
        { word: "server", meaning: "Máy chủ", phonetic: "/ˈsɝː.vɚ/" },
        { word: "innovation", meaning: "Sự đổi mới", phonetic: "/ˌɪn.əˈveɪ.ʃən/" },
        { word: "interface", meaning: "Giao diện", phonetic: "/ˈɪn.t̬ɚ.feɪs/" },
        { word: "access", meaning: "Truy cập", phonetic: "/ˈæk.ses/" },
        { word: "configure", meaning: "Cấu hình", phonetic: "/kənˈfɪɡ.jɚ/" },
        { word: "install", meaning: "Cài đặt", phonetic: "/ɪnˈstɑːl/" },
        { word: "update", meaning: "Cập nhật", phonetic: "/ʌpˈdeɪt/" },
        { word: "upgrade", meaning: "Nâng cấp", phonetic: "/ʌpˈɡreɪd/" },
        { word: "monitor", meaning: "Giám sát, màn hình", phonetic: "/ˈmɑː.nə.t̬ɚ/" },
        { word: "security", meaning: "Bảo mật", phonetic: "/səˈkjʊr.ə.t̬i/" },
        { word: "system", meaning: "Hệ thống", phonetic: "/ˈsɪs.təm/" },
        { word: "troubleshoot", meaning: "Khắc phục sự cố", phonetic: "/ˈtrʌb.əl.ʃuːt/" },
        { word: "variable", meaning: "Biến số", phonetic: "/ˈver.i.ə.bəl/" },
        { word: "logic", meaning: "Tính logic", phonetic: "/ˈlɑː.dʒɪk/" },
        { word: "algorithm", meaning: "Thuật toán", phonetic: "/ˈæl.ɡə.rɪ.ðəm/" },

        // --- CHỦ ĐỀ 4: KẾ TOÁN & TÀI CHÍNH ---
        { word: "budget", meaning: "Ngân sách", phonetic: "/ˈbʌdʒ.ɪt/" },
        { word: "revenue", meaning: "Doanh thu", phonetic: "/ˈrev.ə.nuː/" },
        { word: "expense", meaning: "Chi phí", phonetic: "/ɪkˈspens/" },
        { word: "profit", meaning: "Lợi nhuận", phonetic: "/ˈprɑː.fɪt/" },
        { word: "invest", meaning: "Đầu tư", phonetic: "/ɪnˈvest/" },
        { word: "audit", meaning: "Kiểm toán", phonetic: "/ˈɑː.dɪt/" },
        { word: "balance", meaning: "Số dư", phonetic: "/ˈbæl.əns/" },
        { word: "invoice", meaning: "Hóa đơn", phonetic: "/ˈɪn.vɔɪs/" },
        { word: "tax", meaning: "Thuế", phonetic: "/tæks/" },
        { word: "transaction", meaning: "Giao dịch", phonetic: "/trænˈzæk.ʃən/" },
        { word: "fund", meaning: "Quỹ", phonetic: "/fʌnd/" },
        { word: "loan", meaning: "Khoản vay", phonetic: "/loʊn/" },
        { word: "debt", meaning: "Khoản nợ", phonetic: "/det/" },
        { word: "interest", meaning: "Lãi suất", phonetic: "/ˈɪn.trɪst/" },
        { word: "payment", meaning: "Sự thanh toán", phonetic: "/ˈpeɪ.mənt/" },
        { word: "estimate", meaning: "Ước tính", phonetic: "/ˈes.tə.meɪt/" },
        { word: "currency", meaning: "Tiền tệ", phonetic: "/ˈkɝː.ən.si/" },
        { word: "discount", meaning: "Giảm giá", phonetic: "/ˈdɪs.kaʊnt/" },
        { word: "refund", meaning: "Hoàn tiền", phonetic: "/ˈriː.fʌnd/" },
        { word: "receipt", meaning: "Biên lai", phonetic: "/rɪˈsiːt/" },

        // --- CHỦ ĐỀ 5: DỊCH VỤ & KHÁCH HÀNG ---
        { word: "customer", meaning: "Khách hàng", phonetic: "/ˈkʌs.tə.mɚ/" },
        { word: "service", meaning: "Dịch vụ", phonetic: "/ˈsɝː.vɪs/" },
        { word: "feedback", meaning: "Phản hồi", phonetic: "/ˈfiːd.bæk/" },
        { word: "satisfaction", meaning: "Sự hài lòng", phonetic: "/ˌsæt̬.ɪsˈfæk.ʃən/" },
        { word: "complaint", meaning: "Lời phàn nàn", phonetic: "/kəmˈpleɪnt/" },
        { word: "warranty", meaning: "Bảo hành", phonetic: "/ˈwɔːr.ən.t̬i/" },
        { word: "delivery", meaning: "Giao hàng", phonetic: "/dɪˈlɪv.ɚ.i/" },
        { word: "order", meaning: "Đơn hàng", phonetic: "/ˈɔːr.dɚ/" },
        { word: "loyalty", meaning: "Sự trung thành", phonetic: "/ˈlɔɪ.əl.t̬i/" },
        { word: "inquiry", meaning: "Yêu cầu cung cấp thông tin", phonetic: "/ˈɪn.kwə.ri/" },
        { word: "assist", meaning: "Hỗ trợ", phonetic: "/əˈsɪst/" },
        { word: "resolve", meaning: "Giải quyết", phonetic: "/rɪˈzɑːlv/" },
        { word: "policy", meaning: "Chính sách", phonetic: "/ˈpɑː.lə.si/" },
        { word: "priority", meaning: "Sự ưu tiên", phonetic: "/praɪˈɔːr.ə.t̬i/" },
        { word: "replace", meaning: "Thay thế", phonetic: "/rɪˈpleɪs/" },
        { word: "respond", meaning: "Phản hồi", phonetic: "/rɪˈspɑːnd/" },
        { word: "survey", meaning: "Khảo sát", phonetic: "/ˈsɝː.veɪ/" },
        { word: "upgrade", meaning: "Nâng cấp", phonetic: "/ʌpˈɡreɪd/" },
        { word: "apologize", meaning: "Xin lỗi", phonetic: "/əˈpɑː.lə.dʒaɪz/" },
        { word: "recommend", meaning: "Đề xuất", phonetic: "/ˌrek.əˈmend/" }
    ];

    let addedCount = 0;
    const failedWords = [];

    // 2. VÒNG LẶP ĐÃ ĐƯỢC CẬP NHẬT GỌN GÀNG TẠI ĐÂY
    for (const item of wordList) {
      const existingWord = await Vocabulary.findOne({ word: { $regex: new RegExp(`^${item.word}$`, "i") } });
      if (existingWord) continue;

      try {
        const newVocab = new Vocabulary({
          word: item.word,
          phonetic: item.phonetic,
          meaning: item.meaning, // Nghĩa tiếng Việt
          category: "Tổng hợp",
          proficiency: 0,
          roadmapId: roadmapId,
        });

        await newVocab.save();
        addedCount++;
      } catch (err) {
        failedWords.push(item.word);
      }
    }

    res.status(200).json({
      message: "Đã gieo dữ liệu (Seeding) thành công!",
      wordsAdded: addedCount,
      failedWords: failedWords,
    });
} catch (err) {
        // Thêm dòng này để lỡ có lỗi, Terminal sẽ in ra tiếng Việt rõ ràng luôn
        console.log(`Lỗi không thể lưu từ [${item.word}]: ${err.message}`); 
        
        failedWords.push(item.word);
      }
  
};