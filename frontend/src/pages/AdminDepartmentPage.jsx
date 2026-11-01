import { useCallback, useEffect, useState } from 'react';
import { Alert, Button, Form, Input, Modal, Popconfirm, Space, Table, message } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import {
  createDepartment,
  deleteDepartment,
  fetchDepartments,
  updateDepartment,
} from '../api/departmentApi';
import PageTitle from '../components/PageTitle';
import { formatDateTime } from '../utils/formatDate';

const NAME_RULES = [
  { required: true, whitespace: true, message: '부서명을 입력하세요.' },
  { max: 100, message: '100자 이하로 입력하세요.' },
];

const AdminDepartmentPage = () => {
  const [form] = Form.useForm();
  const [departments, setDepartments] = useState([]);
  const [loading, setLoading] = useState(false);
  const [editing, setEditing] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setDepartments(await fetchDepartments());
    } catch (err) {
      message.error(err.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const openModal = (department) => {
    setEditing(department ?? {});
    form.setFieldsValue({ name: department?.name ?? '' });
  };

  const closeModal = () => {
    setEditing(null);
    form.resetFields();
  };

  const handleSubmit = async ({ name }) => {
    setSubmitting(true);
    try {
      if (editing.deptCode) {
        await updateDepartment(editing.deptCode, name.trim());
        message.success('부서명을 수정했습니다.');
      } else {
        await createDepartment(name.trim());
        message.success('부서를 추가했습니다.');
      }
      closeModal();
      await load();
    } catch (err) {
      message.error(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (department) => {
    try {
      await deleteDepartment(department.deptCode);
      message.success('부서를 삭제했습니다.');
      await load();
    } catch (err) {
      message.error(err.message);
    }
  };

  const columns = [
    { title: '부서 코드', dataIndex: 'deptCode', width: 120 },
    { title: '부서명', dataIndex: 'name' },
    { title: '소속 사원 수', dataIndex: 'employeeCount', width: 130, align: 'right' },
    {
      title: '등록일',
      dataIndex: 'createdAt',
      width: 180,
      render: (value) => formatDateTime(value),
    },
    {
      title: '관리',
      width: 140,
      render: (_, record) => (
        <Space>
          <Button size="small" onClick={() => openModal(record)}>수정</Button>
          <Popconfirm
            title="부서를 삭제할까요?"
            description="소속 사원이 있으면 삭제할 수 없습니다."
            okText="삭제"
            cancelText="취소"
            okButtonProps={{ danger: true }}
            onConfirm={() => handleDelete(record)}
          >
            <Button size="small" danger>삭제</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <Space orientation="vertical" size="large" style={{ width: '100%' }}>
      <PageTitle
        title="부서 관리"
        description="부서를 추가하고 이름을 수정하거나 삭제합니다."
        extra={<Button type="primary" icon={<PlusOutlined />} onClick={() => openModal()}>부서 추가</Button>}
      />
      <Alert
        type="info"
        showIcon
        title="소속 사원이 있는 부서는 삭제할 수 없습니다. 사원을 다른 부서로 옮긴 뒤 삭제하세요."
      />
      <Table
        rowKey="deptCode"
        columns={columns}
        dataSource={departments}
        loading={loading}
        pagination={false}
      />
      <Modal
        title={editing?.deptCode ? '부서명 수정' : '부서 추가'}
        open={editing !== null}
        okText="저장"
        cancelText="취소"
        confirmLoading={submitting}
        onOk={() => form.submit()}
        onCancel={closeModal}
        destroyOnHidden
      >
        <Form form={form} layout="vertical" onFinish={handleSubmit}>
          <Form.Item name="name" label="부서명" rules={NAME_RULES}>
            <Input autoFocus maxLength={100} />
          </Form.Item>
        </Form>
      </Modal>
    </Space>
  );
};

export default AdminDepartmentPage;
