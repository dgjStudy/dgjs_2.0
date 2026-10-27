const API_BASE_URL = '/api';
const SAFE_METHODS = ['GET', 'HEAD', 'OPTIONS'];
const CSRF_COOKIE = 'XSRF-TOKEN';
const CSRF_HEADER = 'X-XSRF-TOKEN';

let unauthorizedHandler = null;

// 로그인이 풀렸을 때(401) 호출할 함수를 등록함. AuthProvider가 사용자 정보를 비우는 데 씀
export const setUnauthorizedHandler = (handler) => {
  unauthorizedHandler = handler;
};

const readCookie = (name) => {
  const entry = document.cookie.split('; ').find((row) => row.startsWith(`${name}=`));
  return entry ? decodeURIComponent(entry.slice(name.length + 1)) : null;
};

// 서버가 응답마다 내려주는 CSRF 쿠키가 없으면 가벼운 조회 한 번으로 받아 옴
const ensureCsrfToken = async () => {
  if (!readCookie(CSRF_COOKIE)) {
    await fetch(`${API_BASE_URL}/auth/me`, { credentials: 'same-origin' });
  }
  return readCookie(CSRF_COOKIE);
};

export const apiFetch = async (path, options = {}) => {
  const { skipUnauthorizedHandler, ...fetchOptions } = options;
  const method = (fetchOptions.method || 'GET').toUpperCase();
  const headers = { ...(fetchOptions.headers || {}) };

  if (!SAFE_METHODS.includes(method)) {
    const token = await ensureCsrfToken();
    if (token) {
      headers[CSRF_HEADER] = token;
    }
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...fetchOptions,
    headers,
    credentials: 'same-origin',
  });

  if (response.status === 401 && !skipUnauthorizedHandler && unauthorizedHandler) {
    unauthorizedHandler();
  }
  return response;
};

export const toError = async (response, fallback) => {
  const errorData = await response.json().catch(() => ({}));
  return new Error(errorData.message || fallback);
};
