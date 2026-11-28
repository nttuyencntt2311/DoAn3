import { useState, useEffect } from "react";
import { Plus, Search, Edit, Trash2, X } from "lucide-react";

import { adminFetch } from "../utils/adminApi";

export default function Vocabulary() {
  // 1. Khởi tạo mảng rỗng chờ dữ liệu từ API
  const [words, setWords] = useState([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);
  
  // Đổi giá trị mặc định thành chủ đề đầu tiên trong danh sách TOEIC
  const [formData, setFormData] = useState({
    word: "",
    phonetic: "",
    meaning: "",
    category: "Hợp đồng & Kinh doanh",
  });

  const [searchTerm, setSearchTerm] = useState("");

  // 2. Tải danh sách từ vựng khi component mount
  useEffect(() => {
    fetchVocabularies();
  }, []);

  const fetchVocabularies = async () => {
    try {
      const response = await adminFetch("/api/vocabularies");
      if (response.ok) {
        const data = await response.json();
        setWords(data);
      }
    } catch (error) {
      console.error("Lỗi khi tải từ vựng:", error);
    }
  };

  // 3. Logic lọc an toàn
  const filteredWords = words.filter((item) => {
    const searchLower = searchTerm.toLowerCase();
    const word = item.word || "";
    const meaning = item.meaning || "";
    const phonetic = item.phonetic || "";
    
    return (
      word.toLowerCase().includes(searchLower) ||
      meaning.toLowerCase().includes(searchLower) ||
      phonetic.toLowerCase().includes(searchLower)
    );
  });

  const handleOpenAdd = () => {
    setEditingId(null);
    // Cập nhật giá trị mặc định khi mở modal thêm mới
    setFormData({ 
      word: "", 
      phonetic: "", 
      meaning: "", 
      category: "Hợp đồng & Kinh doanh" 
    });
    setIsModalOpen(true);
  };

  const handleOpenEdit = (item) => {
    setEditingId(item._id); 
    setFormData({
      word: item.word || "",
      phonetic: item.phonetic || "",
      meaning: item.meaning || "",
      category: item.category || "Hợp đồng & Kinh doanh",
    });
    setIsModalOpen(true);
  };

  // 4. API Xóa từ vựng
  const handleDeleteWord = async (id) => {
    const isConfirm = window.confirm(
      "Bạn có chắc chắn muốn xóa từ vựng này khỏi hệ thống?"
    );
    if (isConfirm) {
      try {
        const response = await adminFetch(`/api/vocabularies/${id}`, { method: "DELETE" });
        if (response.ok) {
          setWords(words.filter((word) => word._id !== id));
        } else {
          alert("Lỗi khi xóa từ vựng từ Server!");
        }
      } catch (error) {
        console.error("Lỗi xóa từ vựng:", error);
        alert("Không thể kết nối đến máy chủ.");
      }
    }
  };

  // 5. API Thêm mới / Cập nhật từ vựng
  const handleSaveWord = async (e) => {
    e.preventDefault();
    try {
      if (editingId) {
        const response = await adminFetch(`/api/vocabularies/${editingId}`, {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(formData)
        });

        if (!response.ok) {
          const errData = await response.json().catch(() => ({}));
          alert(errData.message || errData.error || `Không thể cập nhật từ vựng (${response.status})`);
          return;
        }
        const updatedWord = await response.json();
        setWords(
          words.map((w) =>
            w._id === editingId ? { ...w, ...updatedWord } : w
          )
        );
      } else {
        const response = await adminFetch("/api/vocabularies", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(formData)
        });

        if (!response.ok) {
          const errData = await response.json().catch(() => ({}));
          alert(errData.message || errData.error || `Không thể thêm từ vựng (${response.status})`);
          return;
        }
        const newWord = await response.json();
        setWords([newWord, ...words]);
      }
      setIsModalOpen(false);
    } catch (error) {
      console.error("Lỗi lưu từ vựng:", error);
      alert("Đã xảy ra lỗi trong quá trình lưu dữ liệu!");
    }
  };

  return (
    <div className="space-y-6 relative">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <h1 className="text-2xl font-bold text-gray-800">Quản lý Từ vựng</h1>
        <button
          onClick={handleOpenAdd}
          className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg flex items-center gap-2 transition-colors shadow-sm"
        >
          <Plus size={20} />
          <span>Thêm từ mới</span>
        </button>
      </div>

      {/* Thanh tìm kiếm */}
      <div className="bg-white p-4 rounded-xl border border-gray-200 shadow-sm flex gap-4">
        <div className="relative flex-1">
          <Search
            className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"
            size={20}
          />
          <input
            type="text"
            placeholder="Tìm kiếm từ vựng, ý nghĩa, phiên âm..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-10 pr-4 py-2 bg-gray-50 border border-gray-200 rounded-lg focus:outline-none focus:border-blue-500"
          />
        </div>
      </div>

      {/* Bảng dữ liệu */}
      <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="bg-gray-50 text-gray-600 text-sm border-b border-gray-200">
              <th className="p-4 font-medium">Từ vựng</th>
              <th className="p-4 font-medium">Phiên âm</th>
              <th className="p-4 font-medium">Nghĩa tiếng Việt</th>
              <th className="p-4 font-medium">Chủ đề</th>
              <th className="p-4 font-medium text-center">Thao tác</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200">
            {filteredWords.length > 0 ? (
              filteredWords.map((item) => (
                <tr key={item._id} className="hover:bg-gray-50">
                  <td className="p-4 font-semibold text-blue-600">
                    {item.word}
                  </td>
                  <td className="p-4 text-gray-500">{item.phonetic}</td>
                  <td className="p-4 text-gray-800">{item.meaning}</td>
                  <td className="p-4">
                    <span className="bg-blue-50 text-blue-700 px-3 py-1 rounded-full text-xs font-medium">
                      {item.category || "Chưa phân loại"}
                    </span>
                  </td>
                  <td className="p-4 text-center">
                    <button
                      onClick={() => handleOpenEdit(item)}
                      className="text-gray-400 hover:text-blue-600 mx-2 transition-colors"
                      title="Chỉnh sửa"
                    >
                      <Edit size={18} />
                    </button>
                    <button
                      onClick={() => handleDeleteWord(item._id)}
                      className="text-gray-400 hover:text-red-600 mx-2 transition-colors"
                      title="Xóa"
                    >
                      <Trash2 size={18} />
                    </button>
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan="5" className="p-8 text-center text-gray-500">
                  Không tìm thấy từ vựng nào khớp với "{searchTerm}".
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {/* MODAL THÊM / SỬA */}
      {isModalOpen && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl shadow-xl w-full max-w-md overflow-hidden">
            <div className="flex justify-between items-center p-6 border-b border-gray-200">
              <h2 className="text-xl font-bold text-gray-800">
                {editingId ? "Cập nhật từ vựng" : "Thêm từ vựng mới"}
              </h2>
              <button
                onClick={() => setIsModalOpen(false)}
                className="text-gray-400 hover:text-gray-600"
              >
                <X size={24} />
              </button>
            </div>

            <form onSubmit={handleSaveWord} className="p-6 space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Từ tiếng Anh *
                </label>
                <input
                  type="text"
                  required
                  value={formData.word}
                  onChange={(e) =>
                    setFormData({ ...formData, word: e.target.value })
                  }
                  className="w-full px-4 py-2 bg-gray-50 border border-gray-200 rounded-lg focus:outline-none focus:border-blue-500"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Phiên âm
                </label>
                <input
                  type="text"
                  value={formData.phonetic}
                  onChange={(e) =>
                    setFormData({ ...formData, phonetic: e.target.value })
                  }
                  className="w-full px-4 py-2 bg-gray-50 border border-gray-200 rounded-lg focus:outline-none focus:border-blue-500"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Nghĩa tiếng Việt *
                </label>
                <input
                  type="text"
                  required
                  value={formData.meaning}
                  onChange={(e) =>
                    setFormData({ ...formData, meaning: e.target.value })
                  }
                  className="w-full px-4 py-2 bg-gray-50 border border-gray-200 rounded-lg focus:outline-none focus:border-blue-500"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Chủ đề (Category)
                </label>
                {/* Đã cập nhật danh sách 5 chủ đề tại đây */}
                <select
                  value={formData.category}
                  onChange={(e) =>
                    setFormData({ ...formData, category: e.target.value })
                  }
                  className="w-full px-4 py-2 bg-gray-50 border border-gray-200 rounded-lg focus:outline-none focus:border-blue-500"
                >
                  <option value="Hợp đồng & Kinh doanh">Hợp đồng & Kinh doanh</option>
                  <option value="Môi trường văn phòng">Môi trường văn phòng</option>
                  <option value="Hệ thống thông tin & Công nghệ">Hệ thống thông tin & Công nghệ</option>
                  <option value="Kế toán & Tài chính">Kế toán & Tài chính</option>
                  <option value="Dịch vụ & Khách hàng">Dịch vụ & Khách hàng</option>
                </select>
              </div>

              <div className="flex gap-3 pt-4">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="flex-1 px-4 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 font-medium transition-colors"
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  className="flex-1 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 font-medium transition-colors"
                >
                  {editingId ? "Cập nhật" : "Lưu từ vựng"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}