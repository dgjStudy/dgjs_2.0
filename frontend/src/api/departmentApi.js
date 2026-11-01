import { apiFetch, toError } from './apiClient';

const JSON_HEADERS = { 'Content-Type': 'application/json' };

export const fetchDepartments = async () => {
  const response = await apiFetch('/departments');
  if (!response.ok) {
    throw await toError(response, '부서 목록을 불러오지 못했습니다.');
  }
  return response.json();
};

export const createDepartment = async (name) => {
  const response = await apiFetch('/departments', {
    method: 'POST',
    headers: JSON_HEADERS,
    body: JSON.stringify({ name }),
  });
  if (!response.ok) {
    throw await toError(response, '부서를 추가하지 못했습니다.');
  }
  return response.json();
};

export const updateDepartment = async (deptCode, name) => {
  const response = await apiFetch(`/departments/${encodeURIComponent(deptCode)}`, {
    method: 'PUT',
    headers: JSON_HEADERS,
    body: JSON.stringify({ name }),
  });
  if (!response.ok) {
    throw await toError(response, '부서명을 수정하지 못했습니다.');
  }
  return response.json();
};

export const deleteDepartment = async (deptCode) => {
  const response = await apiFetch(`/departments/${encodeURIComponent(deptCode)}`, {
    method: 'DELETE',
  });
  if (!response.ok) {
    throw await toError(response, '부서를 삭제하지 못했습니다.');
  }
};
