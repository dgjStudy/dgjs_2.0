import { apiFetch, toError } from './apiClient';

export const fetchDatasets = async () => {
  const response = await apiFetch('/datasets');
  if (!response.ok) {
    throw await toError(response, '데이터셋 목록을 불러오는 중 오류가 발생했습니다.');
  }
  return response.json();
};

export const fetchDatasetDetail = async (id) => {
  const response = await apiFetch(`/datasets/${id}`);
  if (!response.ok) {
    throw await toError(response, `데이터셋 상세 정보(ID: ${id})를 불러오지 못했습니다.`);
  }
  return response.json();
};

export const fetchDatasetRows = async (id, page = 0, size = 20) => {
  const response = await apiFetch(`/datasets/${id}/rows?page=${page}&size=${size}`);
  if (!response.ok) {
    throw await toError(response, `행 데이터를 불러오지 못했습니다.`);
  }
  return response.json();
};

export const previewDatasetFile = async (formData) => {
  const response = await apiFetch(`/preview`, {
    method: 'POST',
    body: formData,
  });
  if (!response.ok) {
    throw await toError(response, '파일 파싱 및 검증 중 오류가 발생했습니다.');
  }
  return response.json();
};

export const uploadDatasetAsync = async (formData) => {
  const response = await apiFetch(`/upload-async`, {
    method: 'POST',
    body: formData,
  });
  if (!response.ok) {
    throw await toError(response, '비동기 업로드 처리에 실패했습니다.');
  }
  return response.json();
};

export const fetchTaskProgress = async (taskId) => {
  const response = await apiFetch(`/tasks/${taskId}/progress`);
  if (!response.ok) {
    throw new Error('진행 상태를 조회하지 못했습니다.');
  }
  return response.json();
};

export const deleteDataset = async (id) => {
  const response = await apiFetch(`/datasets/${id}`, {
    method: 'DELETE',
  });
  if (!response.ok) {
    throw await toError(response, '데이터셋 삭제 실패');
  }
  return response.json();
};

export const createManualDataset = async (payload) => {
  const response = await apiFetch(`/datasets/manual`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  });
  if (!response.ok) {
    throw await toError(response, '수동 데이터셋 생성에 실패했습니다.');
  }
  return response.json();
};

export const updateDatasetMeta = async (id, meta) => {
  const response = await apiFetch(`/datasets/${id}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(meta),
  });
  if (!response.ok) {
    throw await toError(response, '데이터셋 정보 수정에 실패했습니다.');
  }
  return response.json();
};

export const addDatasetRow = async (datasetId, rowData) => {
  const response = await apiFetch(`/datasets/${datasetId}/rows`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(rowData),
  });
  if (!response.ok) {
    throw await toError(response, '데이터 행 추가에 실패했습니다.');
  }
  return response.json();
};

export const updateDatasetRow = async (datasetId, rowId, rowData) => {
  const response = await apiFetch(`/datasets/${datasetId}/rows/${rowId}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(rowData),
  });
  if (!response.ok) {
    throw await toError(response, '데이터 행 수정에 실패했습니다.');
  }
  return response.json();
};

export const deleteDatasetRow = async (datasetId, rowId) => {
  const response = await apiFetch(`/datasets/${datasetId}/rows/${rowId}`, {
    method: 'DELETE',
  });
  if (!response.ok) {
    throw await toError(response, '데이터 행 삭제에 실패했습니다.');
  }
  return response.json();
};

