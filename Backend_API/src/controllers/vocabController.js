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

    // 3. Xử lý dữ liệu trả về để lưu vào DB (Dùng ?. để tránh lỗi khi API trả về thiếu dữ liệu)
    const newVocab = new Vocabulary({
      word: data.word,
      phonetic: data.phonetic || "",
      meaning:
        data.meanings?.[0]?.definitions?.[0]?.definition ||
        "Chưa có định nghĩa",
      defEng: data.meanings?.[0]?.definitions?.[0]?.definition || "",
      // Em có thể map thêm các trường examples, synonyms ở đây sau
    });

    await newVocab.save();
    res.status(200).json(newVocab);
  } catch (error) {
    // Xử lý riêng lỗi 404 từ API từ điển
    if (error.response && error.response.status === 404) {
      return res
        .status(404)
        .json({ message: "Từ vựng không tồn tại trong từ điển" });
    }
    res
      .status(500)
      .json({
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

    // 3. Xử lý dữ liệu trả về để lưu vào DB (Dùng ?. để tránh lỗi khi API trả về thiếu dữ liệu)
    const newVocab = new Vocabulary({
      word: data.word,
      phonetic: data.phonetic || "",
      meaning:
        data.meanings?.[0]?.definitions?.[0]?.definition ||
        "Chưa có định nghĩa",
      defEng: data.meanings?.[0]?.definitions?.[0]?.definition || "",
      // Em có thể map thêm các trường examples, synonyms ở đây sau
    });

    await newVocab.save();
    res.status(200).json(newVocab);
  } catch (error) {
    // Xử lý riêng lỗi 404 từ API từ điển
    if (error.response && error.response.status === 404) {
      return res
        .status(404)
        .json({ message: "Từ vựng không tồn tại trong từ điển" });
    }
    res
      .status(500)
      .json({
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
// Thêm hàm này vào vocabController.js
exports.getVocabStats = async (req, res) => {
    try {
        // Đếm tổng số từ đang có trong Database
        const totalWords = await Vocabulary.countDocuments();
        
        // Giả sử em có trường 'proficiency' (độ thông thạo) từ 0 đến 100
        // Đếm số từ đã thuộc (ví dụ proficiency >= 80)
        const masteredWords = await Vocabulary.countDocuments({ proficiency: { $gte: 80 } });
        
        // Đếm số từ đang học (proficiency > 0 và < 80)
        const learningWords = await Vocabulary.countDocuments({ proficiency: { $gt: 0, $lt: 80 } });
        
        // Đếm từ mới (proficiency = 0)
        const newWords = await Vocabulary.countDocuments({ proficiency: 0 });

        // Tính phần trăm thành thạo
        const masteryPercentage = totalWords === 0 ? 0 : Math.round((masteredWords / totalWords) * 100);

        // Trả về dữ liệu cho Android
        res.status(200).json({
            totalWords,
            masteryPercentage,
            masteredWords,
            learningWords,
            newWords,
            newWordsThisWeek: 15 // Tạm thời hardcode hoặc em có thể query theo ngày tạo (createdAt)
        });
    } catch (error) {
        res.status(500).json({ message: "Lỗi khi lấy thống kê", error: error.message });
    }
};
