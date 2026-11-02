import { SyncOutlined } from '@ant-design/icons';
import { PageContainer } from '@ant-design/pro-components';
import {
  Button,
  Card,
  Col,
  Empty,
  message,
  Popconfirm,
  Row,
  Table,
  Tag,
  Tree,
  Typography,
} from 'antd';
import type { TableColumnsType } from 'antd';
import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  getDepartmentUsers,
  getOrgTree,
  syncOrg,
  type DepartmentNode,
} from '@/services/org';
import type { UserInfo } from '@/types';
import { getStoredUser } from '@/utils/auth';

interface OrgTreeNode {
  key: number;
  title: string;
  children?: OrgTreeNode[];
}

const { Text } = Typography;

function toTreeData(nodes: DepartmentNode[]): OrgTreeNode[] {
  return nodes.map((node) => ({
    key: node.id,
    title: `${node.name}${node.memberCount ? ` (${node.memberCount})` : ''}`,
    children: node.children?.length ? toTreeData(node.children) : undefined,
  }));
}

function findDepartment(nodes: DepartmentNode[], id: number | null): DepartmentNode | null {
  if (id == null) return null;
  for (const node of nodes) {
    if (node.id === id) return node;
    const child = findDepartment(node.children ?? [], id);
    if (child) return child;
  }
  return null;
}

export default function OrgPage() {
  const [tree, setTree] = useState<DepartmentNode[]>([]);
  const [treeLoading, setTreeLoading] = useState(false);
  const [syncing, setSyncing] = useState(false);
  const [selectedDept, setSelectedDept] = useState<number | null>(null);
  const [users, setUsers] = useState<UserInfo[]>([]);
  const [usersLoading, setUsersLoading] = useState(false);

  const isAdmin = getStoredUser()?.role === 'admin';

  const loadTree = useCallback(async () => {
    setTreeLoading(true);
    try {
      const data = await getOrgTree();
      setTree(data);
      setSelectedDept((current) => current ?? data[0]?.id ?? null);
    } catch {
      // 错误提示由请求拦截器处理
    } finally {
      setTreeLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadTree();
  }, [loadTree]);

  useEffect(() => {
    if (selectedDept == null) {
      setUsers([]);
      return;
    }
    setUsersLoading(true);
    getDepartmentUsers(selectedDept)
      .then(setUsers)
      .catch(() => setUsers([]))
      .finally(() => setUsersLoading(false));
  }, [selectedDept]);

  const handleSync = async () => {
    setSyncing(true);
    try {
      const result = await syncOrg();
      message.success(
        `同步完成：部门 ${result.departments} 个（清理 ${result.departmentsRemoved} 个），` +
          `新增用户 ${result.usersCreated} 个，更新 ${result.usersUpdated} 个，` +
          `禁用 ${result.usersDisabled} 个，关系 ${result.relations} 条`,
      );
      await loadTree();
      if (selectedDept != null) {
        setUsers(await getDepartmentUsers(selectedDept));
      }
    } catch {
      // 错误提示由请求拦截器处理
    } finally {
      setSyncing(false);
    }
  };

  const treeData = useMemo(() => toTreeData(tree), [tree]);
  const selectedName = useMemo(
    () => findDepartment(tree, selectedDept)?.name,
    [tree, selectedDept],
  );

  const columns: TableColumnsType<UserInfo> = [
    { title: '用户名', dataIndex: 'username', width: 140 },
    { title: '姓名', dataIndex: 'realName', width: 120 },
    { title: '工号', dataIndex: 'employeeNo', width: 110, render: (value) => value || '-' },
    { title: '邮箱', dataIndex: 'email', render: (value) => value || '-' },
    { title: '手机号', dataIndex: 'phone', width: 130, render: (value) => value || '-' },
    {
      title: '角色',
      dataIndex: 'role',
      width: 100,
      render: (value: string) =>
        value === 'admin' ? <Tag color="blue">管理员</Tag> : <Tag>普通用户</Tag>,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 90,
      render: (value: number) =>
        value === 1 ? <Tag color="success">启用</Tag> : <Tag color="error">禁用</Tag>,
    },
  ];

  return (
    <PageContainer
      title="组织架构"
      subTitle="同步自飞书通讯录"
      extra={
        isAdmin ? (
          <Popconfirm
            title="确认立即从飞书同步组织架构？"
            description="将拉取全员部门与成员，数据量较大时可能耗时较长"
            okText="开始同步"
            cancelText="取消"
            onConfirm={handleSync}
          >
            <Button type="primary" icon={<SyncOutlined />} loading={syncing}>
              立即同步
            </Button>
          </Popconfirm>
        ) : undefined
      }
    >
      <Row gutter={[16, 16]}>
        <Col xs={24} lg={8}>
          <Card title="部门" loading={treeLoading} styles={{ body: { minHeight: 420 } }}>
            {tree.length === 0 && !treeLoading ? (
              <Empty description="暂无部门数据，请先执行同步" />
            ) : (
              <Tree
                treeData={treeData}
                defaultExpandAll
                selectedKeys={selectedDept == null ? [] : [selectedDept]}
                onSelect={(keys) =>
                  setSelectedDept(keys.length > 0 ? Number(keys[0]) : null)
                }
              />
            )}
          </Card>
        </Col>
        <Col xs={24} lg={16}>
          <Card
            title={
              selectedName ? (
                <span>
                  成员 <Text type="secondary">· {selectedName}</Text>
                </span>
              ) : (
                '成员'
              )
            }
          >
            <Table<UserInfo>
              rowKey="id"
              size="middle"
              loading={usersLoading}
              columns={columns}
              dataSource={users}
              pagination={false}
              scroll={{ x: 900 }}
              locale={{ emptyText: '当前部门暂无成员' }}
            />
          </Card>
        </Col>
      </Row>
    </PageContainer>
  );
}
