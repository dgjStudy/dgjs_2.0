import { Menu } from 'antd';
import { useNavigate } from 'react-router-dom';
import { TableOutlined } from '@ant-design/icons';

const MENU_ITEMS = [
  { key: '/data', icon: <TableOutlined />, label: '데이터셋' },
];

const SideMenu = ({ onNavigate }) => {
  const navigate = useNavigate();

  const handleClick = ({ key }) => {
    navigate(key);
    onNavigate?.();
  };

  return (
    <Menu
      mode="inline"
      selectedKeys={['/data']}
      items={MENU_ITEMS}
      onClick={handleClick}
      style={{ borderInlineEnd: 'none' }}
    />
  );
};

export default SideMenu;
