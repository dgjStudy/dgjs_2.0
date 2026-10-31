import { apiFetch, toError } from './apiClient';

// 데이터셋을 연결할 수 있는 프로젝트 목록임. ADMIN은 전체, 그 밖에는 참여 중인 프로젝트만 내려옴
export const fetchMyProjects = async () => {
  const response = await apiFetch('/projects/mine');
  if (!response.ok) {
    throw await toError(response, '프로젝트 목록을 불러오지 못했습니다.');
  }
  return response.json();
};
