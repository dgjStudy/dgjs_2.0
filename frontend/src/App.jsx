import { BrowserRouter, Routes, Route, Navigate, Outlet } from 'react-router-dom';
import { ConfigProvider, message } from 'antd';
import AuthProvider from './auth/AuthProvider';
import MainLayout from './components/MainLayout';
import RequireAdmin from './components/RequireAdmin';
import RequireAuth from './components/RequireAuth';
import AdminDepartmentPage from './pages/AdminDepartmentPage';
import AdminEmployeePage from './pages/AdminEmployeePage';
import AdminProjectPage from './pages/AdminProjectPage';
import DatasetListPage from './pages/DatasetListPage';
import DatasetDetailPage from './pages/DatasetDetailPage';
import LoginPage from './pages/LoginPage';
import MyPage from './pages/MyPage';
import UploadPage from './pages/UploadPage';
import { getThemeConfig } from './theme/theme';

// 오류가 반복돼도 알림이 화면을 덮지 않도록 동시 표시 개수를 제한함
message.config({ maxCount: 3 });

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
              <Route element={<RequireAdmin />}>
                <Route path="/admin/departments" element={<AdminDepartmentPage />} />
                <Route path="/admin/projects" element={<AdminProjectPage />} />
                <Route path="/admin/employees" element={<AdminEmployeePage />} />
              </Route>
            </Route>
          </Routes>
        </AuthProvider>
      </BrowserRouter>
    </ConfigProvider>
  );
}
