import { Routes, Route, Navigate } from "react-router-dom";
import AdminLayout from "./layouts/AdminLayout";
import Dashboard from "./pages/Dashboard";
import Login from "./pages/Login";
import Vocabulary from "./pages/Vocabulary";
import Users from "./pages/Users";
import XaiAnalytics from "./pages/XaiAnalytics";

// Component làm nhiệm vụ gác cổng
const ProtectedRoute = ({ children }) => {
  const isAuthenticated = localStorage.getItem("adminToken");
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }
  return children;
};

function App() {
  return (
    <Routes>
      {/* Đường dẫn mở: Không cần đăng nhập */}
      <Route path="/login" element={<Login />} />

      {/* Đường dẫn kín: Bắt buộc phải qua ProtectedRoute */}
      <Route
        path="/"
        element={
          <ProtectedRoute>
            <AdminLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<Dashboard />} />
        <Route path="vocabulary" element={<Vocabulary />} />
        <Route path="users" element={<Users />} />
        <Route path="xai-analytics" element={<XaiAnalytics />} />
      </Route>
    </Routes>
  );
}

export default App;