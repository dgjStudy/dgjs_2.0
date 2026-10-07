const API_BASE_URL = 'http://localhost:8080/api';

export const fetchDatasets = async () => {
  const response = await fetch(`${API_BASE_URL}/datasets`);
  if (!response.ok) {
    throw new Error('데이터셋 목록을 불러오는 중 오류가 발생했습니다.');
  }
  return response.json();
};

export const fetchDatasetDetail = async (id) => {
  const response = await fetch(`${API_BASE_URL}/datasets/${id}`);
  if (!response.ok) {
    throw new Error(`데이터셋 상세 정보(ID: ${id})를 불러오지 못했습니다.`);
  }
  return response.json();
};

export const uploadDatasetFile = async (formData) => {
  const response = await fetch(`${API_BASE_URL}/upload`, {
    method: 'POST',
    body: formData,
  });
  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.message || '엑셀 업로드 처리에 실패했습니다.');
  }
  return response.json();
};

export const deleteDataset = async (id) => {
  const response = await fetch(`${API_BASE_URL}/datasets/${id}`, {
    method: 'DELETE',
  });
  if (!response.ok) {
    throw new Error('데이터셋 삭제 실패');
  }
  return response.json();
};
