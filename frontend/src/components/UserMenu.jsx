import { useNavigate } from 'react-router-dom';
import { Avatar, Dropdown, Space, Typography, message } from 'antd';
import { DownOutlined, LogoutOutlined, UserOutlined } from '@ant-design/icons';
import { useAuth } from '../auth/useAuth';

const { Text } = Typography;

const TEXT_BLOCK_STYLE = { display: 'flex', flexDirection: 'column', lineHeight: 1.3, textAlign: 'left' };

const UserMenu = () => {
  const navigate = useNavigate();
  const { user, signOut } = useAuth();

  const handleLogout = async () => {
    try {
      await signOut();
    } catch (err) {
      message.error(err.message || '로그아웃에 실패했습니다.');
    }
  };

  const items = [
    { key: 'me', icon: <UserOutlined />, label: '내 정보', onClick: () => navigate('/me') },
    { type: 'divider' },
    { key: 'logout', icon: <LogoutOutlined />, label: '로그아웃', danger: true, onClick: handleLogout },
  ];

  return (
    <Dropdown menu={{ items }} trigger={['click']}>
      <Space size={10} style={{ cursor: 'pointer' }}>
        <Avatar icon={<UserOutlined />} />
        <div style={TEXT_BLOCK_STYLE}>
          <Text strong>{user.name}</Text>
          <Text type="secondary" style={{ fontSize: 12 }}>
            {user.deptName} · {user.jobTitle}
          </Text>
        </div>
        <DownOutlined style={{ fontSize: 12 }} />
      </Space>
    </Dropdown>
  );
};

export default UserMenu;
