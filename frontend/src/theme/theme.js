import { theme as antdTheme } from 'antd';

const PRIMARY_COLOR = '#1668dc';

const LIGHT_TOKEN = {
  colorPrimary: PRIMARY_COLOR,
  colorBgLayout: '#f5f6f8',
  borderRadius: 8,
};

const DARK_TOKEN = {
  colorPrimary: PRIMARY_COLOR,
  borderRadius: 8,
};

// mode는 'light' | 'dark'. 다크 전환 기능은 후속 작업이며 현재는 light만 사용함
export const getThemeConfig = (mode = 'light') => ({
  algorithm: mode === 'dark' ? antdTheme.darkAlgorithm : antdTheme.defaultAlgorithm,
  token: mode === 'dark' ? DARK_TOKEN : LIGHT_TOKEN,
});
