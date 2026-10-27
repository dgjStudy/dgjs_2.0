import { apiFetch, toError } from './apiClient';

const JSON_HEADERS = { 'Content-Type': 'application/json' };

export const login = async (email, password) => {
  const response = await apiFetch('/auth/login', {
    method: 'POST',
    headers: JSON_HEADERS,
    body: JSON.stringify({ email, password }),
    skipUnauthorizedHandler: true,
  });
  if (!response.ok) {
    throw await toError(response, '로그인에 실패했습니다.');
  }
  return response.json();
};

export const logout = async () => {
  const response = await apiFetch('/auth/logout', { method: 'POST', skipUnauthorizedHandler: true });
  if (!response.ok && response.status !== 401) {
    throw await toError(response, '로그아웃에 실패했습니다.');
  }
};

// 로그인하지 않은 상태(401)는 오류가 아니라 null로 돌려줌
export const fetchMe = async () => {
  const response = await apiFetch('/auth/me', { skipUnauthorizedHandler: true });
  if (response.status === 401) {
    return null;
  }
  if (!response.ok) {
    throw await toError(response, '사용자 정보를 불러오지 못했습니다.');
  }
  return response.json();
};

export const changePassword = async (currentPassword, newPassword) => {
  const response = await apiFetch('/auth/change-password', {
    method: 'POST',
    headers: JSON_HEADERS,
    body: JSON.stringify({ currentPassword, newPassword }),
  });
  if (!response.ok) {
    throw await toError(response, '비밀번호 변경에 실패했습니다.');
  }
  return response.json();
};
