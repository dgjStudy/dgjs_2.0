import { useState } from 'react';
import { Button, Drawer, Layout, Space, Typography, theme } from 'antd';
import { DatabaseOutlined, MenuOutlined } from '@ant-design/icons';
import SideMenu from './SideMenu';
import UserMenu from './UserMenu';

const { Header, Content } = Layout;
const { Text } = Typography;

const BRAND_STYLE = { fontSize: 16, fontWeight: 600 };
const CONTENT_STYLE = { padding: '24px 28px' };

const Brand = () => {
  const { token } = theme.useToken();
  return (
    <Space size={8}>
      <DatabaseOutlined style={{ color: token.colorPrimary, fontSize: 20 }} />
      <Text style={BRAND_STYLE}>DGJS 2.0</Text>
    </Space>
  );
};

const MainLayout = ({ children }) => {
  const { token } = theme.useToken();
  const [menuOpen, setMenuOpen] = useState(false);

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          background: token.colorBgContainer,
          borderBottom: `1px solid ${token.colorBorderSecondary}`,
          padding: '0 20px',
        }}
      >
        <Space size={12}>
          <Button
            type="text"
            aria-label="메뉴 열기"
            icon={<MenuOutlined />}
            onClick={() => setMenuOpen(true)}
          />
          <Brand />
        </Space>
        <UserMenu />
      </Header>
      <Drawer
        placement="left"
        size={240}
        title={<Brand />}
        open={menuOpen}
        onClose={() => setMenuOpen(false)}
        styles={{ body: { padding: 8 } }}
      >
        <SideMenu onNavigate={() => setMenuOpen(false)} />
      </Drawer>
      <Content style={CONTENT_STYLE}>{children}</Content>
    </Layout>
  );
};

export default MainLayout;
