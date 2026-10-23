import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ConfigProvider } from 'antd';
import MainLayout from './components/MainLayout';
import DatasetListPage from './pages/DatasetListPage';
import DatasetDetailPage from './pages/DatasetDetailPage';
import UploadPage from './pages/UploadPage';
import { getThemeConfig } from './theme/theme';

export default function App() {
  return (
    <ConfigProvider theme={getThemeConfig('light')}>
      <BrowserRouter>
        <MainLayout>
          <Routes>
            <Route path="/" element={<Navigate to="/data" replace />} />
            <Route path="/data" element={<DatasetListPage />} />
            <Route path="/data/:id" element={<DatasetDetailPage />} />
            <Route path="/upload" element={<UploadPage />} />
          </Routes>
        </MainLayout>
      </BrowserRouter>
    </ConfigProvider>
  );
}
