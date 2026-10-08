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
  Steps,
  Progress,
  Alert,
  Divider,
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
import {
  fetchDatasets,
  fetchDatasetDetail,
  uploadDatasetFile,
  deleteDataset,
  previewDatasetFile,
  uploadDatasetAsync,
  fetchTaskProgress,
} from './api/datasetApi';


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

// 3. /upload - Data Upload & Dynamic Schema Definition & Async Progress Page
const DataUploadPage = () => {
  const navigate = useNavigate();
  const [form] = Form.useForm();
  const [currentStep, setCurrentStep] = useState(0);
  const [fileList, setFileList] = useState([]);
  const [parsing, setParsing] = useState(false);
  const [previewData, setPreviewData] = useState(null);
  const [columnsSchema, setColumnsSchema] = useState([]);

  // Async task states
  const [uploadTaskId, setUploadTaskId] = useState(null);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [uploadStatusMsg, setUploadStatusMsg] = useState('적재 준비 중...');
  const [taskStatus, setTaskStatus] = useState('IDLE'); // IDLE, PROCESSING, COMPLETED, FAILED

  // File Upload -> Preview API Call
  const handleFileChange = async ({ fileList }) => {
    setFileList(fileList);
    if (fileList.length > 0) {
      const rawFile = fileList[0].originFileObj || fileList[0];
      setParsing(true);
      try {
        const formData = new FormData();
        formData.append('file', rawFile);
        const res = await previewDatasetFile(formData);
        setPreviewData(res);
        setColumnsSchema(res.columns || []);

        // 파일명에서 확장자를 제외한 명칭을 기본 데이터셋 이름으로 설정
        const fileNameWithoutExt = rawFile.name ? rawFile.name.replace(/\.[^/.]+$/, '') : '';
        form.setFieldsValue({
          name: form.getFieldValue('name') || fileNameWithoutExt,
        });

        if (!res.valid) {
          message.warning('엑셀 파일 구조에 적재 주의/불가 사항이 있습니다. 안내 메시지를 확인해 주세요.');
        } else {
          message.success('엑셀 미리보기 및 타입 자동 추정이 완료되었습니다.');
        }
        setCurrentStep(1);
      } catch (err) {
        message.error(err.message || '파일 파싱 실패');
      } finally {
        setParsing(false);
      }
    } else {
      setPreviewData(null);
      setColumnsSchema([]);
      setCurrentStep(0);
    }
  };

  const handleColumnNameChange = (columnKey, newTitle) => {
    setColumnsSchema((prev) =>
      prev.map((col) => (col.columnKey === columnKey ? { ...col, columnName: newTitle } : col))
    );
  };

  const handleDataTypeChange = (columnKey, newType) => {
    setColumnsSchema((prev) =>
      prev.map((col) => (col.columnKey === columnKey ? { ...col, dataType: newType } : col))
    );
  };

  // Start Async Upload
  const handleStartAsyncUpload = async () => {
    try {
      const values = await form.validateFields();
      if (fileList.length === 0) {
        message.error('첨부된 파일이 없습니다.');
        return;
      }

      const rawFile = fileList[0].originFileObj || fileList[0];
      const formData = new FormData();
      formData.append('file', rawFile);

      const metadataPayload = {
        name: values.name,
        description: values.description || '',
        category: values.category || '기타',
        ownerId: values.ownerId || 'user_admin',
        orgId: values.orgId || 'org_hq',
        columns: columnsSchema,
      };

      formData.append('metadata', JSON.stringify(metadataPayload));

      const res = await uploadDatasetAsync(formData);
      setUploadTaskId(res.taskId);
      setTaskStatus('PROCESSING');
      setCurrentStep(2);
    } catch (err) {
      message.error(err.message || '비동기 적재 요청에 실패했습니다.');
    }
  };

  // Polling task progress
  useEffect(() => {
    let timer = null;
    if (uploadTaskId && taskStatus === 'PROCESSING') {
      timer = setInterval(async () => {
        try {
          const res = await fetchTaskProgress(uploadTaskId);
          setUploadProgress(res.progress || 0);
          setUploadStatusMsg(res.message || '진행 중...');

          if (res.status === 'COMPLETED') {
            setTaskStatus('COMPLETED');
            clearInterval(timer);
            message.success('데이터 적재가 완전히 완료되었습니다!');
            setTimeout(() => {
              navigate(`/data/${res.datasetId}`);
            }, 1500);
          } else if (res.status === 'FAILED') {
            setTaskStatus('FAILED');
            clearInterval(timer);
            message.error(res.message || '적재 중 오류가 발생했습니다.');
          }
        } catch (err) {
          console.error(err);
        }
      }, 1000);
    }
    return () => {
      if (timer) clearInterval(timer);
    };
  }, [uploadTaskId, taskStatus, navigate]);

  const schemaColumns = [
    {
      title: '컬럼 키',
      dataIndex: 'columnKey',
      key: 'columnKey',
      width: 120,
      render: (text) => <Text code>{text}</Text>,
    },
    {
      title: '컬럼명 (표시 이름)',
      dataIndex: 'columnName',
      key: 'columnName',
      render: (text, record) => (
        <Input
          value={text}
          onChange={(e) => handleColumnNameChange(record.columnKey, e.target.value)}
        />
      ),
    },
    {
      title: '데이터 타입 설정',
      dataIndex: 'dataType',
      key: 'dataType',
      width: 180,
      render: (text, record) => (
        <Select
          value={text}
          style={{ width: '100%' }}
          onChange={(val) => handleDataTypeChange(record.columnKey, val)}
          options={[
            { value: 'STRING', label: '문자형 (STRING)' },
            { value: 'NUMBER', label: '숫자형 (NUMBER)' },
            { value: 'DATE', label: '날짜형 (DATE)' },
            { value: 'BOOLEAN', label: '불리언 (BOOLEAN)' },
          ]}
        />
      ),
    },
    {
      title: '정렬 순서',
      dataIndex: 'sortOrder',
      key: 'sortOrder',
      width: 100,
      render: (val) => val + 1,
    },
  ];

  const previewDataColumns = (columnsSchema || []).map((col) => ({
    title: col.columnName,
    dataIndex: col.columnKey,
    key: col.columnKey,
  }));

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Breadcrumb items={[{ title: '홈' }, { title: '데이터 적재 및 스키마 정의' }]} />

      <Card title={<Title level={4} style={{ margin: 0 }}>신규 데이터 적재 및 동적 스키마 정의</Title>}>
        <Steps
          current={currentStep}
          style={{ marginBottom: 32 }}
          items={[
            { title: '파일 업로드 & 미리보기' },
            { title: '동적 스키마 정의 & 유효성 검증' },
            { title: '비동기 적재 & 진행률' },
          ]}
        />

        {currentStep === 0 && (
          <Spin spinning={parsing} tip="엑셀 파일 분석 및 미리보기 파싱 중...">
            <Form
              form={form}
              layout="vertical"
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
                <Input.TextArea rows={2} placeholder="데이터셋의 목적 및 수집 경로 설명" />
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
                  onChange={handleFileChange}
                  maxCount={1}
                  accept=".xlsx,.xls,.csv"
                >
                  <p className="ant-upload-drag-icon">
                    <FileExcelOutlined style={{ fontSize: 36, color: '#1890ff' }} />
                  </p>
                  <p className="ant-upload-text">클릭하거나 엑셀/CSV 파일을 이곳으로 드래그하세요.</p>
                  <p className="ant-upload-hint">파일 첨부 즉시 하위 샘플 10행 파싱 및 컬럼 스키마가 자동 생성됩니다.</p>
                </Upload.Dragger>
              </Form.Item>
            </Form>
          </Spin>
        )}

        {currentStep === 1 && (
          <Space direction="vertical" size="large" style={{ width: '100%' }}>
            {previewData && !previewData.valid ? (
              <Alert
                message="DB 적재 제한 / 데이터 구조 주의사항"
                description={
                  <div>
                    {previewData.warnings && previewData.warnings.map((w, idx) => (
                      <div key={idx}>• {w}</div>
                    ))}
                    <div style={{ marginTop: 8, fontWeight: 'bold' }}>
                      * 데이터베이스 정형 테이블 저장이 불가능한 구조이므로 적재 진행이 제한됩니다. 파일을 수정 후 다시 시도해 주세요.
                    </div>
                  </div>
                }
                type="error"
                showIcon
              />
            ) : previewData && previewData.warnings && previewData.warnings.length > 0 ? (
              <Alert
                message="자동 스키마 보정 안내"
                description={
                  <div>
                    {previewData.warnings.map((w, idx) => (
                      <div key={idx}>• {w}</div>
                    ))}
                  </div>
                }
                type="warning"
                showIcon
              />
            ) : (
              <Alert
                message="정형 DB 적재가 가능한 파일 구조입니다."
                description="추정된 컬럼 데이터 타입을 검토 및 수정하신 후 [비동기 적재 시작]을 클릭하세요."
                type="success"
                showIcon
              />
            )}

            <Card size="small" title={<Space><PlusOutlined /><span>데이터셋 기본 정보 및 컬럼 스키마 정의</span></Space>}>
              <Form
                form={form}
                layout="vertical"
                initialValues={{ category: '에너지/환경', ownerId: 'user_admin', orgId: 'org_hq' }}
              >
                <Form.Item
                  name="name"
                  label="데이터셋 명칭"
                  rules={[{ required: true, message: '데이터셋 이름을 입력해주세요.' }]}
                >
                  <Input placeholder="예: 2026년 3분기 공장별 에너지 사용량" />
                </Form.Item>

                <Form.Item name="description" label="데이터셋 설명">
                  <Input.TextArea rows={2} placeholder="데이터셋의 목적 및 수집 경로 설명" />
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
              </Form>

              <Divider style={{ margin: '16px 0' }} />

              <Table
                columns={schemaColumns}
                dataSource={columnsSchema}
                rowKey="columnKey"
                pagination={false}
                size="small"
              />
            </Card>

            <Card size="small" title={<Space><EyeOutlined /><span>데이터 파싱 샘플 미리보기 (상위 {previewData?.totalPreviewRows || 0}행)</span></Space>}>
              <Table
                columns={previewDataColumns}
                dataSource={previewData?.previewRows || []}
                rowKey="key"
                pagination={false}
                size="small"
                scroll={{ x: 'max-content' }}
              />
            </Card>

            <Space>
              <Button
                type="primary"
                icon={<CheckCircleOutlined />}
                size="large"
                disabled={previewData && !previewData.valid}
                onClick={handleStartAsyncUpload}
              >
                비동기 적재 시작
              </Button>
              <Button size="large" onClick={() => setCurrentStep(0)}>
                이전 단계 (파일 재선택)
              </Button>
            </Space>
          </Space>
        )}

        {currentStep === 2 && (
          <div style={{ textAlign: 'center', padding: '40px 20px' }}>
            <Title level={4}>{uploadStatusMsg}</Title>
            <Progress
              type="circle"
              percent={uploadProgress}
              status={taskStatus === 'FAILED' ? 'exception' : uploadProgress === 100 ? 'success' : 'active'}
              size={140}
              style={{ margin: '24px 0' }}
            />
            <div>
              {taskStatus === 'PROCESSING' && (
                <Text type="secondary">서버에서 비동기로 대용량 엑셀 행을 읽어 DB에 적재 중입니다. 화면을 이탈해도 백그라운드에서 계속 진행됩니다.</Text>
              )}
              {taskStatus === 'COMPLETED' && (
                <Text type="success">적재 완료! 잠시 후 데이터 상세 화면으로 이동합니다.</Text>
              )}
              {taskStatus === 'FAILED' && (
                <div>
                  <Text type="danger">적재 실패 사유: {uploadStatusMsg}</Text>
                  <br />
                  <Button style={{ marginTop: 16 }} onClick={() => setCurrentStep(1)}>스키마 다시 확인</Button>
                </div>
              )}
            </div>
          </div>
        )}
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

