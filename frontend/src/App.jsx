import { BrowserRouter, Routes, Route, Navigate, Outlet } from 'react-router-dom';
import { ConfigProvider } from 'antd';
import AuthProvider from './auth/AuthProvider';
import MainLayout from './components/MainLayout';
import RequireAuth from './components/RequireAuth';
import DatasetListPage from './pages/DatasetListPage';
import DatasetDetailPage from './pages/DatasetDetailPage';
import LoginPage from './pages/LoginPage';
import MyPage from './pages/MyPage';
import UploadPage from './pages/UploadPage';
import { getThemeConfig } from './theme/theme';

export default function App() {
  return (
    <ConfigProvider theme={getThemeConfig('light')}>
      <BrowserRouter>
        <AuthProvider>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route
              element={
                <RequireAuth>
                  <MainLayout>
                    <Outlet />
                  </MainLayout>
                </RequireAuth>
              }
            >
              <Route path="/" element={<Navigate to="/data" replace />} />
              <Route path="/data" element={<DatasetListPage />} />
              <Route path="/data/:id" element={<DatasetDetailPage />} />
              <Route path="/upload" element={<UploadPage />} />
              <Route path="/me" element={<MyPage />} />
            </Route>
          </Routes>
        </AuthProvider>
      </BrowserRouter>
    </ConfigProvider>
  );
}
