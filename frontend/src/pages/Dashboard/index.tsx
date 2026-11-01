import {
  AuditOutlined,
  FileTextOutlined,
  TeamOutlined,
} from '@ant-design/icons';
import { PageContainer } from '@ant-design/pro-components';
import { Card, Col, Row, Statistic, Typography } from 'antd';
import { getStoredUser } from '@/utils/auth';

const { Paragraph, Text, Title } = Typography;

export default function Dashboard() {
  const user = getStoredUser();

  return (
    <PageContainer title="工作台" subTitle="欢迎使用 CP OA 办公自动化系统">
      <Title level={4} style={{ marginTop: 0 }}>
        你好，{user?.realName || user?.username || '同事'} 👋
      </Title>
      <Paragraph type="secondary">
        这里是系统概览。左侧菜单已接入用户管理示例，可直接在该页面完成增删改查。
      </Paragraph>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} sm={12} lg={8}>
          <Card>
            <Statistic
              title="待办审批"
              value={12}
              prefix={<AuditOutlined />}
              valueStyle={{ color: '#1677ff' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={8}>
          <Card>
            <Statistic
              title="我发起的流程"
              value={8}
              prefix={<FileTextOutlined />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={8}>
          <Card>
            <Statistic
              title="系统用户"
              value={1}
              prefix={<TeamOutlined />}
              valueStyle={{ color: '#faad14' }}
            />
          </Card>
        </Col>
      </Row>

      <Card title="快速开始" style={{ marginTop: 16 }}>
        <Paragraph>
          当前账号：<Text strong>{user?.username ?? '-'}</Text>
          {user?.role ? (
            <Text type="secondary">（{user.role === 'admin' ? '管理员' : '普通用户'}）</Text>
          ) : null}
        </Paragraph>
        <Paragraph type="secondary" style={{ marginBottom: 0 }}>
          接口通过 Vite 代理转发到 <Text code>http://localhost:8080</Text>，
          后端登录态使用 Shiro + JWT，Token 缓存在 Redis 中。
        </Paragraph>
      </Card>
    </PageContainer>
  );
}
