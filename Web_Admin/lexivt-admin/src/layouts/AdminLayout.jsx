import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { LayoutDashboard, BookOpen, Users, LogOut, Bell } from 'lucide-react';

export default function AdminLayout() {
  const navigate = useNavigate();

  const navItems = [
    { name: 'Dashboard', path: '/', icon: <LayoutDashboard size={20} /> },
    { name: 'Từ vựng', path: '/vocabulary', icon: <BookOpen size={20} /> },
    { name: 'Người dùng', path: '/users', icon: <Users size={20} /> },
  ];

  const handleLogout = () => {
    localStorage.removeItem('adminToken'); // Xóa token
    navigate('/login'); // Đẩy về trang đăng nhập
  };

  return (
    <div className="flex h-screen bg-gray-50">
      {/* Sidebar bên trái */}
      <aside className="w-64 bg-white border-r border-gray-200 flex flex-col">
        <div className="h-16 flex items-center px-6 border-b border-gray-200">
          <span className="text-2xl font-bold text-blue-600">LexiVT Admin</span>
        </div>
        
        <nav className="flex-1 py-4 flex flex-col gap-1 px-3">
          {navItems.map((item) => (
            <NavLink
              key={item.name}
              to={item.path}
              className={({ isActive }) =>
                `flex items-center gap-3 px-3 py-2.5 rounded-lg transition-colors ${
                  isActive 
                    ? 'bg-blue-50 text-blue-600 font-medium' 
                    : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'
                }`
              }
            >
              {item.icon}
              {item.name}
            </NavLink>
          ))}
        </nav>

        <div className="p-4 border-t border-gray-200">
          <button 
            onClick={handleLogout}
            className="flex items-center gap-3 px-3 py-2.5 w-full text-red-600 hover:bg-red-50 rounded-lg transition-colors"
          >
            <LogOut size={20} />
            <span className="font-medium">Đăng xuất</span>
          </button>
        </div>
      </aside>

      {/* Khu vực nội dung bên phải */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* Header (Thanh Topbar) */}
        <header className="h-16 bg-white border-b border-gray-200 flex items-center justify-between px-8">
          <h2 className="text-xl font-semibold text-gray-800">Trang quản trị</h2>
          <div className="flex items-center gap-4">
            <button className="p-2 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-full">
              <Bell size={20} />
            </button>
            <div className="flex items-center gap-2 cursor-pointer">
              <div className="w-8 h-8 bg-blue-600 rounded-full flex items-center justify-center text-white font-bold">
                A
              </div>
              <span className="text-sm font-medium text-gray-700">Admin</span>
            </div>
          </div>
        </header>

        {/* Main Content (Nội dung thay đổi theo route) */}
        <main className="flex-1 overflow-y-auto p-8">
          <Outlet />
        </main>
      </div>
    </div>
  );
}