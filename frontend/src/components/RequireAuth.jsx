import { Navigate, useLocation } from 'react-router-dom';
import { Spin } from 'antd';
import { useAuth } from '../auth/useAuth';

const CENTER_STYLE = { textAlign: 'center', padding: '120px 0' };

// 로그인하지 않았으면 로그인 화면으로, 임시 비밀번호 상태이면 내 정보 화면으로 보냄
const RequireAuth = ({ children }) => {
  const { user, loading } = useAuth();
  const location = useLocation();

  if (loading) {
    return (
      <div style={CENTER_STYLE}>
        <Spin size="large" description="불러오는 중..." />
      </div>
    );
  }
  if (!user) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }
  if (user.mustChangePassword && location.pathname !== '/me') {
    return <Navigate to="/me" replace />;
  }
  return children;
};

export default RequireAuth;
