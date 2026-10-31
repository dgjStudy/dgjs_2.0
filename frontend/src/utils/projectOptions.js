export const toProjectOptions = (projects) =>
  projects.map((p) => ({ value: p.projectCode, label: p.name }));

// 현재 연결된 프로젝트가 선택지에 없어도(참여가 해제된 경우 등) 이름이 보이도록 끼워 넣음
export const withCurrentProject = (options, projectCode, projectName) => {
  if (!projectCode || options.some((o) => o.value === projectCode)) {
    return options;
  }
  return [{ value: projectCode, label: projectName || projectCode }, ...options];
};
