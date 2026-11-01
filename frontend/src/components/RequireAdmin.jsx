import { Button, Result } from 'antd';
import { Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';

// ADMIN이 아니면 접근 불가 화면을 보여 주는 라우트 가드임. RequireAuth 아래에서 쓴다
const RequireAdmin = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  if (user?.role !== 'ADMIN') {
    return (
      <Result
        status="403"
        title="접근 권한이 없습니다."
        subTitle="관리자만 사용할 수 있는 화면입니다."
        extra={<Button type="primary" onClick={() => navigate('/data')}>데이터셋으로 이동</Button>}
      />
    );
  }
  return <Outlet />;
};

export default RequireAdmin;
