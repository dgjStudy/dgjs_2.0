import { apiFetch, toError } from './apiClient';

// 데이터셋을 연결할 수 있는 프로젝트 목록임. ADMIN은 전체, 그 밖에는 참여 중인 프로젝트만 내려옴
export const fetchMyProjects = async () => {
  const response = await apiFetch('/projects/mine');
  if (!response.ok) {
    throw await toError(response, '프로젝트 목록을 불러오지 못했습니다.');
  }
  return response.json();
};

const JSON_HEADERS = { 'Content-Type': 'application/json' };
const projectPath = (projectCode) => `/projects/${encodeURIComponent(projectCode)}`;

export const fetchProjects = async () => {
  const response = await apiFetch('/projects');
  if (!response.ok) {
    throw await toError(response, '프로젝트 목록을 불러오지 못했습니다.');
  }
  return response.json();
};

export const createProject = async (name) => {
  const response = await apiFetch('/projects', {
    method: 'POST',
    headers: JSON_HEADERS,
    body: JSON.stringify({ name }),
  });
  if (!response.ok) {
    throw await toError(response, '프로젝트를 추가하지 못했습니다.');
  }
  return response.json();
};

export const updateProject = async (projectCode, name) => {
  const response = await apiFetch(projectPath(projectCode), {
    method: 'PUT',
    headers: JSON_HEADERS,
    body: JSON.stringify({ name }),
  });
  if (!response.ok) {
    throw await toError(response, '프로젝트명을 수정하지 못했습니다.');
  }
  return response.json();
};

export const deleteProject = async (projectCode) => {
  const response = await apiFetch(projectPath(projectCode), { method: 'DELETE' });
  if (!response.ok) {
    throw await toError(response, '프로젝트를 삭제하지 못했습니다.');
  }
};

export const fetchProjectMembers = async (projectCode) => {
  const response = await apiFetch(`${projectPath(projectCode)}/members`);
  if (!response.ok) {
    throw await toError(response, '참여자 목록을 불러오지 못했습니다.');
  }
  return response.json();
};

export const addProjectMember = async (projectCode, empNo) => {
  const response = await apiFetch(`${projectPath(projectCode)}/members`, {
    method: 'POST',
    headers: JSON_HEADERS,
    body: JSON.stringify({ empNo }),
  });
  if (!response.ok) {
    throw await toError(response, '참여자를 추가하지 못했습니다.');
  }
};

export const removeProjectMember = async (projectCode, empNo) => {
  const response = await apiFetch(`${projectPath(projectCode)}/members/${encodeURIComponent(empNo)}`, {
    method: 'DELETE',
  });
  if (!response.ok) {
    throw await toError(response, '참여자를 제거하지 못했습니다.');
  }
};
