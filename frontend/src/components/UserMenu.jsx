import { Avatar, Dropdown, Space, Typography } from 'antd';
import { DownOutlined, UserOutlined } from '@ant-design/icons';
import { MOCK_USER } from '../constants/mockUser';

const { Text } = Typography;

const TEXT_BLOCK_STYLE = { display: 'flex', flexDirection: 'column', lineHeight: 1.3, textAlign: 'left' };

const UserMenu = () => (
  <Dropdown
    menu={{ items: [{ key: 'me', label: '내 정보 (준비 중)', disabled: true }] }}
    trigger={['click']}
  >
    <Space size={10} style={{ cursor: 'pointer' }}>
      <Avatar icon={<UserOutlined />} />
      <div style={TEXT_BLOCK_STYLE}>
        <Text strong>{MOCK_USER.name}</Text>
        <Text type="secondary" style={{ fontSize: 12 }}>
          {MOCK_USER.deptName} · {MOCK_USER.jobTitle}
        </Text>
      </div>
      <DownOutlined style={{ fontSize: 12 }} />
    </Space>
  </Dropdown>
);

export default UserMenu;
