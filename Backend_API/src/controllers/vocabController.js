const axios = require("axios");
const Vocabulary = require("../models/Vocabulary");
const { updateStreak } = require("../service/streakService");
const xaiService = require("../service/xaiService");
const User = require("../models/User");
// 1. Hàm lấy danh sách tất cả từ vựng (GET)
const jwt = require("jsonwebtoken"); // 🚀 Nhớ thêm dòng này ở đầu file nếu chưa có

exports.getAllVocabularies = async (req, res) => {
  try {
    const vocabularies = await Vocabulary.find();

    // 1. Tự động kiểm tra Token ngầm (Không chặn đứng như verifyToken)
    let userId = null;
    const authHeader = req.header("Authorization");
    if (authHeader && authHeader.startsWith("Bearer ")) {
      const token = authHeader.split(" ")[1];
      try {
        const decoded = jwt.verify(
          token,
          process.env.JWT_SECRET || "your_jwt_secret_key",
        );
        userId = decoded.userId;
      } catch (err) {
        // Token sai hoặc hết hạn thì bỏ qua, coi như không có
      }
    }

    // 2. Nếu có User ID (Từ App gọi lên) -> Trả về danh sách có điểm
    if (userId) {
      const user = await User.findById(userId);
      if (user) {
        const progressList = user.vocabProgress || [];
        const progressMap = {};

        progressList.forEach((p) => {
          const id = p.wordId || p.word || p._id;
          if (id) progressMap[id.toString()] = p.proficiency;
        });

        const personalizedVocab = vocabularies.map((v) => {
          const vObj = v.toObject();
          vObj.proficiency = progressMap[vObj._id.toString()] || 0;
          return vObj;
        });

        return res.status(200).json(personalizedVocab);
      }
    }

    // 3. Nếu không có Token (Từ Admin gọi lên) -> Trả về danh sách gốc (Điểm 0)
    const defaultVocab = vocabularies.map((v) => {
      const vObj = v.toObject();
      vObj.proficiency = 0;
      return vObj;
    });

    res.status(200).json(defaultVocab);
  } catch (error) {
    console.error("Lỗi getAllVocabularies:", error);
    res.status(500).json({ message: "Lỗi server khi lấy danh sách" });
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

// 🚀 6. Hàm CẬP NHẬT ĐIỂM SỐ từ thẻ Flashcard Android
exports.updateProficiency = async (req, res) => {
  try {
    const { id } = req.params; // wordId

    // 🚀 ĐÃ BỔ SUNG LẠI DÒNG NÀY ĐỂ LẤY ĐIỂM SỐ TỪ ANDROID
    const { proficiency } = req.body;

    // Lấy userId từ Token (bảo mật nhất), dự phòng lấy từ body
    const userId =
      req.user && req.user.userId ? req.user.userId : req.body.userId;

    if (!userId) {
      return res
        .status(400)
        .json({ message: "Thiếu userId để cập nhật tiến độ cá nhân." });
    }

    const user = await User.findById(userId);
    if (!user) {
      return res.status(404).json({ message: "Không tìm thấy người dùng." });
    }

    if (!user.vocabProgress) {
      user.vocabProgress = [];
    }

    // Quét cả wordId và word phòng trường hợp Schema đặt tên khác
    const vocabIndex = user.vocabProgress.findIndex(
      (v) =>
        (v.wordId && v.wordId.toString() === id) ||
        (v.word && v.word.toString() === id),
    );

    let isNewlyMastered = false;

    if (vocabIndex > -1) {
      // Đã từng học -> Cập nhật điểm
      const oldProficiency = user.vocabProgress[vocabIndex].proficiency || 0;
      user.vocabProgress[vocabIndex].proficiency = proficiency;

      // ĐỒNG BỘ ĐIỂM (Dùng 80 cho khớp 100% với Frontend Android)
      if (oldProficiency < 80 && proficiency >= 80) {
        isNewlyMastered = true;
      }

      // Lệnh BẮT BUỘC để Mongoose chịu lưu sự thay đổi bên trong mảng
      user.markModified("vocabProgress");
    } else {
      // Chưa từng học -> Thêm mới
      user.vocabProgress.push({
        word: id,
        wordId: id,
        proficiency: proficiency,
        lastReviewedDate: new Date(), // 🚀 BỔ SUNG Ở ĐÂY
        reviewCount: 1, // 🚀 BỔ SUNG Ở ĐÂY
      });

      if (proficiency >= 80) {
        isNewlyMastered = true;
      }
    }

    // ĐỒNG BỘ DỮ LIỆU APP & ADMIN
    if (isNewlyMastered) {
      // ❌ Xóa bỏ dòng cộng dồn cũ dễ gây sai số: user.wordsLearned = (user.wordsLearned || 0) + 1;

      // Cộng điểm XP và cập nhật lại cấp độ theo công thức lũy tiến
      user.xp = (user.xp || 0) + 5;
      user.level = Math.floor(Math.sqrt(Math.max(0, user.xp) / 100)) + 1;
    }

    // 🚀 BỔ SUNG LỆNH NÀY: Bắt Backend tự quét mảng thực tế và đếm lại toàn bộ từ >= 80 điểm
    // Con số này sẽ được đè thẳng lên số 45 cũ, đảm bảo Admin luôn khớp 100% với App
    user.wordsLearned = user.vocabProgress.filter(
      (v) => v.proficiency >= 80,
    ).length;

    // Lưu tiến độ từ vựng trước (KHÔNG ghi đè lastStudyDate tại đây để updateStreak tính đúng khoảng cách ngày)
    await user.save();

    const streakUser = await updateStreak(userId);
    const currentStreakDays = streakUser ? streakUser.streakDays : (user.streakDays || 0);

    res.status(200).json({
      message: "Cập nhật tiến độ học thành công",
      wordsLearned: user.wordsLearned,
      xp: user.xp,
      streakDays: currentStreakDays, // Trả về đúng số nguyên streakDays
    });
  } catch (error) {
    console.error("Lỗi khi updateProficiency:", error);
    res
      .status(500)
      .json({ message: "Lỗi server khi cập nhật điểm", error: error.message });
  }
};
// 7. Hàm lấy Thống kê (Cá nhân hóa theo từng học viên)
exports.getVocabStats = async (req, res) => {
  try {
    const totalWords = await Vocabulary.countDocuments();

    // Lấy userId từ verifyToken (hoặc giải mã Bearer token nếu route cũ không gắn middleware)
    let userId = req.user ? (req.user.userId || req.user.id) : null;
    if (!userId) {
      const authHeader = req.header("Authorization");
      if (authHeader && authHeader.startsWith("Bearer ")) {
        try {
          const decoded = jwt.verify(
            authHeader.split(" ")[1],
            process.env.JWT_SECRET || "your_jwt_secret_key"
          );
          userId = decoded.userId || decoded.id;
        } catch (err) {
          userId = null;
        }
      }
    }

    let masteredWords = 0;
    let learningWords = 0;

    if (userId) {
      const user = await User.findById(userId);
      const progressList = (user && user.vocabProgress) ? user.vocabProgress : [];
      masteredWords = progressList.filter((p) => (p.proficiency || 0) >= 80).length;
      learningWords = progressList.filter(
        (p) => (p.proficiency || 0) > 0 && (p.proficiency || 0) < 80
      ).length;
    }

    const newWords = Math.max(0, totalWords - masteredWords - learningWords);
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

// 8. Hàm tạo dữ liệu tự động (Seeding) - PHIÊN BẢN CẬP NHẬT CHỦ ĐỀ
exports.seedDefaultVocabulary = async (req, res) => {
  try {
    const { roadmapId } = req.body;
    if (!roadmapId) {
      return res.status(400).json({ message: "Vui lòng cung cấp roadmapId!" });
    }

    // --- 1. Tách các từ ra từng mảng riêng và tự động gán category ---

    // CHỦ ĐỀ 1: HỢP ĐỒNG & KINH DOANH (100 từ)
    const topic1 = [
      { word: "contract", meaning: "Hợp đồng", phonetic: "/ˈkɑːn.trækt/" },
      { word: "agreement", meaning: "Thỏa thuận", phonetic: "/əˈɡriː.mənt/" },
      { word: "signature", meaning: "Chữ ký", phonetic: "/ˈsɪɡ.nə.tʃɚ/" },
      { word: "negotiate", meaning: "Đàm phán", phonetic: "/nəˈɡoʊ.ʃi.eɪt/" },
      { word: "condition", meaning: "Điều kiện", phonetic: "/kənˈdɪʃ.ən/" },
      {
        word: "guarantee",
        meaning: "Bảo hành, đảm bảo",
        phonetic: "/ˌɡer.ənˈtiː/",
      },
      { word: "obligate", meaning: "Bắt buộc", phonetic: "/ˈɑː.blə.ɡeɪt/" },
      { word: "provision", meaning: "Điều khoản", phonetic: "/prəˈvɪʒ.ən/" },
      { word: "resolve", meaning: "Giải quyết", phonetic: "/rɪˈzɑːlv/" },
      {
        word: "specific",
        meaning: "Cụ thể, đặc thù",
        phonetic: "/spəˈsɪf.ɪk/",
      },
      { word: "cancel", meaning: "Hủy bỏ", phonetic: "/ˈkæn.səl/" },
      { word: "establish", meaning: "Thành lập", phonetic: "/ɪˈstæb.lɪʃ/" },
      {
        word: "party",
        meaning: "Bên (trong hợp đồng)",
        phonetic: "/ˈpɑːr.t̬i/",
      },
      { word: "binding", meaning: "Ràng buộc", phonetic: "/ˈbaɪn.dɪŋ/" },
      { word: "breach", meaning: "Vi phạm", phonetic: "/briːtʃ/" },
      { word: "clause", meaning: "Điều khoản", phonetic: "/klɑːz/" },
      { word: "commit", meaning: "Cam kết", phonetic: "/kəˈmɪt/" },
      { word: "comply", meaning: "Tuân thủ", phonetic: "/kəmˈplaɪ/" },
      { word: "draft", meaning: "Bản nháp", phonetic: "/dræft/" },
      { word: "valid", meaning: "Có hiệu lực", phonetic: "/ˈvæl.ɪd/" },
      { word: "accept", meaning: "Chấp nhận", phonetic: "/əkˈsept/" },
      {
        word: "acquire",
        meaning: "Thu mua, giành được",
        phonetic: "/əˈkwaɪɚ/",
      },
      { word: "advantage", meaning: "Lợi thế", phonetic: "/ədˈvæn.t̬ɪdʒ/" },
      {
        word: "affiliate",
        meaning: "Công ty liên kết",
        phonetic: "/əˈfɪl.i.eɪt/",
      },
      { word: "append", meaning: "Đính kèm", phonetic: "/əˈpend/" },
      { word: "arbitrate", meaning: "Phân xử", phonetic: "/ˈɑːr.bə.treɪt/" },
      { word: "asset", meaning: "Tài sản", phonetic: "/ˈæs.et/" },
      { word: "authorize", meaning: "Ủy quyền", phonetic: "/ˈɑː.θɚ.aɪz/" },
      { word: "bankrupt", meaning: "Phá sản", phonetic: "/ˈbæŋ.krʌpt/" },
      { word: "bargain", meaning: "Mặc cả, món hời", phonetic: "/ˈbɑːr.ɡɪn/" },
      { word: "bid", meaning: "Đấu thầu", phonetic: "/bɪd/" },
      { word: "board", meaning: "Hội đồng quản trị", phonetic: "/bɔːrd/" },
      { word: "brand", meaning: "Thương hiệu", phonetic: "/brænd/" },
      { word: "buyout", meaning: "Mua lại công ty", phonetic: "/ˈbaɪ.aʊt/" },
      { word: "capital", meaning: "Vốn", phonetic: "/ˈkæp.ə.t̬əl/" },
      { word: "commerce", meaning: "Thương mại", phonetic: "/ˈkɑː.mɝːs/" },
      { word: "compete", meaning: "Cạnh tranh", phonetic: "/kəmˈpiːt/" },
      {
        word: "compromise",
        meaning: "Thỏa hiệp",
        phonetic: "/ˈkɑːm.prə.maɪz/",
      },
      { word: "consent", meaning: "Sự đồng ý", phonetic: "/kənˈsent/" },
      {
        word: "consolidate",
        meaning: "Hợp nhất",
        phonetic: "/kənˈsɑː.lə.deɪt/",
      },
      { word: "consult", meaning: "Tư vấn", phonetic: "/kənˈsʌlt/" },
      { word: "contractor", meaning: "Nhà thầu", phonetic: "/ˈkɑːn.træk.tɚ/" },
      {
        word: "corporate",
        meaning: "Thuộc về doanh nghiệp",
        phonetic: "/ˈkɔːr.pɚ.ət/",
      },
      { word: "deal", meaning: "Giao dịch", phonetic: "/diːl/" },
      { word: "declare", meaning: "Tuyên bố", phonetic: "/dɪˈkler/" },
      { word: "default", meaning: "Vỡ nợ, mặc định", phonetic: "/dɪˈfɑːlt/" },
      { word: "defect", meaning: "Khuyết điểm", phonetic: "/ˈdiː.fekt/" },
      { word: "defend", meaning: "Bảo vệ, bào chữa", phonetic: "/dɪˈfend/" },
      { word: "demand", meaning: "Nhu cầu", phonetic: "/dɪˈmænd/" },
      { word: "dispute", meaning: "Tranh chấp", phonetic: "/dɪˈspjuːt/" },
      { word: "dissolve", meaning: "Giải thể", phonetic: "/dɪˈzɑːlv/" },
      {
        word: "endorse",
        meaning: "Xác nhận, tán thành",
        phonetic: "/ɪnˈdɔːrs/",
      },
      {
        word: "enterprise",
        meaning: "Doanh nghiệp",
        phonetic: "/ˈen.t̬ɚ.praɪz/",
      },
      { word: "equity", meaning: "Vốn chủ sở hữu", phonetic: "/ˈek.wə.t̬i/" },
      { word: "expansion", meaning: "Sự mở rộng", phonetic: "/ɪkˈspæn.ʃən/" },
      { word: "firm", meaning: "Công ty", phonetic: "/fɝːm/" },
      { word: "forbid", meaning: "Cấm", phonetic: "/fɚˈbɪd/" },
      { word: "franchise", meaning: "Nhượng quyền", phonetic: "/ˈfræn.tʃaɪz/" },
      { word: "fraud", meaning: "Gian lận", phonetic: "/frɑːd/" },
      {
        word: "fulfill",
        meaning: "Hoàn thành, đáp ứng",
        phonetic: "/fʊlˈfɪl/",
      },
      { word: "govern", meaning: "Quản lý, chi phối", phonetic: "/ˈɡʌv.ɚn/" },
      { word: "hire", meaning: "Thuê mướn", phonetic: "/haɪr/" },
      {
        word: "incorporate",
        meaning: "Sáp nhập, kết hợp",
        phonetic: "/ɪnˈkɔːr.pɚ.eɪt/",
      },
      {
        word: "infringe",
        meaning: "Vi phạm, xâm phạm",
        phonetic: "/ɪnˈfrɪndʒ/",
      },
      { word: "joint", meaning: "Chung, liên doanh", phonetic: "/dʒɔɪnt/" },
      { word: "lease", meaning: "Cho thuê (dài hạn)", phonetic: "/liːs/" },
      { word: "legal", meaning: "Hợp pháp", phonetic: "/ˈliː.ɡəl/" },
      {
        word: "liability",
        meaning: "Trách nhiệm pháp lý",
        phonetic: "/ˌlaɪ.əˈbɪl.ə.t̬i/",
      },
      { word: "license", meaning: "Giấy phép", phonetic: "/ˈlaɪ.səns/" },
      { word: "liquidate", meaning: "Thanh lý", phonetic: "/ˈlɪk.wə.deɪt/" },
      { word: "market", meaning: "Thị trường", phonetic: "/ˈmɑːr.kɪt/" },
      { word: "merge", meaning: "Sáp nhập", phonetic: "/mɝːdʒ/" },
      { word: "monopoly", meaning: "Độc quyền", phonetic: "/məˈnɑː.pəl.i/" },
      {
        word: "mutual",
        meaning: "Lẫn nhau, chung",
        phonetic: "/ˈmjuː.tʃu.əl/",
      },
      {
        word: "nullify",
        meaning: "Hủy bỏ, vô hiệu hóa",
        phonetic: "/ˈnʌl.ə.faɪ/",
      },
      { word: "offer", meaning: "Đề nghị", phonetic: "/ˈɑː.fɚ/" },
      { word: "partner", meaning: "Đối tác", phonetic: "/ˈpɑːr.tnɚ/" },
      { word: "patent", meaning: "Bằng sáng chế", phonetic: "/ˈpæt.ənt/" },
      {
        word: "penalty",
        meaning: "Hình phạt, tiền phạt",
        phonetic: "/ˈpen.əl.t̬i/",
      },
      { word: "profit", meaning: "Lợi nhuận", phonetic: "/ˈprɑː.fɪt/" },
      { word: "propose", meaning: "Đề xuất", phonetic: "/prəˈpoʊz/" },
      { word: "quota", meaning: "Hạn ngạch", phonetic: "/ˈkwoʊ.t̬ə/" },
      { word: "ratify", meaning: "Phê chuẩn", phonetic: "/ˈræt̬.ə.faɪ/" },
      { word: "renew", meaning: "Gia hạn", phonetic: "/rɪˈnuː/" },
      { word: "represent", meaning: "Đại diện", phonetic: "/ˌrep.rɪˈzent/" },
      { word: "resign", meaning: "Từ chức", phonetic: "/rɪˈzaɪn/" },
      { word: "restrict", meaning: "Hạn chế", phonetic: "/rɪˈstrɪkt/" },
      { word: "retail", meaning: "Bán lẻ", phonetic: "/ˈriː.teɪl/" },
      { word: "risk", meaning: "Rủi ro", phonetic: "/rɪsk/" },
      { word: "sanction", meaning: "Lệnh trừng phạt", phonetic: "/ˈsæŋk.ʃən/" },
      { word: "settle", meaning: "Giải quyết, dàn xếp", phonetic: "/ˈset̬.əl/" },
      { word: "share", meaning: "Cổ phần", phonetic: "/ʃer/" },
      { word: "sponsor", meaning: "Nhà tài trợ", phonetic: "/ˈspɑːn.sɚ/" },
      { word: "stake", meaning: "Cổ phần, lợi ích", phonetic: "/steɪk/" },
      {
        word: "stipulate",
        meaning: "Quy định (trong HĐ)",
        phonetic: "/ˈstɪp.jə.leɪt/",
      },
      { word: "sue", meaning: "Kiện", phonetic: "/suː/" },
      { word: "term", meaning: "Điều khoản, kỳ hạn", phonetic: "/tɝːm/" },
      { word: "trademark", meaning: "Nhãn hiệu", phonetic: "/ˈtreɪd.mɑːrk/" },
      { word: "violate", meaning: "Vi phạm", phonetic: "/ˈvaɪ.ə.leɪt/" },
      { word: "venture", meaning: "Dự án kinh doanh", phonetic: "/ˈven.tʃɚ/" },
    ].map((item) => ({ ...item, category: "Hợp đồng & Kinh doanh" }));

    // CHỦ ĐỀ 2: MÔI TRƯỜNG VĂN PHÒNG (100 từ)
    const topic2 = [
      { word: "colleague", meaning: "Đồng nghiệp", phonetic: "/ˈkɑː.liːɡ/" },
      { word: "manager", meaning: "Người quản lý", phonetic: "/ˈmæn.ə.dʒɚ/" },
      { word: "meeting", meaning: "Cuộc họp", phonetic: "/ˈmiː.t̬ɪŋ/" },
      {
        word: "agenda",
        meaning: "Chương trình nghị sự",
        phonetic: "/əˈdʒen.də/",
      },
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
      {
        word: "supervisor",
        meaning: "Người giám sát",
        phonetic: "/ˈsuː.pɚ.vaɪ.zɚ/",
      },
      {
        word: "presentation",
        meaning: "Bài thuyết trình",
        phonetic: "/ˌprez.ənˈteɪ.ʃən/",
      },
      { word: "absent", meaning: "Vắng mặt", phonetic: "/ˈæb.sənt/" },
      { word: "accomplish", meaning: "Hoàn thành", phonetic: "/əˈkɑːm.plɪʃ/" },
      {
        word: "administration",
        meaning: "Sự quản trị",
        phonetic: "/ədˌmɪn.əˈstreɪ.ʃən/",
      },
      {
        word: "advance",
        meaning: "Tiến bộ, thăng tiến",
        phonetic: "/ədˈvæns/",
      },
      {
        word: "applicant",
        meaning: "Người ứng tuyển",
        phonetic: "/ˈæp.lə.kənt/",
      },
      { word: "appoint", meaning: "Bổ nhiệm", phonetic: "/əˈpɔɪnt/" },
      {
        word: "appraise",
        meaning: "Đánh giá (hiệu suất)",
        phonetic: "/əˈpreɪz/",
      },
      { word: "archive", meaning: "Lưu trữ", phonetic: "/ˈɑːr.kaɪv/" },
      { word: "attach", meaning: "Đính kèm", phonetic: "/əˈtætʃ/" },
      {
        word: "background",
        meaning: "Kinh nghiệm, lý lịch",
        phonetic: "/ˈbæk.ɡraʊnd/",
      },
      { word: "badge", meaning: "Thẻ nhân viên", phonetic: "/bædʒ/" },
      { word: "binder", meaning: "Bìa kẹp tài liệu", phonetic: "/ˈbaɪn.dɚ/" },
      { word: "brief", meaning: "Bản tóm tắt", phonetic: "/briːf/" },
      {
        word: "cabinet",
        meaning: "Tủ đựng tài liệu",
        phonetic: "/ˈkæb.ən.ət/",
      },
      { word: "cafeteria", meaning: "Căn tin", phonetic: "/ˌkæf.əˈtɪr.i.ə/" },
      { word: "candidate", meaning: "Ứng viên", phonetic: "/ˈkæn.dɪ.dət/" },
      { word: "career", meaning: "Sự nghiệp", phonetic: "/kəˈrɪr/" },
      { word: "certificate", meaning: "Chứng chỉ", phonetic: "/sɚˈtɪf.ə.kət/" },
      { word: "chair", meaning: "Chủ tọa, ghế", phonetic: "/tʃer/" },
      {
        word: "commute",
        meaning: "Đi làm (quãng đường)",
        phonetic: "/kəˈmjuːt/",
      },
      {
        word: "compensation",
        meaning: "Lương thưởng, đền bù",
        phonetic: "/ˌkɑːm.penˈseɪ.ʃən/",
      },
      { word: "conference", meaning: "Hội nghị", phonetic: "/ˈkɑːn.fɚ.əns/" },
      { word: "conflict", meaning: "Xung đột", phonetic: "/ˈkɑːn.flɪkt/" },
      {
        word: "coordinate",
        meaning: "Điều phối",
        phonetic: "/koʊˈɔːr.dən.eɪt/",
      },
      { word: "copy", meaning: "Bản sao", phonetic: "/ˈkɑː.pi/" },
      {
        word: "cubicle",
        meaning: "Không gian làm việc (ô nhỏ)",
        phonetic: "/ˈkjuː.bɪ.kəl/",
      },
      { word: "delegate", meaning: "Giao phó", phonetic: "/ˈdel.ə.ɡət/" },
      { word: "desk", meaning: "Bàn làm việc", phonetic: "/desk/" },
      { word: "dismiss", meaning: "Sa thải", phonetic: "/dɪˈsmɪs/" },
      { word: "drawer", meaning: "Ngăn kéo", phonetic: "/drɔːr/" },
      { word: "employ", meaning: "Thuê làm", phonetic: "/ɪmˈplɔɪ/" },
      { word: "enroll", meaning: "Đăng ký", phonetic: "/ɪnˈroʊl/" },
      {
        word: "equipment",
        meaning: "Trang thiết bị",
        phonetic: "/ɪˈkwɪp.mənt/",
      },
      {
        word: "executive",
        meaning: "Giám đốc điều hành",
        phonetic: "/ɪɡˈzek.jə.t̬ɪv/",
      },
      {
        word: "facility",
        meaning: "Cơ sở vật chất",
        phonetic: "/fəˈsɪl.ə.t̬i/",
      },
      { word: "file", meaning: "Hồ sơ, tệp", phonetic: "/faɪl/" },
      { word: "fire", meaning: "Sa thải", phonetic: "/faɪr/" },
      { word: "folder", meaning: "Thư mục", phonetic: "/ˈfoʊl.dɚ/" },
      { word: "goal", meaning: "Mục tiêu", phonetic: "/ɡoʊl/" },
      { word: "human", meaning: "Nhân sự", phonetic: "/ˈhjuː.mən/" },
      { word: "implement", meaning: "Thực hiện", phonetic: "/ˈɪm.plə.ment/" },
      { word: "instruct", meaning: "Hướng dẫn", phonetic: "/ɪnˈstrʌkt/" },
      { word: "intern", meaning: "Thực tập sinh", phonetic: "/ˈɪn.tɝːn/" },
      { word: "interview", meaning: "Phỏng vấn", phonetic: "/ˈɪn.t̬ɚ.vjuː/" },
      { word: "layout", meaning: "Bố cục", phonetic: "/ˈleɪ.aʊt/" },
      { word: "leave", meaning: "Nghỉ phép", phonetic: "/liːv/" },
      { word: "memo", meaning: "Bản ghi nhớ", phonetic: "/ˈmem.oʊ/" },
      { word: "motivate", meaning: "Động viên", phonetic: "/ˈmoʊ.t̬ə.veɪt/" },
      { word: "notice", meaning: "Thông báo", phonetic: "/ˈnoʊ.t̬ɪs/" },
      { word: "objective", meaning: "Mục tiêu", phonetic: "/əbˈdʒek.tɪv/" },
      {
        word: "orientation",
        meaning: "Sự định hướng",
        phonetic: "/ˌɔːr.i.enˈteɪ.ʃən/",
      },
      { word: "paper", meaning: "Giấy tờ", phonetic: "/ˈpeɪ.pɚ/" },
      {
        word: "participate",
        meaning: "Tham gia",
        phonetic: "/pɑːrˈtɪs.ə.peɪt/",
      },
      { word: "pension", meaning: "Lương hưu", phonetic: "/ˈpen.ʃən/" },
      { word: "perform", meaning: "Thực hiện", phonetic: "/pɚˈfɔːrm/" },
      { word: "personnel", meaning: "Nhân sự", phonetic: "/ˌpɝː.sənˈnel/" },
      { word: "plan", meaning: "Kế hoạch", phonetic: "/plæn/" },
      { word: "printer", meaning: "Máy in", phonetic: "/ˈprɪn.t̬ɚ/" },
      {
        word: "probation",
        meaning: "Thời gian thử việc",
        phonetic: "/proʊˈbeɪ.ʃən/",
      },
      {
        word: "productivity",
        meaning: "Năng suất",
        phonetic: "/ˌproʊ.dʌkˈtɪv.ə.t̬i/",
      },
      { word: "project", meaning: "Dự án", phonetic: "/ˈprɑː.dʒekt/" },
      { word: "qualify", meaning: "Đủ điều kiện", phonetic: "/ˈkwɑː.lə.faɪ/" },
      { word: "recruit", meaning: "Tuyển dụng", phonetic: "/rɪˈkruːt/" },
      { word: "report", meaning: "Báo cáo", phonetic: "/rɪˈpɔːrt/" },
      { word: "retire", meaning: "Nghỉ hưu", phonetic: "/rɪˈtaɪr/" },
      { word: "salary", meaning: "Tiền lương", phonetic: "/ˈsæl.ɚ.i/" },
      { word: "shift", meaning: "Ca làm việc", phonetic: "/ʃɪft/" },
      { word: "skill", meaning: "Kỹ năng", phonetic: "/skɪl/" },
      { word: "staple", meaning: "Ghim giấy", phonetic: "/ˈsteɪ.pəl/" },
      {
        word: "stationery",
        meaning: "Văn phòng phẩm",
        phonetic: "/ˈsteɪ.ʃə.ner.i/",
      },
      { word: "strike", meaning: "Đình công", phonetic: "/straɪk/" },
      { word: "task", meaning: "Nhiệm vụ", phonetic: "/tæsk/" },
      { word: "team", meaning: "Đội ngũ", phonetic: "/tiːm/" },
      { word: "timecard", meaning: "Thẻ chấm công", phonetic: "/ˈtaɪm.kɑːrd/" },
      { word: "train", meaning: "Đào tạo", phonetic: "/treɪn/" },
    ].map((item) => ({ ...item, category: "Môi trường văn phòng" }));

    // CHỦ ĐỀ 3: HỆ THỐNG THÔNG TIN & CÔNG NGHỆ (100 từ)
    const topic3 = [
      {
        word: "developer",
        meaning: "Lập trình viên",
        phonetic: "/dɪˈvel.ə.pɚ/",
      },
      {
        word: "database",
        meaning: "Cơ sở dữ liệu",
        phonetic: "/ˈdeɪ.t̬ə.beɪs/",
      },
      { word: "software", meaning: "Phần mềm", phonetic: "/ˈsɑːft.wer/" },
      { word: "hardware", meaning: "Phần cứng", phonetic: "/ˈhɑːrd.wer/" },
      { word: "network", meaning: "Mạng lưới", phonetic: "/ˈnet.wɝːk/" },
      { word: "server", meaning: "Máy chủ", phonetic: "/ˈsɝː.vɚ/" },
      {
        word: "innovation",
        meaning: "Sự đổi mới",
        phonetic: "/ˌɪn.əˈveɪ.ʃən/",
      },
      { word: "interface", meaning: "Giao diện", phonetic: "/ˈɪn.t̬ɚ.feɪs/" },
      { word: "access", meaning: "Truy cập", phonetic: "/ˈæk.ses/" },
      { word: "configure", meaning: "Cấu hình", phonetic: "/kənˈfɪɡ.jɚ/" },
      { word: "install", meaning: "Cài đặt", phonetic: "/ɪnˈstɑːl/" },
      { word: "update", meaning: "Cập nhật", phonetic: "/ʌpˈdeɪt/" },
      { word: "upgrade", meaning: "Nâng cấp", phonetic: "/ʌpˈɡreɪd/" },
      {
        word: "monitor",
        meaning: "Giám sát, màn hình",
        phonetic: "/ˈmɑː.nə.t̬ɚ/",
      },
      { word: "security", meaning: "Bảo mật", phonetic: "/səˈkjʊr.ə.t̬i/" },
      { word: "system", meaning: "Hệ thống", phonetic: "/ˈsɪs.təm/" },
      {
        word: "troubleshoot",
        meaning: "Khắc phục sự cố",
        phonetic: "/ˈtrʌb.əl.ʃuːt/",
      },
      { word: "variable", meaning: "Biến số", phonetic: "/ˈver.i.ə.bəl/" },
      { word: "logic", meaning: "Tính logic", phonetic: "/ˈlɑː.dʒɪk/" },
      { word: "algorithm", meaning: "Thuật toán", phonetic: "/ˈæl.ɡə.rɪ.ðəm/" },
      { word: "account", meaning: "Tài khoản", phonetic: "/əˈkaʊnt/" },
      { word: "admin", meaning: "Quản trị viên", phonetic: "/ˈæd.mɪn/" },
      {
        word: "application",
        meaning: "Ứng dụng",
        phonetic: "/ˌæp.ləˈkeɪ.ʃən/",
      },
      { word: "array", meaning: "Mảng (lập trình)", phonetic: "/əˈreɪ/" },
      { word: "backup", meaning: "Sao lưu", phonetic: "/ˈbæk.ʌp/" },
      { word: "bandwidth", meaning: "Băng thông", phonetic: "/ˈbænd.wɪdθ/" },
      { word: "binary", meaning: "Hệ nhị phân", phonetic: "/ˈbaɪ.nɚ.i/" },
      { word: "boot", meaning: "Khởi động máy", phonetic: "/buːt/" },
      {
        word: "broadband",
        meaning: "Băng thông rộng",
        phonetic: "/ˈbrɑːd.bænd/",
      },
      { word: "browser", meaning: "Trình duyệt", phonetic: "/ˈbraʊ.zɚ/" },
      { word: "bug", meaning: "Lỗi phần mềm", phonetic: "/bʌɡ/" },
      { word: "byte", meaning: "Byte (đơn vị dữ liệu)", phonetic: "/baɪt/" },
      { word: "cache", meaning: "Bộ nhớ đệm", phonetic: "/kæʃ/" },
      { word: "circuit", meaning: "Mạch điện", phonetic: "/ˈsɝː.kɪt/" },
      { word: "client", meaning: "Máy khách", phonetic: "/ˈklaɪ.ənt/" },
      { word: "cloud", meaning: "Đám mây (lưu trữ)", phonetic: "/klaʊd/" },
      { word: "code", meaning: "Mã lệnh", phonetic: "/koʊd/" },
      { word: "command", meaning: "Câu lệnh", phonetic: "/kəˈmænd/" },
      { word: "compile", meaning: "Biên dịch", phonetic: "/kəmˈpaɪl/" },
      { word: "component", meaning: "Thành phần", phonetic: "/kəmˈpoʊ.nənt/" },
      { word: "connect", meaning: "Kết nối", phonetic: "/kəˈnekt/" },
      { word: "console", meaning: "Bảng điều khiển", phonetic: "/kənˈsoʊl/" },
      { word: "crash", meaning: "Sập (hệ thống)", phonetic: "/kræʃ/" },
      { word: "cursor", meaning: "Con trỏ chuột", phonetic: "/ˈkɝː.sɚ/" },
      { word: "cyber", meaning: "Không gian mạng", phonetic: "/ˈsaɪ.bɚ/" },
      {
        word: "dashboard",
        meaning: "Bảng điều khiển",
        phonetic: "/ˈdæʃ.bɔːrd/",
      },
      { word: "data", meaning: "Dữ liệu", phonetic: "/ˈdeɪ.t̬ə/" },
      { word: "debug", meaning: "Gỡ lỗi", phonetic: "/ˌdiːˈbʌɡ/" },
      { word: "delete", meaning: "Xóa", phonetic: "/dɪˈliːt/" },
      { word: "deploy", meaning: "Triển khai", phonetic: "/dɪˈplɔɪ/" },
      { word: "desktop", meaning: "Máy tính để bàn", phonetic: "/ˈdesk.tɑːp/" },
      { word: "device", meaning: "Thiết bị", phonetic: "/dɪˈvaɪs/" },
      { word: "digital", meaning: "Kỹ thuật số", phonetic: "/ˈdɪdʒ.ə.t̬əl/" },
      { word: "directory", meaning: "Thư mục", phonetic: "/dɪˈrek.tɚ.i/" },
      { word: "domain", meaning: "Tên miền", phonetic: "/doʊˈmeɪn/" },
      { word: "download", meaning: "Tải xuống", phonetic: "/ˈdaʊn.loʊd/" },
      { word: "encryption", meaning: "Mã hóa", phonetic: "/ɪnˈkrɪp.ʃən/" },
      { word: "error", meaning: "Lỗi", phonetic: "/ˈer.ɚ/" },
      { word: "execute", meaning: "Thực thi", phonetic: "/ˈek.sə.kjuːt/" },
      { word: "firewall", meaning: "Tường lửa", phonetic: "/ˈfaɪr.wɑːl/" },
      { word: "format", meaning: "Định dạng", phonetic: "/ˈfɔːr.mæt/" },
      {
        word: "framework",
        meaning: "Khung làm việc",
        phonetic: "/ˈfreɪm.wɝːk/",
      },
      { word: "gateway", meaning: "Cổng giao tiếp", phonetic: "/ˈɡeɪt.weɪ/" },
      { word: "hacker", meaning: "Tin tặc", phonetic: "/ˈhæk.ɚ/" },
      { word: "host", meaning: "Máy chủ", phonetic: "/hoʊst/" },
      { word: "input", meaning: "Đầu vào", phonetic: "/ˈɪn.pʊt/" },
      { word: "integrate", meaning: "Tích hợp", phonetic: "/ˈɪn.t̬ə.ɡreɪt/" },
      { word: "internet", meaning: "Mạng internet", phonetic: "/ˈɪn.t̬ɚ.net/" },
      { word: "ip", meaning: "Giao thức mạng", phonetic: "/ˌaɪˈpiː/" },
      { word: "keyboard", meaning: "Bàn phím", phonetic: "/ˈkiː.bɔːrd/" },
      { word: "link", meaning: "Liên kết", phonetic: "/lɪŋk/" },
      { word: "login", meaning: "Đăng nhập", phonetic: "/ˈlɑːɡ.ɪn/" },
      { word: "malware", meaning: "Phần mềm độc hại", phonetic: "/ˈmæl.wer/" },
      { word: "memory", meaning: "Bộ nhớ", phonetic: "/ˈmem.ɚ.i/" },
      { word: "module", meaning: "Mô-đun", phonetic: "/ˈmɑː.dʒuːl/" },
      { word: "mouse", meaning: "Chuột máy tính", phonetic: "/maʊs/" },
      { word: "offline", meaning: "Ngoại tuyến", phonetic: "/ˌɑːfˈlaɪn/" },
      { word: "online", meaning: "Trực tuyến", phonetic: "/ˌɑːnˈlaɪn/" },
      { word: "output", meaning: "Đầu ra", phonetic: "/ˈaʊt.pʊt/" },
      { word: "password", meaning: "Mật khẩu", phonetic: "/ˈpæs.wɝːd/" },
      { word: "patch", meaning: "Bản vá lỗi", phonetic: "/pætʃ/" },
      { word: "platform", meaning: "Nền tảng", phonetic: "/ˈplæt.fɔːrm/" },
      { word: "portal", meaning: "Cổng thông tin", phonetic: "/ˈpɔːr.t̬əl/" },
      { word: "program", meaning: "Chương trình", phonetic: "/ˈproʊ.ɡræm/" },
      { word: "protocol", meaning: "Giao thức", phonetic: "/ˈproʊ.t̬ə.kɑːl/" },
      { word: "query", meaning: "Truy vấn", phonetic: "/ˈkwɪr.i/" },
      { word: "reboot", meaning: "Khởi động lại", phonetic: "/ˌriːˈbuːt/" },
      { word: "router", meaning: "Bộ định tuyến", phonetic: "/ˈraʊ.t̬ɚ/" },
      { word: "script", meaning: "Kịch bản", phonetic: "/skrɪpt/" },
      { word: "search", meaning: "Tìm kiếm", phonetic: "/sɝːtʃ/" },
      { word: "source", meaning: "Nguồn", phonetic: "/sɔːrs/" },
      { word: "spam", meaning: "Thư rác", phonetic: "/spæm/" },
      { word: "storage", meaning: "Lưu trữ", phonetic: "/ˈstɔːr.ɪdʒ/" },
      { word: "terminal", meaning: "Dòng lệnh", phonetic: "/ˈtɝː.mə.nəl/" },
      { word: "upload", meaning: "Tải lên", phonetic: "/ˈʌp.loʊd/" },
      { word: "virus", meaning: "Vi-rút", phonetic: "/ˈvaɪ.rəs/" },
      { word: "web", meaning: "Trang web", phonetic: "/web/" },
      { word: "wireless", meaning: "Không dây", phonetic: "/ˈwaɪr.ləs/" },
    ].map((item) => ({ ...item, category: "Hệ thống thông tin & Công nghệ" }));

    // CHỦ ĐỀ 4: KẾ TOÁN & TÀI CHÍNH (100 từ)
    const topic4 = [
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
      { word: "accrue", meaning: "Tích lũy", phonetic: "/əˈkruː/" },
      {
        word: "accumulate",
        meaning: "Tích lũy, dồn lại",
        phonetic: "/əˈkjuː.mjə.leɪt/",
      },
      { word: "allocate", meaning: "Phân bổ", phonetic: "/ˈæl.ə.keɪt/" },
      { word: "allowance", meaning: "Trợ cấp", phonetic: "/əˈlaʊ.əns/" },
      { word: "amortize", meaning: "Khấu hao", phonetic: "/ˈæm.ɚ.taɪz/" },
      { word: "analyze", meaning: "Phân tích", phonetic: "/ˈæn.əl.aɪz/" },
      { word: "atm", meaning: "Máy rút tiền", phonetic: "/ˌeɪ.tiːˈem/" },
      { word: "bank", meaning: "Ngân hàng", phonetic: "/bæŋk/" },
      { word: "bill", meaning: "Hóa đơn", phonetic: "/bɪl/" },
      { word: "bond", meaning: "Trái phiếu", phonetic: "/bɑːnd/" },
      { word: "borrow", meaning: "Vay, mượn", phonetic: "/ˈbɑːr.oʊ/" },
      { word: "broker", meaning: "Người môi giới", phonetic: "/ˈbroʊ.kɚ/" },
      { word: "calculate", meaning: "Tính toán", phonetic: "/ˈkæl.kjə.leɪt/" },
      { word: "cash", meaning: "Tiền mặt", phonetic: "/kæʃ/" },
      { word: "charge", meaning: "Tính phí", phonetic: "/tʃɑːrdʒ/" },
      { word: "check", meaning: "Séc (ngân hàng)", phonetic: "/tʃek/" },
      { word: "clear", meaning: "Thanh toán bù trừ", phonetic: "/klɪr/" },
      {
        word: "collateral",
        meaning: "Tài sản thế chấp",
        phonetic: "/kəˈlæt̬.ɚ.əl/",
      },
      { word: "commission", meaning: "Tiền hoa hồng", phonetic: "/kəˈmɪʃ.ən/" },
      { word: "commodity", meaning: "Hàng hóa", phonetic: "/kəˈmɑː.də.t̬i/" },
      { word: "compound", meaning: "Lãi kép", phonetic: "/ˈkɑːm.paʊnd/" },
      { word: "cost", meaning: "Chi phí", phonetic: "/kɑːst/" },
      { word: "credit", meaning: "Tín dụng", phonetic: "/ˈkred.ɪt/" },
      { word: "deduction", meaning: "Sự khấu trừ", phonetic: "/dɪˈdʌk.ʃən/" },
      { word: "deficit", meaning: "Thâm hụt", phonetic: "/ˈdef.ə.sɪt/" },
      { word: "deposit", meaning: "Tiền gửi", phonetic: "/dɪˈpɑː.zɪt/" },
      { word: "depreciate", meaning: "Khấu hao", phonetic: "/dɪˈpriː.ʃi.eɪt/" },
      { word: "dividend", meaning: "Cổ tức", phonetic: "/ˈdɪv.ə.dend/" },
      { word: "earn", meaning: "Kiếm tiền", phonetic: "/ɝːn/" },
      { word: "evade", meaning: "Trốn (thuế)", phonetic: "/ɪˈveɪd/" },
      {
        word: "exchange",
        meaning: "Trao đổi, ngoại đoái",
        phonetic: "/ɪksˈtʃeɪndʒ/",
      },
      { word: "fee", meaning: "Lệ phí", phonetic: "/fiː/" },
      { word: "finance", meaning: "Tài chính", phonetic: "/ˈfaɪ.næns/" },
      { word: "forecast", meaning: "Dự báo", phonetic: "/ˈfɔːr.kæst/" },
      { word: "gross", meaning: "Tổng (chưa trừ thuế)", phonetic: "/ɡroʊs/" },
      { word: "income", meaning: "Thu nhập", phonetic: "/ˈɪn.kʌm/" },
      { word: "inflation", meaning: "Lạm phát", phonetic: "/ɪnˈfleɪ.ʃən/" },
      { word: "inherit", meaning: "Thừa kế", phonetic: "/ɪnˈher.ɪt/" },
      {
        word: "insolvent",
        meaning: "Mất khả năng thanh toán",
        phonetic: "/ɪnˈsɑːl.vənt/",
      },
      { word: "insure", meaning: "Bảo hiểm", phonetic: "/ɪnˈʃʊr/" },
      {
        word: "inventory",
        meaning: "Hàng tồn kho",
        phonetic: "/ˈɪn.vən.tɔːr.i/",
      },
      { word: "ledger", meaning: "Sổ cái", phonetic: "/ˈledʒ.ɚ/" },
      { word: "lend", meaning: "Cho vay", phonetic: "/lend/" },
      { word: "liquid", meaning: "Thanh khoản", phonetic: "/ˈlɪk.wɪd/" },
      {
        word: "margin",
        meaning: "Tỷ suất lợi nhuận",
        phonetic: "/ˈmɑːr.dʒɪn/",
      },
      { word: "mortgage", meaning: "Thế chấp", phonetic: "/ˈmɔːr.ɡɪdʒ/" },
      { word: "net", meaning: "Ròng, thực nhận", phonetic: "/net/" },
      { word: "owe", meaning: "Nợ", phonetic: "/oʊ/" },
      { word: "pay", meaning: "Trả tiền", phonetic: "/peɪ/" },
      {
        word: "portfolio",
        meaning: "Danh mục đầu tư",
        phonetic: "/pɔːrtˈfoʊ.li.oʊ/",
      },
      { word: "premium", meaning: "Phí bảo hiểm", phonetic: "/ˈpriː.mi.əm/" },
      { word: "price", meaning: "Giá cả", phonetic: "/praɪs/" },
      { word: "principal", meaning: "Tiền gốc", phonetic: "/ˈprɪn.sə.pəl/" },
      { word: "quote", meaning: "Báo giá", phonetic: "/kwoʊt/" },
      { word: "rate", meaning: "Tỷ giá", phonetic: "/reɪt/" },
      { word: "ration", meaning: "Định mức", phonetic: "/ˈræʃ.ən/" },
      { word: "reconcile", meaning: "Đối chiếu", phonetic: "/ˈrek.ən.saɪl/" },
      { word: "remit", meaning: "Chuyển tiền", phonetic: "/rɪˈmɪt/" },
      { word: "reserve", meaning: "Dự trữ", phonetic: "/rɪˈzɝːv/" },
      { word: "return", meaning: "Tiền lời", phonetic: "/rɪˈtɝːn/" },
      { word: "save", meaning: "Tiết kiệm", phonetic: "/seɪv/" },
      { word: "statement", meaning: "Sao kê", phonetic: "/ˈsteɪt.mənt/" },
      { word: "stock", meaning: "Cổ phiếu", phonetic: "/stɑːk/" },
      { word: "subsidy", meaning: "Trợ cấp", phonetic: "/ˈsʌb.sə.di/" },
      { word: "surplus", meaning: "Thặng dư", phonetic: "/ˈsɝː.pləs/" },
      { word: "transfer", meaning: "Chuyển khoản", phonetic: "/trænsˈfɝː/" },
      { word: "value", meaning: "Giá trị", phonetic: "/ˈvæl.juː/" },
      { word: "wage", meaning: "Tiền công", phonetic: "/weɪdʒ/" },
    ].map((item) => ({ ...item, category: "Kế toán & Tài chính" }));

    // CHỦ ĐỀ 5: DỊCH VỤ & KHÁCH HÀNG (100 từ)
    const topic5 = [
      { word: "customer", meaning: "Khách hàng", phonetic: "/ˈkʌs.tə.mɚ/" },
      { word: "service", meaning: "Dịch vụ", phonetic: "/ˈsɝː.vɪs/" },
      { word: "feedback", meaning: "Phản hồi", phonetic: "/ˈfiːd.bæk/" },
      {
        word: "satisfaction",
        meaning: "Sự hài lòng",
        phonetic: "/ˌsæt̬.ɪsˈfæk.ʃən/",
      },
      { word: "complaint", meaning: "Lời phàn nàn", phonetic: "/kəmˈpleɪnt/" },
      { word: "warranty", meaning: "Bảo hành", phonetic: "/ˈwɔːr.ən.t̬i/" },
      { word: "delivery", meaning: "Giao hàng", phonetic: "/dɪˈlɪv.ɚ.i/" },
      { word: "order", meaning: "Đơn hàng", phonetic: "/ˈɔːr.dɚ/" },
      { word: "loyalty", meaning: "Sự trung thành", phonetic: "/ˈlɔɪ.əl.t̬i/" },
      {
        word: "inquiry",
        meaning: "Yêu cầu thông tin",
        phonetic: "/ˈɪn.kwə.ri/",
      },
      { word: "assist", meaning: "Hỗ trợ", phonetic: "/əˈsɪst/" },
      { word: "resolve", meaning: "Giải quyết", phonetic: "/rɪˈzɑːlv/" },
      { word: "policy", meaning: "Chính sách", phonetic: "/ˈpɑː.lə.si/" },
      { word: "priority", meaning: "Sự ưu tiên", phonetic: "/praɪˈɔːr.ə.t̬i/" },
      { word: "replace", meaning: "Thay thế", phonetic: "/rɪˈpleɪs/" },
      { word: "respond", meaning: "Phản hồi", phonetic: "/rɪˈspɑːnd/" },
      { word: "survey", meaning: "Khảo sát", phonetic: "/ˈsɝː.veɪ/" },
      { word: "upgrade", meaning: "Nâng cấp", phonetic: "/ʌpˈɡreɪd/" },
      { word: "apologize", meaning: "Xin lỗi", phonetic: "/əˈpɑː.lə.dʒaɪz/" },
      { word: "recommend", meaning: "Đề xuất", phonetic: "/ˌrek.əˈmend/" },
      { word: "address", meaning: "Giải quyết, địa chỉ", phonetic: "/əˈdres/" },
      { word: "advise", meaning: "Khuyên bảo", phonetic: "/ədˈvaɪz/" },
      { word: "afford", meaning: "Đủ khả năng", phonetic: "/əˈfɔːrd/" },
      { word: "agent", meaning: "Đại lý", phonetic: "/ˈeɪ.dʒənt/" },
      { word: "answer", meaning: "Trả lời", phonetic: "/ˈæn.sɚ/" },
      { word: "appeal", meaning: "Sự hấp dẫn", phonetic: "/əˈpiːl/" },
      { word: "approach", meaning: "Tiếp cận", phonetic: "/əˈproʊtʃ/" },
      { word: "assure", meaning: "Đảm bảo", phonetic: "/əˈʃʊr/" },
      { word: "attract", meaning: "Thu hút", phonetic: "/əˈtrækt/" },
      { word: "base", meaning: "Nền tảng", phonetic: "/beɪs/" },
      { word: "benefit", meaning: "Lợi ích", phonetic: "/ˈben.ə.fɪt/" },
      { word: "care", meaning: "Chăm sóc", phonetic: "/ker/" },
      { word: "cart", meaning: "Giỏ hàng", phonetic: "/kɑːrt/" },
      { word: "catalog", meaning: "Danh mục", phonetic: "/ˈkæt̬.əl.ɑːɡ/" },
      { word: "choice", meaning: "Lựa chọn", phonetic: "/tʃɔɪs/" },
      { word: "claim", meaning: "Khiếu nại", phonetic: "/kleɪm/" },
      { word: "comfort", meaning: "Sự thoải mái", phonetic: "/ˈkʌm.fɚt/" },
      { word: "compensate", meaning: "Đền bù", phonetic: "/ˈkɑːm.pen.seɪt/" },
      { word: "compliment", meaning: "Lời khen", phonetic: "/ˈkɑːm.plə.mənt/" },
      { word: "confirm", meaning: "Xác nhận", phonetic: "/kənˈfɝːm/" },
      { word: "consume", meaning: "Tiêu thụ", phonetic: "/kənˈsuːm/" },
      { word: "contact", meaning: "Liên hệ", phonetic: "/ˈkɑːn.tækt/" },
      { word: "convince", meaning: "Thuyết phục", phonetic: "/kənˈvɪns/" },
      { word: "coupon", meaning: "Phiếu giảm giá", phonetic: "/ˈkuː.pɑːn/" },
      { word: "courtesy", meaning: "Sự lịch sự", phonetic: "/ˈkɝː.t̬ə.si/" },
      { word: "delay", meaning: "Sự chậm trễ", phonetic: "/dɪˈleɪ/" },
      { word: "dispatch", meaning: "Gửi đi", phonetic: "/dɪˈspætʃ/" },
      { word: "display", meaning: "Trưng bày", phonetic: "/dɪˈspleɪ/" },
      { word: "expect", meaning: "Mong đợi", phonetic: "/ɪkˈspekt/" },
      { word: "explain", meaning: "Giải thích", phonetic: "/ɪkˈspleɪn/" },
      { word: "express", meaning: "Chuyển phát nhanh", phonetic: "/ɪkˈspres/" },
      { word: "extra", meaning: "Thêm, phụ", phonetic: "/ˈek.strə/" },
      { word: "fault", meaning: "Lỗi", phonetic: "/fɑːlt/" },
      { word: "feature", meaning: "Tính năng", phonetic: "/ˈfiː.tʃɚ/" },
      { word: "fix", meaning: "Sửa chữa", phonetic: "/fɪks/" },
      { word: "free", meaning: "Miễn phí", phonetic: "/friː/" },
      { word: "greet", meaning: "Chào hỏi", phonetic: "/ɡriːt/" },
      { word: "guide", meaning: "Hướng dẫn", phonetic: "/ɡaɪd/" },
      { word: "handle", meaning: "Xử lý", phonetic: "/ˈhæn.dəl/" },
      { word: "help", meaning: "Giúp đỡ", phonetic: "/help/" },
      { word: "hotline", meaning: "Đường dây nóng", phonetic: "/ˈhɑːt.laɪn/" },
      { word: "inform", meaning: "Thông báo", phonetic: "/ɪnˈfɔːrm/" },
      { word: "interact", meaning: "Tương tác", phonetic: "/ˌɪn.t̬ɚˈækt/" },
      { word: "issue", meaning: "Vấn đề", phonetic: "/ˈɪʃ.uː/" },
      { word: "membership", meaning: "Thành viên", phonetic: "/ˈmem.bɚ.ʃɪp/" },
      { word: "package", meaning: "Gói hàng", phonetic: "/ˈpæk.ɪdʒ/" },
      { word: "patron", meaning: "Khách quen", phonetic: "/ˈpeɪ.trən/" },
      { word: "polite", meaning: "Lịch sự", phonetic: "/pəˈlaɪt/" },
      { word: "preference", meaning: "Sự ưu tiên", phonetic: "/ˈpref.ɚ.əns/" },
      { word: "promise", meaning: "Lời hứa", phonetic: "/ˈprɑː.mɪs/" },
      { word: "provide", meaning: "Cung cấp", phonetic: "/prəˈvaɪd/" },
      { word: "purchase", meaning: "Mua hàng", phonetic: "/ˈpɝː.tʃəs/" },
      { word: "quality", meaning: "Chất lượng", phonetic: "/ˈkwɑː.lə.t̬i/" },
      { word: "recall", meaning: "Thu hồi", phonetic: "/rɪˈkɑːl/" },
      { word: "reception", meaning: "Lễ tân", phonetic: "/rɪˈsep.ʃən/" },
      { word: "register", meaning: "Đăng ký", phonetic: "/ˈredʒ.ə.stɚ/" },
      { word: "relation", meaning: "Mối quan hệ", phonetic: "/rɪˈleɪ.ʃən/" },
      { word: "repair", meaning: "Sửa chữa", phonetic: "/rɪˈper/" },
      { word: "request", meaning: "Yêu cầu", phonetic: "/rɪˈkwest/" },
      { word: "return", meaning: "Trả lại", phonetic: "/rɪˈtɝːn/" },
      { word: "review", meaning: "Đánh giá", phonetic: "/rɪˈvjuː/" },
      { word: "sample", meaning: "Mẫu thử", phonetic: "/ˈsæm.pəl/" },
      { word: "solution", meaning: "Giải pháp", phonetic: "/səˈluː.ʃən/" },
      { word: "support", meaning: "Hỗ trợ", phonetic: "/səˈpɔːrt/" },
    ].map((item) => ({ ...item, category: "Dịch vụ & Khách hàng" }));

    // --- 2. Gộp tất cả lại thành 1 mảng lớn ---
    const wordList = [...topic1, ...topic2, ...topic3, ...topic4, ...topic5];

    let updatedCount = 0;

    // --- 3. Chạy vòng lặp cập nhật lên MongoDB ---
    for (const item of wordList) {
      // Dùng findOneAndUpdate để CẬP NHẬT TỪ CŨ thay vì bỏ qua
      await Vocabulary.findOneAndUpdate(
        { word: { $regex: new RegExp(`^${item.word}$`, "i") } },
        {
          $set: {
            meaning: item.meaning,
            phonetic: item.phonetic,
            category: item.category, // Cập nhật đúng chủ đề
            roadmapId: roadmapId,
          },
          $setOnInsert: { proficiency: 0 }, // Nếu là từ mới tinh thì gán điểm = 0
        },
        { upsert: true, new: true },
      );
      updatedCount++;
    }

    res.status(200).json({
      message: "Đã phân loại Chủ đề thành công!",
      wordsProcessed: updatedCount,
    });
  } catch (error) {
    res.status(500).json({ message: "Lỗi server", error: error.message });
  }
};

// Ví dụ hàm lưu từ vựng (saveWord) trong vocabController.js của bạn:
exports.saveWord = async (req, res) => {
  try {
    const { wordId } = req.body;
    const userId = req.user.id; // Lấy ID của User đang đăng nhập từ authMiddleware

    // ... (Logic lưu từ vựng vào bảng User hoặc bảng Sổ tay của bạn) ...

    // === TỰ ĐỘNG CẬP NHẬT CHUỖI NGÀY HỌC (STREAK) ===
    // Mỗi khi người dùng bấm Lưu từ, hệ thống hiểu là họ có hoạt động học tập hôm nay
    const streakUser = await updateStreak(userId);
    const currentStreakDays = streakUser ? streakUser.streakDays : 0;

    res.status(200).json({
      message: "Đã lưu từ vựng vào sổ tay thành công!",
      streakDays: currentStreakDays, // Trả về đúng số nguyên streakDays
    });
  } catch (error) {
    res
      .status(500)
      .json({ message: "Lỗi server khi lưu từ", error: error.message });
  }
};

const { genAI } = require("../service/aiService");

// [HÀM 1] API Nhận text, gọi AI tạo Quiz và lưu vào Database
exports.generateCustomQuiz = async (req, res) => {
  try {
    const { content, questionCount = 10 } = req.body;
    if (!content)
      return res.status(400).json({ error: "Nội dung không được để trống!" });

    // Tạo tên Category duy nhất (VD: Quiz Tự Tạo - 1713500000)
    const customCategory = `Quiz Tự Tạo - ${new Date().getTime()}`;

    // Gọi Gemini AI
    const prompt = `Dựa vào đoạn văn bản sau: "${content}". Hãy tạo ra chính xác ${questionCount} từ vựng quan trọng nhất. 
        Trả về ĐÚNG định dạng mảng JSON thuần túy (không kèm markdown, không có \`\`\`json):
        [{"word": "từ tiếng anh", "phonetic": "phiên âm", "meaning": "nghĩa tiếng việt"}]`;

    const model = genAI.getGenerativeModel({ model: "gemini-2.5-flash" });
    const result = await model.generateContent(prompt);
    let textResult = result.response.text();

    // Cắt bỏ markdown nếu AI trả về lỗi định dạng
    textResult = textResult
      .replace(/```json/g, "")
      .replace(/```/g, "")
      .trim();
    const quizData = JSON.parse(textResult);

    // Lưu vào Collection Vocabulary có sẵn
    const Vocabulary = require("../models/Vocabulary");
    const savedVocabs = await Promise.all(
      quizData.map(async (item) => {
        const newVocab = new Vocabulary({
          word: item.word,
          phonetic: item.phonetic || "",
          meaning: item.meaning,
          category: customCategory, // Gắn category tùy chỉnh để dễ dàng lấy ra
          proficiency: 0,
        });
        return await newVocab.save();
      }),
    );

    res
      .status(200)
      .json({ message: "Tạo Quiz thành công", categoryName: customCategory });
  } catch (error) {
    console.error("Lỗi AI:", error);
    res.status(500).json({ error: "Lỗi hệ thống khi tạo quiz từ AI!" });
  }
};

// [HÀM 2] API Lấy danh sách tên các Quiz do người dùng tự tạo
exports.getCustomQuizCategories = async (req, res) => {
  try {
    const Vocabulary = require("../models/Vocabulary");
    // Tìm các category có chữ "Quiz Tự Tạo"
    const customCategories = await Vocabulary.distinct("category", {
      category: { $regex: "^Quiz Tự Tạo" },
    });
    res.status(200).json({ success: true, categories: customCategories });
  } catch (error) {
    res.status(500).json({ error: "Lỗi hệ thống!" });
  }
};
// =====================================================================================
// 🚀 TÍNH NĂNG 4: SỔ TAY TIÊN TRI (PREDICTIVE FLASHCARDS - ROCK SOLID VERSION)
// =====================================================================================

exports.getPredictiveFlashcards = async (req, res) => {
  try {
    const userId = req.user && req.user.userId ? req.user.userId : req.body.userId;
    if (!userId) {
      return res.status(401).json({ message: "Vui lòng đăng nhập!" });
    }

    const user = await User.findById(userId);
    if (!user || !user.vocabProgress || user.vocabProgress.length === 0) {
      return res.status(200).json({ message: "Trống", warning: "", predictiveFlashcards: [] });
    }

    // Lọc các từ chưa đạt thành thạo tuyệt đối (< 95) hoặc đã lâu chưa ôn (>= 2 ngày)
    const nowMs = Date.now();
    const candidates = user.vocabProgress.filter((p) => {
      const prof = p.proficiency || 0;
      const daysSince = p.lastReviewedDate
        ? (nowMs - new Date(p.lastReviewedDate).getTime()) / (1000 * 60 * 60 * 24)
        : 3.0;
      return prof < 80 || daysSince >= 2.0;
    });

    if (candidates.length === 0) {
      return res.status(200).json({ message: "Đã thuộc hết", warning: "", predictiveFlashcards: [] });
    }

    const userHistoryAcc = user.level ? Math.min(0.2 + user.level * 0.1, 0.9) : 0.5;
    const totalQuestions = (user.quizzesCount || 0) * 5;
    const userAvgTimeMs =
      user.timeLearnedMinutes > 0 && totalQuestions > 0
        ? Math.min(120000, Math.max(5000, Math.round((user.timeLearnedMinutes * 60 * 1000) / totalQuestions)))
        : 23000.0;

    // Giới hạn top 15 từ ưu tiên nhất để tối ưu độ trễ gọi XAI
    const topCandidates = candidates.slice(0, 15);

    const finalFlashcards = await Promise.all(
      topCandidates.map(async (item) => {
        let wordDetail = null;
        try {
          if (item.wordId && item.wordId.toString().length === 24) {
            wordDetail = await Vocabulary.findById(item.wordId);
          }
          if (!wordDetail) {
            wordDetail = await Vocabulary.findOne({ word: item.wordId || item.word });
          }
        } catch (e) {
          console.error("Bỏ qua lỗi tìm kiếm từ:", e.message);
        }

        const proficiency = item.proficiency || 0;
        const mistakeCount = item.mistakeCount || 0;
        const reviewCount = item.reviewCount || 1;
        const daysSinceReview = item.lastReviewedDate
          ? Math.max(0, (nowMs - new Date(item.lastReviewedDate).getTime()) / (1000 * 60 * 60 * 24))
          : 2.0;

        // Gọi FastAPI XGBoost + SHAP + Ebbinghaus
        const xaiRes = await xaiService.getForgettingPrediction({
          historyAcc: userHistoryAcc,
          totalQuestions,
          proficiency,
          mistakeCount,
          reviewCount,
          daysSinceReview,
          avgTime: userAvgTimeMs,
        });

        let predictedAccuracy;
        let isStruggling;
        let xaiReason;

        if (xaiRes && typeof xaiRes.predicted_accuracy === "number") {
          predictedAccuracy = Number((xaiRes.predicted_accuracy / 100).toFixed(2));
          isStruggling = Boolean(xaiRes.is_struggling);
          xaiReason = xaiRes.xai_reason || "Dự đoán bằng XGBoost + SHAP.";
        } else {
          // Fallback khoa học bằng đường cong quên Ebbinghaus R = exp(-t / S) khi Python offline
          const stabilityDays = Math.max(
            0.8,
            (1.5 + reviewCount * 1.2 + proficiency / 25.0) / (1.0 + mistakeCount * 0.5)
          );
          const retention = Math.max(0.05, Math.min(0.95, Math.exp(-daysSinceReview / stabilityDays)));
          predictedAccuracy = Number(retention.toFixed(2));
          isStruggling = retention < 0.65;
          const riskPct = Math.round((1 - retention) * 100);
          xaiReason = `Nguy cơ quên ${riskPct}% (Ebbinghaus: ${daysSinceReview.toFixed(1)} ngày chưa ôn, sai ${mistakeCount} lần).`;
        }

        return {
          wordId: item.wordId || item.word,
          wordText: wordDetail ? wordDetail.word : "Unknown",
          meaning: wordDetail ? wordDetail.meaning : "Cần ôn tập gấp",
          phonetic: wordDetail ? wordDetail.phonetic : "",
          predicted_accuracy: predictedAccuracy,
          is_struggling: isStruggling,
          xai_reason: xaiReason,
        };
      })
    );

    // Sắp xếp từ có nguy cơ quên cao nhất (predicted_accuracy thấp nhất) lên đầu
    finalFlashcards.sort((a, b) => a.predicted_accuracy - b.predicted_accuracy);

    res.status(200).json({
      message: "Thành công!",
      warning: `Sổ tay Tiên tri XAI phát hiện ${finalFlashcards.length} từ vựng có nguy cơ quên cao. Hãy ôn lại ngay!`,
      predictiveFlashcards: finalFlashcards,
    });
  } catch (error) {
    console.error("Lỗi Sổ tay Tiên tri:", error);
    res.status(500).json({ message: "Lỗi hệ thống!" });
  }
};
