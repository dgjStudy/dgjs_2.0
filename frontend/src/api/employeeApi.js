import { apiFetch, toError } from './apiClient';

export const searchEmployees = async ({ keyword = '', deptCode = '', page = 0, size = 20 } = {}) => {
  const params = new URLSearchParams({ keyword, deptCode, page, size });
  const response = await apiFetch(`/employees?${params}`);
  if (!response.ok) {
    throw await toError(response, '사원 목록을 불러오지 못했습니다.');
  }
  return response.json();
};
