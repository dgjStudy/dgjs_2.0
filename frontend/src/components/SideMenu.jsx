import { Menu } from 'antd';
import { useLocation, useNavigate } from 'react-router-dom';
import { ApartmentOutlined, ProjectOutlined, TableOutlined } from '@ant-design/icons';
import { useAuth } from '../auth/useAuth';

const DATA_ITEMS = [
  { key: '/data', icon: <TableOutlined />, label: '데이터셋' },
];

const ADMIN_ITEMS = [
  {
    key: 'admin',
    type: 'group',
    label: '관리',
    children: [
      { key: '/admin/departments', icon: <ApartmentOutlined />, label: '부서' },
      { key: '/admin/projects', icon: <ProjectOutlined />, label: '프로젝트' },
    ],
  },
];

const findSelectedKey = (pathname, items) =>
  items
    .flatMap((item) => item.children ?? [item])
    .map((item) => item.key)
    .find((key) => pathname === key || pathname.startsWith(`${key}/`));

const SideMenu = ({ onNavigate }) => {
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const { user } = useAuth();

  const items = user?.role === 'ADMIN' ? [...DATA_ITEMS, ...ADMIN_ITEMS] : DATA_ITEMS;
  const selectedKey = findSelectedKey(pathname, items);

  const handleClick = ({ key }) => {
    navigate(key);
    onNavigate?.();
  };

  return (
    <Menu
      mode="inline"
      selectedKeys={selectedKey ? [selectedKey] : []}
      items={items}
      onClick={handleClick}
      style={{ borderInlineEnd: 'none' }}
    />
  );
};

export default SideMenu;
