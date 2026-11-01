import { PlusOutlined } from '@ant-design/icons';
import {
  ModalForm,
  PageContainer,
  ProFormSelect,
  ProFormText,
  ProTable,
  type ActionType,
  type ProColumns,
} from '@ant-design/pro-components';
import { Button, Popconfirm, Tag, message } from 'antd';
import { useRef, useState } from 'react';
import { createUser, deleteUser, pageUsers, updateUser } from '@/services/user';
import type { UserFormValues, UserInfo } from '@/types';

const roleMap: Record<string, { text: string; color: string }> = {
  admin: { text: '管理员', color: 'blue' },
  user: { text: '普通用户', color: 'default' },
};

export default function UserPage() {
  const actionRef = useRef<ActionType | undefined>(undefined);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<UserInfo | null>(null);

  const openCreate = () => {
    setEditing(null);
    setModalOpen(true);
  };

  const openEdit = (record: UserInfo) => {
    setEditing(record);
    setModalOpen(true);
  };

  const handleDelete = async (id: number) => {
    try {
      await deleteUser(id);
      message.success('用户已删除');
      actionRef.current?.reload();
    } catch {
      // 错误提示由请求拦截器统一处理
    }
  };

  const columns: ProColumns<UserInfo>[] = [
    { title: 'ID', dataIndex: 'id', width: 72, search: false },
    { title: '用户名', dataIndex: 'username', search: false },
    { title: '姓名', dataIndex: 'realName', width: 140 },
    {
      title: '角色',
      dataIndex: 'role',
      width: 110,
      search: false,
      render: (_, record) => {
        const meta = roleMap[record.role] ?? roleMap.user;
        return <Tag color={meta.color}>{meta.text}</Tag>;
      },
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 90,
      search: false,
      render: (_, record) =>
        record.status === 1 ? (
          <Tag color="success">启用</Tag>
        ) : (
          <Tag color="error">禁用</Tag>
        ),
    },
    { title: '邮箱', dataIndex: 'email', search: false },
    { title: '手机号', dataIndex: 'phone', search: false },
    { title: '创建时间', dataIndex: 'createTime', width: 170, search: false },
    {
      title: '操作',
      valueType: 'option',
      width: 140,
      render: (_, record) => [
        <a key="edit" onClick={() => openEdit(record)}>
          编辑
        </a>,
        <Popconfirm
          key="delete"
          title="确认删除该用户？"
          okText="删除"
          cancelText="取消"
          okButtonProps={{ danger: true }}
          disabled={record.id === 1}
          onConfirm={() => handleDelete(record.id)}
        >
          <a style={{ color: record.id === 1 ? undefined : '#ff4d4f' }}>删除</a>
        </Popconfirm>,
      ],
    },
  ];

  return (
    <PageContainer title="用户管理" subTitle="系统账号的增删改查">
      <ProTable<UserInfo>
        actionRef={actionRef}
        rowKey="id"
        columns={columns}
        search={{ labelWidth: 'auto' }}
        pagination={{ defaultPageSize: 10, showSizeChanger: true }}
        request={async (params) => {
          const res = await pageUsers({
            current: params.current ?? 1,
            pageSize: params.pageSize ?? 10,
            keyword: params.realName as string | undefined,
          });
          return { data: res.records, total: res.total, success: true };
        }}
        toolBarRender={() => [
          <Button
            key="create"
            type="primary"
            icon={<PlusOutlined />}
            onClick={openCreate}
          >
            新建用户
          </Button>,
        ]}
      />

      <ModalForm<UserFormValues>
        title={editing ? '编辑用户' : '新建用户'}
        open={modalOpen}
        modalProps={{
          destroyOnHidden: true,
          maskClosable: false,
          onCancel: () => setModalOpen(false),
        }}
        initialValues={editing ?? { role: 'user', status: 1 }}
        onOpenChange={setModalOpen}
        onFinish={async (values) => {
          try {
            if (editing) {
              await updateUser(editing.id, {
                realName: values.realName,
                email: values.email,
                phone: values.phone,
                role: values.role,
                status: values.status,
                password: values.password || undefined,
              });
              message.success('用户已更新');
            } else {
              await createUser(values);
              message.success('用户已创建');
            }
            actionRef.current?.reload();
            return true;
          } catch {
            return false;
          }
        }}
      >
        <ProFormText
          name="username"
          label="用户名"
          disabled={!!editing}
          rules={[{ required: !editing, message: '请输入用户名' }]}
        />
        <ProFormText.Password
          name="password"
          label="密码"
          placeholder={editing ? '留空则不修改' : '至少 6 位'}
          rules={editing ? [{ min: 6, message: '密码至少 6 位' }] : [{ required: true, min: 6, message: '请输入至少 6 位密码' }]}
        />
        <ProFormText
          name="realName"
          label="姓名"
          rules={[{ required: true, message: '请输入姓名' }]}
        />
        <ProFormText
          name="email"
          label="邮箱"
          rules={[{ type: 'email', message: '邮箱格式不正确' }]}
        />
        <ProFormText name="phone" label="手机号" />
        <ProFormSelect
          name="role"
          label="角色"
          options={[
            { label: '管理员', value: 'admin' },
            { label: '普通用户', value: 'user' },
          ]}
          rules={[{ required: true, message: '请选择角色' }]}
        />
        <ProFormSelect
          name="status"
          label="状态"
          options={[
            { label: '启用', value: 1 },
            { label: '禁用', value: 0 },
          ]}
          rules={[{ required: true, message: '请选择状态' }]}
        />
      </ModalForm>
    </PageContainer>
  );
}
