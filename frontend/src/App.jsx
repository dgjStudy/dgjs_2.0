import { BrowserRouter, Routes, Route } from 'react-router-dom';
import MainLayout from './components/MainLayout';
import DatasetListPage from './pages/DatasetListPage';
import DatasetDetailPage from './pages/DatasetDetailPage';
import UploadPage from './pages/UploadPage';

export default function App() {
  return (
    <BrowserRouter>
      <MainLayout>
        <Routes>
          <Route path="/" element={<DatasetListPage />} />
          <Route path="/data" element={<DatasetListPage />} />
          <Route path="/data/:id" element={<DatasetDetailPage />} />
          <Route path="/upload" element={<UploadPage />} />
        </Routes>
      </MainLayout>
    </BrowserRouter>
  );
}
