import React, { useState, useEffect } from 'react';
import { BrowserRouter, Routes, Route, useNavigate, useParams, useLocation } from 'react-router-dom';
import {
  Layout,
  Menu,
  Table,
  Button,
  Card,
  Tag,
  Space,
  Upload,
  Form,
  Input,
  Select,
  Typography,
  Breadcrumb,
  Descriptions,
  message,
  Spin,
  Popconfirm,
} from 'antd';
import {
  DatabaseOutlined,
  UploadOutlined,
  FileExcelOutlined,
  ArrowLeftOutlined,
  EyeOutlined,
  PlusOutlined,
  CheckCircleOutlined,
  DeleteOutlined,
} from '@ant-design/icons';
import { fetchDatasets, fetchDatasetDetail, uploadDatasetFile, deleteDataset } from './api/datasetApi';

const { Header, Content } = Layout;
const { Title, Text } = Typography;

// App Layout Wrapper
const MainLayout = ({ children }) => {
  const navigate = useNavigate();
  const location = useLocation();

  const getSelectedKey = () => {
    if (location.pathname.startsWith('/data')) return '/data';
    if (location.pathname === '/upload') return '/upload';
    return '/data';
  };

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header style={{ display: 'flex', alignItems: 'center', background: '#001529', padding: '0 24px' }}>
        <div style={{ color: '#fff', fontSize: '18px', fontWeight: 'bold', marginRight: '40px', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <DatabaseOutlined style={{ color: '#1890ff', fontSize: '22px' }} />
          <span>DGJS 2.0 Platform</span>
        </div>
        <Menu
          theme="dark"
          mode="horizontal"
          selectedKeys={[getSelectedKey()]}
          style={{ flex: 1, minWidth: 0 }}
          items={[
            { key: '/data', icon: <DatabaseOutlined />, label: '전체 데이터셋 목록', onClick: () => navigate('/data') },
            { key: '/upload', icon: <UploadOutlined />, label: '데이터 적재 및 스키마 정의', onClick: () => navigate('/upload') },
          ]}
        />
      </Header>
      <Content style={{ padding: '24px 48px', background: '#f0f2f5' }}>
        {children}
      </Content>
    </Layout>
  );
};

// 1. /data - Dataset List View
const DataListPage = () => {
  const navigate = useNavigate();
  const [datasets, setDatasets] = useState([]);
  const [loading, setLoading] = useState(true);

  const loadData = async () => {
    setLoading(true);
    try {
      const data = await fetchDatasets();
      setDatasets(data);
    } catch (err) {
      message.error(err.message || '데이터셋 목록 조회에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleDelete = async (id) => {
    try {
      await deleteDataset(id);
      message.success('데이터셋이 삭제되었습니다.');
      loadData();
    } catch (err) {
      message.error(err.message || '삭제 중 오류가 발생했습니다.');
    }
  };

  const columns = [
    {
      title: '데이터셋명',
      dataIndex: 'name',
      key: 'name',
      render: (text, record) => (
        <a onClick={() => navigate(`/data/${record.id}`)} style={{ fontWeight: 600 }}>
          <FileExcelOutlined style={{ marginRight: 8, color: '#1890ff' }} />
          {text}
        </a>
      ),
    },
    {
      title: '분류',
      dataIndex: 'category',
      key: 'category',
      render: (cat) => <Tag color="blue">{cat || '기타'}</Tag>,
    },
    {
      title: '행 수',
      dataIndex: 'rowCount',
      key: 'rowCount',
      render: (val) => `${(val || 0).toLocaleString()} 행`,
    },
    {
      title: '열 수',
      dataIndex: 'columnCount',
      key: 'columnCount',
      render: (val) => `${val || 0} 개`,
    },
    {
      title: '등록일시',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (val) => (val ? new Date(val).toLocaleString() : '-'),
    },
    {
      title: '소속 / 소유자',
      key: 'owner',
      render: (_, record) => `${record.orgId || '-'} / ${record.ownerId || '-'}`,
    },
    {
      title: '작업',
      key: 'action',
      render: (_, record) => (
        <Space>
          <Button
            type="primary"
            ghost
            size="small"
            icon={<EyeOutlined />}
            onClick={() => navigate(`/data/${record.id}`)}
          >
            상세 조회
          </Button>
          <Popconfirm
            title="데이터셋 삭제"
            description="이 데이터셋과 적재된 데이터를 삭제하시겠습니까?"
            onConfirm={() => handleDelete(record.id)}
            okText="삭제"
            cancelText="취소"
          >
            <Button type="text" danger size="small" icon={<DeleteOutlined />}>
              삭제
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Breadcrumb items={[{ title: '홈' }, { title: '데이터셋 목록' }]} />
      <Card
        title={
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <Title level={4} style={{ margin: 0 }}>전체 데이터셋 목록</Title>
              <Text type="secondary">적재된 데이터 플랫폼 메타데이터 및 시각화용 데이터셋입니다.</Text>
            </div>
            <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/upload')}>
              신규 데이터 적재
            </Button>
          </div>
        }
      >
        <Table
          columns={columns}
          dataSource={datasets}
          rowKey="id"
          loading={loading}
          pagination={{ pageSize: 10 }}
        />
      </Card>
    </Space>
  );
};

// 2. /data/:id - Dataset Detail Grid View
const DataDetailPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [detail, setDetail] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const loadDetail = async () => {
      setLoading(true);
      try {
        const data = await fetchDatasetDetail(id);
        setDetail(data);
      } catch (err) {
        message.error(err.message || '상세 정보 조회 실패');
      } finally {
        setLoading(false);
      }
    };
    loadDetail();
  }, [id]);

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '100px 0' }}>
        <Spin size="large" tip="데이터셋 정보를 불러오는 중..." />
      </div>
    );
  }

  if (!detail) {
    return (
      <Card>
        <Typography.Text type="danger">존재하지 않거나 삭제된 데이터셋입니다.</Typography.Text>
        <br />
        <Button onClick={() => navigate('/data')} style={{ marginTop: 16 }}>목록으로 돌아가기</Button>
      </Card>
    );
  }

  const gridColumns = (detail.columns || []).map((col) => ({
    title: col.title,
    dataIndex: col.key,
    key: col.key,
  }));

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Breadcrumb
        items={[
          { title: <a onClick={() => navigate('/data')}>데이터셋 목록</a> },
          { title: detail.name },
        ]}
      />

      <Card>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 16 }}>
          <div>
            <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/data')} style={{ marginBottom: 12 }}>
              목록으로 돌아가기
            </Button>
            <Title level={3} style={{ margin: 0 }}>{detail.name}</Title>
            <Text type="secondary">{detail.description || '등록된 설명이 없습니다.'}</Text>
          </div>
          <Space>
            <Tag color="green">메타데이터 정상</Tag>
            <Tag color="purple">ID: {detail.id}</Tag>
          </Space>
        </div>

        <Descriptions bordered size="small" column={{ xs: 1, sm: 2, md: 4 }}>
          <Descriptions.Item label="카테고리">{detail.category}</Descriptions.Item>
          <Descriptions.Item label="총 레코드 수">{detail.rows ? detail.rows.length.toLocaleString() : 0} 행</Descriptions.Item>
          <Descriptions.Item label="컬럼 수">{detail.columns ? detail.columns.length : 0} 개</Descriptions.Item>
          <Descriptions.Item label="등록일시">{detail.createdAt ? new Date(detail.createdAt).toLocaleString() : '-'}</Descriptions.Item>
          <Descriptions.Item label="소속 조직(org_id)">{detail.orgId}</Descriptions.Item>
          <Descriptions.Item label="소유자(owner_id)">{detail.ownerId}</Descriptions.Item>
        </Descriptions>
      </Card>

      <Card title={<Space><FileExcelOutlined /><span>데이터 상세 그리드 뷰</span></Space>}>
        <Table
          columns={gridColumns}
          dataSource={detail.rows || []}
          rowKey="key"
          bordered
          pagination={{ pageSize: 10 }}
        />
      </Card>
    </Space>
  );
};

// 3. /upload - Data Upload & Schema Definition Page
const DataUploadPage = () => {
  const navigate = useNavigate();
  const [form] = Form.useForm();
  const [fileList, setFileList] = useState([]);
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (values) => {
    if (fileList.length === 0) {
      message.error('엑셀 또는 CSV 데이터 파일을 첨부해 주세요.');
      return;
    }

    setSubmitting(true);
    try {
      const formData = new FormData();
      formData.append('file', fileList[0].originFileObj || fileList[0]);
      formData.append('name', values.name);
      formData.append('description', values.description || '');
      formData.append('category', values.category || '기타');
      formData.append('ownerId', values.ownerId || 'user_admin');
      formData.append('orgId', values.orgId || 'org_hq');

      const res = await uploadDatasetFile(formData);
      message.success(res.message || '데이터셋이 성공적으로 적재되었습니다!');
      navigate('/data');
    } catch (err) {
      message.error(err.message || '업로드 처리 중 오류가 발생했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Breadcrumb items={[{ title: '홈' }, { title: '데이터 적재 및 스키마 정의' }]} />

      <Card title={<Title level={4} style={{ margin: 0 }}>신규 데이터 적재 및 스키마 정의</Title>}>
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
          initialValues={{ category: '에너지/환경', ownerId: 'user_admin', orgId: 'org_hq' }}
        >
          <Form.Item
            name="name"
            label="데이터셋 명칭"
            rules={[{ required: true, message: '데이터셋 이름을 입력하세요.' }]}
          >
            <Input placeholder="예: 2026년 3분기 공장별 에너지 사용량" />
          </Form.Item>

          <Form.Item name="description" label="데이터셋 설명">
            <Input.TextArea rows={3} placeholder="데이터셋의 목적 및 수집 경로 설명" />
          </Form.Item>

          <Space size="large" style={{ display: 'flex' }}>
            <Form.Item name="category" label="분류 카테고리" style={{ width: 240 }}>
              <Select
                options={[
                  { value: '에너지/환경', label: '에너지/환경' },
                  { value: '설비관리', label: '설비관리' },
                  { value: '품질관리', label: '품질관리' },
                  { value: '생산실적', label: '생산실적' },
                ]}
              />
            </Form.Item>

            <Form.Item name="orgId" label="소속 조직 (org_id)" style={{ width: 200 }}>
              <Input disabled />
            </Form.Item>

            <Form.Item name="ownerId" label="등록자 (owner_id)" style={{ width: 200 }}>
              <Input disabled />
            </Form.Item>
          </Space>

          <Form.Item label="엑셀/CSV 데이터 파일 첨부" required>
            <Upload.Dragger
              beforeUpload={() => false}
              fileList={fileList}
              onChange={({ fileList }) => setFileList(fileList)}
              maxCount={1}
              accept=".xlsx,.xls,.csv"
            >
              <p className="ant-upload-drag-icon">
                <FileExcelOutlined style={{ fontSize: 36, color: '#1890ff' }} />
              </p>
              <p className="ant-upload-text">클릭하거나 엑셀/CSV 파일을 이곳으로 드래그하세요.</p>
              <p className="ant-upload-hint">1,000행 이상의 데이터도 메타데이터 파싱 후 비동기 적재 처리됩니다.</p>
            </Upload.Dragger>
          </Form.Item>

          <Form.Item style={{ marginTop: 24 }}>
            <Space>
              <Button type="primary" htmlType="submit" icon={<CheckCircleOutlined />} size="large" loading={submitting}>
                적재 및 스키마 저장
              </Button>
              <Button size="large" onClick={() => navigate('/data')}>
                취소
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>
    </Space>
  );
};

export default function App() {
  return (
    <BrowserRouter>
      <MainLayout>
        <Routes>
          <Route path="/" element={<DataListPage />} />
          <Route path="/data" element={<DataListPage />} />
          <Route path="/data/:id" element={<DataDetailPage />} />
          <Route path="/upload" element={<DataUploadPage />} />
        </Routes>
      </MainLayout>
    </BrowserRouter>
  );
}
