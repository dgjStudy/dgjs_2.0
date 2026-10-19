import { Layout, Menu } from 'antd';
import { useNavigate, useLocation } from 'react-router-dom';
import { DatabaseOutlined, UploadOutlined } from '@ant-design/icons';

const { Header, Content } = Layout;

const MainLayout = ({ children }) => {
  const navigate = useNavigate();
  const location = useLocation();

  const getSelectedKey = () => {
    if (location.pathname.startsWith('/data')) return '/data';
    if (location.pathname === '/upload') return '/upload';
    return '/data';
  };

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header style={{ display: 'flex', alignItems: 'center', background: '#001529', padding: '0 24px' }}>
        <div style={{ color: '#fff', fontSize: '18px', fontWeight: 'bold', marginRight: '40px', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <DatabaseOutlined style={{ color: '#1890ff', fontSize: '22px' }} />
          <span>DGJS 2.0 Platform</span>
        </div>
        <Menu
          theme="dark"
          mode="horizontal"
          selectedKeys={[getSelectedKey()]}
          style={{ flex: 1, minWidth: 0 }}
          items={[
            { key: '/data', icon: <DatabaseOutlined />, label: '전체 데이터셋 목록', onClick: () => navigate('/data') },
            { key: '/upload', icon: <UploadOutlined />, label: '데이터 적재 및 스키마 정의', onClick: () => navigate('/upload') },
          ]}
        />
      </Header>
      <Content style={{ padding: '24px 48px', background: '#f0f2f5' }}>
        {children}
      </Content>
    </Layout>
  );
};

export default MainLayout;
