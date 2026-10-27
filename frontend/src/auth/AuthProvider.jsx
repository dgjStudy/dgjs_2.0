import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { message } from 'antd';
import { fetchMe, login, logout } from '../api/authApi';
import { setUnauthorizedHandler } from '../api/apiClient';
import { AuthContext } from './AuthContext';

// 현재 로그인한 사용자 정보만 담는 컨텍스트임. 그 밖의 서버 데이터는 페이지에서 로드함
const AuthProvider = ({ children }) => {
  const navigate = useNavigate();
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setUnauthorizedHandler(() => setUser(null));
    fetchMe()
      .then(setUser)
      .catch((err) => {
        message.error(err.message || '사용자 정보를 불러오지 못했습니다.');
        setUser(null);
      })
      .finally(() => setLoading(false));
    return () => setUnauthorizedHandler(null);
  }, []);

  const value = useMemo(
    () => ({
      user,
      loading,
      signIn: async (email, password) => {
        const me = await login(email, password);
        setUser(me);
        return me;
      },
      signOut: async () => {
        try {
          await logout();
        } finally {
          // 같은 동기 구간에서 처리해 로그인 화면으로 이동한 뒤 사용자 정보가 비워지게 함
          navigate('/login', { replace: true });
          setUser(null);
        }
      },
      updateUser: setUser,
    }),
    [user, loading, navigate],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export default AuthProvider;
