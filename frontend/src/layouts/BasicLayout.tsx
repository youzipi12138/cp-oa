import {
  BellOutlined,
  DashboardOutlined,
  LogoutOutlined,
  TeamOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { ProLayout } from '@ant-design/pro-components';
import { Dropdown, message, Tooltip } from 'antd';
import { useEffect, useState } from 'react';
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { getCurrentUser, logout } from '@/services/auth';
import type { UserInfo } from '@/types';
import { clearAuth, getStoredUser, setStoredUser } from '@/utils/auth';

const menuRoute = {
  path: '/',
  routes: [
    { path: '/dashboard', name: '工作台', icon: <DashboardOutlined /> },
    { path: '/users', name: '用户管理', icon: <TeamOutlined /> },
  ],
};

export default function BasicLayout() {
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const [user, setUser] = useState<UserInfo | null>(() => getStoredUser());

  useEffect(() => {
    getCurrentUser()
      .then((data) => {
        setUser(data);
        setStoredUser(data);
      })
      .catch(() => {
        // 401 已由请求拦截器统一处理，这里不额外提示
      });
  }, []);

  const handleLogout = async () => {
    try {
      await logout();
    } catch {
      // 退出接口失败也照常清理本地登录态
    }
    clearAuth();
    message.success('已退出登录');
    navigate('/login', { replace: true });
  };

  return (
    <ProLayout
      title="CP OA"
      logo={<img src="/favicon.svg" alt="CP OA" style={{ width: 28, height: 28 }} />}
      layout="mix"
      fixedHeader
      fixSiderbar
      location={{ pathname }}
      route={menuRoute}
      menuItemRender={(item, dom) =>
        item.path ? <Link to={item.path}>{dom}</Link> : dom
      }
      onMenuHeaderClick={() => navigate('/dashboard')}
      actionsRender={() => [
        <Tooltip key="notice" title="暂无通知">
          <BellOutlined style={{ fontSize: 16 }} />
        </Tooltip>,
      ]}
      avatarProps={{
        icon: <UserOutlined />,
        title: user?.realName || user?.username || '未登录',
        render: (_props, dom) => (
          <Dropdown
            menu={{
              items: [
                {
                  key: 'role',
                  label: user?.role === 'admin' ? '管理员' : '普通用户',
                  disabled: true,
                },
                { type: 'divider' },
                { key: 'logout', icon: <LogoutOutlined />, label: '退出登录' },
              ],
              onClick: ({ key }) => {
                if (key === 'logout') {
                  void handleLogout();
                }
              },
            }}
          >
            {dom}
          </Dropdown>
        ),
      }}
    >
      <Outlet />
    </ProLayout>
  );
}
