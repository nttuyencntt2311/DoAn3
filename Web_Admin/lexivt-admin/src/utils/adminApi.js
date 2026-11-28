const API_BASE_URL = import.meta.env.VITE_API_URL || "http://localhost:8080";

let refreshPromise = null;

function clearAdminSessionAndRedirect() {
  localStorage.removeItem("adminToken");
  localStorage.removeItem("adminRefreshToken");
  localStorage.removeItem("adminUser");
  if (window.location.pathname !== "/login") {
    window.location.href = "/login";
  }
}

async function refreshAdminToken() {
  const refreshToken = localStorage.getItem("adminRefreshToken");
  if (!refreshToken) return null;

  try {
    const res = await fetch(`${API_BASE_URL}/api/auth/refresh-token`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    });
    if (!res.ok) return null;
    const data = await res.json();
    if (data?.token) {
      localStorage.setItem("adminToken", data.token);
      return data.token;
    }
    return null;
  } catch {
    return null;
  }
}

export async function adminFetch(path, options = {}) {
  const token = localStorage.getItem("adminToken");
  const headers = {
    ...(options.headers || {}),
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };

  let response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
  });

  if (response.status === 401) {
    if (!refreshPromise) {
      refreshPromise = refreshAdminToken().finally(() => {
        refreshPromise = null;
      });
    }
    const newToken = await refreshPromise;
    if (!newToken) {
      clearAdminSessionAndRedirect();
      return response;
    }

    response = await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      headers: {
        ...(options.headers || {}),
        Authorization: `Bearer ${newToken}`,
      },
    });

    if (response.status === 401) {
      clearAdminSessionAndRedirect();
    }
  }

  return response;
}
