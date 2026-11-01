export const JOB_TITLES = ['사원', '대리', '과장', '차장', '부장', '임원'];

export const ROLE_OPTIONS = [
  { value: 'USER', label: '일반 사용자' },
  { value: 'ADMIN', label: '관리자' },
];

export const STATUS_OPTIONS = [
  { value: 'ACTIVE', label: '재직' },
  { value: 'INACTIVE', label: '비활성' },
];

export const labelOf = (options, value) => options.find((o) => o.value === value)?.label ?? value;
