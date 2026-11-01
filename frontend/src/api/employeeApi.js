import { apiFetch, toError } from './apiClient';

export const searchEmployees = async ({ keyword = '', deptCode = '', page = 0, size = 20 } = {}) => {
  const params = new URLSearchParams({ keyword, deptCode, page, size });
  const response = await apiFetch(`/employees?${params}`);
  if (!response.ok) {
    throw await toError(response, '사원 목록을 불러오지 못했습니다.');
  }
  return response.json();
};

const JSON_HEADERS = { 'Content-Type': 'application/json' };

export const createEmployee = async (values) => {
  const response = await apiFetch('/employees', {
    method: 'POST',
    headers: JSON_HEADERS,
    body: JSON.stringify(values),
  });
  if (!response.ok) {
    throw await toError(response, '계정을 발급하지 못했습니다.');
  }
  return response.json();
};

export const updateEmployee = async (empNo, values) => {
  const response = await apiFetch(`/employees/${encodeURIComponent(empNo)}`, {
    method: 'PUT',
    headers: JSON_HEADERS,
    body: JSON.stringify(values),
  });
  if (!response.ok) {
    throw await toError(response, '사원 정보를 수정하지 못했습니다.');
  }
  return response.json();
};

export const resetEmployeePassword = async (empNo) => {
  const response = await apiFetch(`/employees/${encodeURIComponent(empNo)}/reset-password`, {
    method: 'POST',
  });
  if (!response.ok) {
    throw await toError(response, '비밀번호를 초기화하지 못했습니다.');
  }
  return response.json();
};
