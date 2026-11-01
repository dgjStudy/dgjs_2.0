import { useCallback, useEffect, useState } from 'react';
import { Alert, Button, Form, Input, Modal, Popconfirm, Space, Table, message } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { createProject, deleteProject, fetchProjects, updateProject } from '../api/projectApi';
import PageTitle from '../components/PageTitle';
import ProjectMembersModal from '../components/ProjectMembersModal';
import { formatDateTime } from '../utils/formatDate';

const NAME_RULES = [
  { required: true, whitespace: true, message: '프로젝트명을 입력하세요.' },
  { max: 100, message: '100자 이하로 입력하세요.' },
];

const AdminProjectPage = () => {
  const [form] = Form.useForm();
  const [projects, setProjects] = useState([]);
  const [loading, setLoading] = useState(false);
  const [editing, setEditing] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [membersTarget, setMembersTarget] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setProjects(await fetchProjects());
    } catch (err) {
      message.error(err.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const openModal = (project) => {
    setEditing(project ?? {});
    form.setFieldsValue({ name: project?.name ?? '' });
  };

  const closeModal = () => {
    setEditing(null);
    form.resetFields();
  };

  const handleSubmit = async ({ name }) => {
    setSubmitting(true);
    try {
      if (editing.projectCode) {
        await updateProject(editing.projectCode, name.trim());
        message.success('프로젝트명을 수정했습니다.');
      } else {
        await createProject(name.trim());
        message.success('프로젝트를 추가했습니다.');
      }
      closeModal();
      await load();
    } catch (err) {
      message.error(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (project) => {
    try {
      await deleteProject(project.projectCode);
      message.success('프로젝트를 삭제했습니다.');
      await load();
    } catch (err) {
      message.error(err.message);
    }
  };

  const columns = [
    { title: '프로젝트 코드', dataIndex: 'projectCode', width: 130 },
    { title: '프로젝트명', dataIndex: 'name' },
    { title: '참여자 수', dataIndex: 'memberCount', width: 110, align: 'right' },
    {
      title: '등록일',
      dataIndex: 'createdAt',
      width: 180,
      render: (value) => formatDateTime(value),
    },
    {
      title: '관리',
      width: 230,
      render: (_, record) => (
        <Space>
          <Button size="small" onClick={() => setMembersTarget(record)}>참여자</Button>
          <Button size="small" onClick={() => openModal(record)}>수정</Button>
          <Popconfirm
            title="프로젝트를 삭제할까요?"
            description="연결된 데이터셋은 프로젝트 미지정으로 바뀌고 참여자 연결은 삭제됩니다."
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
        title="프로젝트 관리"
        description="프로젝트를 관리하고 참여자를 지정합니다."
        extra={<Button type="primary" icon={<PlusOutlined />} onClick={() => openModal()}>프로젝트 추가</Button>}
      />
      <Alert
        type="info"
        showIcon
        title="프로젝트를 삭제하면 연결된 데이터셋은 프로젝트 미지정으로 바뀝니다. 참여자는 활성 사원만 추가할 수 있습니다."
      />
      <Table
        rowKey="projectCode"
        columns={columns}
        dataSource={projects}
        loading={loading}
        pagination={false}
      />
      <Modal
        title={editing?.projectCode ? '프로젝트명 수정' : '프로젝트 추가'}
        open={editing !== null}
        okText="저장"
        cancelText="취소"
        confirmLoading={submitting}
        onOk={() => form.submit()}
        onCancel={closeModal}
        destroyOnHidden
      >
        <Form form={form} layout="vertical" onFinish={handleSubmit}>
          <Form.Item name="name" label="프로젝트명" rules={NAME_RULES}>
            <Input autoFocus maxLength={100} />
          </Form.Item>
        </Form>
      </Modal>
      <ProjectMembersModal
        project={membersTarget}
        onClose={() => setMembersTarget(null)}
        onChanged={load}
      />
    </Space>
  );
};

export default AdminProjectPage;
