import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import "./index.css";
import App from "./App.jsx";

// 1. Import công cụ BrowserRouter từ thư viện react-router-dom
import { BrowserRouter } from "react-router-dom";

createRoot(document.getElementById("root")).render(
  <StrictMode>
    {/* 2. Dùng BrowserRouter để bọc lấy component <App /> */}
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </StrictMode>,
);
