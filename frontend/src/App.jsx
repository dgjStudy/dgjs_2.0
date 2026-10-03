import React, { useState } from 'react';
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
  Badge,
  message,
} from 'antd';
import {
  DatabaseOutlined,
  UploadOutlined,
  FileExcelOutlined,
  ArrowLeftOutlined,
  EyeOutlined,
  PlusOutlined,
  CheckCircleOutlined,
} from '@ant-design/icons';

const { Header, Content, Sider } = Layout;
const { Title, Text, Paragraph } = Typography;

// Mock Datasets
const MOCK_DATASETS = [
  {
    id: 'ds-001',
    name: '2026년 3분기 공장별 에너지 사용량',
    category: '에너지/환경',
    rowCount: 1250,
    columnCount: 8,
    ownerId: 'user_admin',
    orgId: 'org_hq',
    createdAt: '2026-10-01 14:20:00',
    description: '전국 5개 생산 공장의 시간대별 전력 및 가스 소비 데이터',
    columns: ['공장코드', '측정일시', '전력사용량(kWh)', '가스사용량(m³)', '피크전력', '가동률(%)', '담당자', '상태'],
    sampleData: [
      { key: '1', 공장코드: 'FACT-SEOUL-01', 측정일시: '2026-10-01 09:00', '전력사용량(kWh)': 420.5, '가스사용량(m³)': 112.0, 피크전력: 450, '가동률(%)': 92, 담당자: '김철수', 상태: '정상' },
      { key: '2', 공장코드: 'FACT-BUSAN-02', 측정일시: '2026-10-01 09:00', '전력사용량(kWh)': 580.2, '가스사용량(m³)': 205.4, 피크전력: 600, '가동률(%)': 88, 담당자: '이영희', 상태: '주의' },
      { key: '3', 공장코드: 'FACT-INCHEON-01', 측정일시: '2026-10-01 09:00', '전력사용량(kWh)': 310.8, '가스사용량(m³)': 95.1, 피크전력: 350, '가동률(%)': 95, 담당자: '박민수', 상태: '정상' },
      { key: '4', 공장코드: 'FACT-DAEJEON-03', 측정일시: '2026-10-01 09:00', '전력사용량(kWh)': 290.0, '가스사용량(m³)': 80.3, 피크전력: 300, '가동률(%)': 75, 담당자: '정수진', 상태: '점검필요' },
    ],
  },
  {
    id: 'ds-002',
    name: '전사 부서별 설비 유지보수 이력',
    category: '설비관리',
    rowCount: 840,
    columnCount: 6,
    ownerId: 'user_maint',
    orgId: 'org_ops',
    createdAt: '2026-09-28 10:15:00',
    description: '생산라인 주요 설비의 정기 점검 및 수리 이력 메타데이터',
    columns: ['설비ID', '설비명', '점검일자', '점검유형', '비용(원)', '조치결과'],
    sampleData: [
      { key: '1', 설비ID: 'EQ-CNC-09', 설비명: '고속 CNC 가공기 #9', 점검일자: '2026-09-25', 점검유형: '정기점검', '비용(원)': 150000, 조치결과: '부품 교체완료' },
      { key: '2', 설비ID: 'EQ-ROBOT-02', 설비명: '다축 용접 로봇 #2', 점검일자: '2026-09-26', 점검유형: '긴급수리', '비용(원)': 450000, 조치결과: '센서 재설정' },
    ],
  },
  {
    id: 'ds-003',
    name: '원자재 공급망 입고 검사 데이터',
    category: '품질관리',
    rowCount: 3400,
    columnCount: 7,
    ownerId: 'user_qa',
    orgId: 'org_qa',
    createdAt: '2026-09-20 16:45:00',
    description: '협력업체별 입고 원자재 불량률 및 품질 검사 수치',
    columns: ['LOT번호', '공급사', '품목명', '입고수량', '합격수량', '불량률(%)', '검사자'],
    sampleData: [
      { key: '1', LOT번호: 'LOT-20260920-A', 공급사: '(주)한국알루미늄', 품목명: 'AL-6061 판재', 입고수량: 5000, 합격수량: 4980, '불량률(%)': 0.4, 검사자: '최동현' },
    ],
  },
];

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
      render: (cat) => <Tag color="blue">{cat}</Tag>,
    },
    {
      title: '행 수',
      dataIndex: 'rowCount',
      key: 'rowCount',
      render: (val) => `${val.toLocaleString()} 행`,
    },
    {
      title: '열 수',
      dataIndex: 'columnCount',
      key: 'columnCount',
      render: (val) => `${val} 개`,
    },
    {
      title: '등록일시',
      dataIndex: 'createdAt',
      key: 'createdAt',
    },
    {
      title: '소속 / 소유자',
      key: 'owner',
      render: (_, record) => `${record.orgId} / ${record.ownerId}`,
    },
    {
      title: '작업',
      key: 'action',
      render: (_, record) => (
        <Button
          type="primary"
          ghost
          size="small"
          icon={<EyeOutlined />}
          onClick={() => navigate(`/data/${record.id}`)}
        >
          상세 조회
        </Button>
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
        <Table columns={columns} dataSource={MOCK_DATASETS} rowKey="id" pagination={{ pageSize: 10 }} />
      </Card>
    </Space>
  );
};

// 2. /data/:id - Dataset Detail Grid View
const DataDetailPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();

  const dataset = MOCK_DATASETS.find((d) => d.id === id) || MOCK_DATASETS[0];

  const gridColumns = dataset.columns.map((col) => ({
    title: col,
    dataIndex: col,
    key: col,
  }));

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Breadcrumb
        items={[
          { title: <a onClick={() => navigate('/data')}>데이터셋 목록</a> },
          { title: dataset.name },
        ]}
      />

      <Card>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 16 }}>
          <div>
            <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/data')} style={{ marginBottom: 12 }}>
              목록으로 돌아가기
            </Button>
            <Title level={3} style={{ margin: 0 }}>{dataset.name}</Title>
            <Text type="secondary">{dataset.description}</Text>
          </div>
          <Space>
            <Tag color="green">메타데이터 정상</Tag>
            <Tag color="purple">ID: {dataset.id}</Tag>
          </Space>
        </div>

        <Descriptions bordered size="small" column={{ xs: 1, sm: 2, md: 4 }}>
          <Descriptions.Item label="카테고리">{dataset.category}</Descriptions.Item>
          <Descriptions.Item label="총 레코드 수">{dataset.rowCount.toLocaleString()} 행</Descriptions.Item>
          <Descriptions.Item label="컬럼 수">{dataset.columnCount} 개</Descriptions.Item>
          <Descriptions.Item label="등록일시">{dataset.createdAt}</Descriptions.Item>
          <Descriptions.Item label="소속 조직(org_id)">{dataset.orgId}</Descriptions.Item>
          <Descriptions.Item label="소유자(owner_id)">{dataset.ownerId}</Descriptions.Item>
        </Descriptions>
      </Card>

      <Card title={<Space><FileExcelOutlined /><span>데이터 상세 그리드 뷰</span></Space>}>
        <Table
          columns={gridColumns}
          dataSource={dataset.sampleData}
          rowKey="key"
          bordered
          pagination={{ pageSize: 5 }}
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

  const handleSubmit = (values) => {
    message.success('데이터셋이 성공적으로 적재 정의되었습니다!');
    console.log('업로드 파라미터:', values, fileList);
    navigate('/data');
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
              <Button type="primary" htmlType="submit" icon={<CheckCircleOutlined />} size="large">
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
