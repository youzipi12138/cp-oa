import { LockOutlined, UserOutlined } from '@ant-design/icons';
import { LoginForm, ProFormText } from '@ant-design/pro-components';
import { message } from 'antd';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { login } from '@/services/auth';
import type { LoginParams } from '@/types';
import { getToken, setStoredUser, setToken } from '@/utils/auth';

export default function Login() {
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as { from?: string } | null)?.from || '/dashboard';

  if (getToken()) {
    return <Navigate to="/dashboard" replace />;
  }

  const handleFinish = async (values: LoginParams) => {
    try {
      const data = await login(values);
      setToken(data.token);
      setStoredUser(data.user);
      message.success(`欢迎回来，${data.user.realName || data.user.username}`);
      navigate(from, { replace: true });
      return true;
    } catch {
      return false;
    }
  };

  return (
    <div className="login-page">
      <div className="login-card">
        <LoginForm<LoginParams>
          logo={
            <img src={`${import.meta.env.BASE_URL}favicon.svg`} alt="CP OA" style={{ height: 40 }} />
          }
          title="CP OA"
          subTitle="企业办公自动化系统"
          initialValues={{ username: 'admin' }}
          submitter={{ searchConfig: { submitText: '登 录' } }}
          onFinish={handleFinish}
        >
          <ProFormText
            name="username"
            fieldProps={{ size: 'large', prefix: <UserOutlined /> }}
            placeholder="用户名"
            rules={[{ required: true, message: '请输入用户名' }]}
          />
          <ProFormText.Password
            name="password"
            fieldProps={{ size: 'large', prefix: <LockOutlined /> }}
            placeholder="密码"
            rules={[{ required: true, message: '请输入密码' }]}
          />
        </LoginForm>
      </div>
    </div>
  );
}
