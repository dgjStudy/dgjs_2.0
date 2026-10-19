import { useState, useEffect } from 'react';
import { BrowserRouter, Routes, Route, useNavigate } from 'react-router-dom';
import {
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
  message,
  Spin,
  Steps,
  Progress,
  Alert,
  Divider,
  Tabs,
} from 'antd';
import {
  FileExcelOutlined,
  EyeOutlined,
  PlusOutlined,
  CheckCircleOutlined,
  DeleteOutlined,
  EditOutlined,
} from '@ant-design/icons';
import {
  previewDatasetFile,
  uploadDatasetAsync,
  fetchTaskProgress,
  createManualDataset,
} from './api/datasetApi';
import MainLayout from './components/MainLayout';
import DatasetListPage from './pages/DatasetListPage';
import DatasetDetailPage from './pages/DatasetDetailPage';
import { CATEGORY_OPTIONS, DEFAULT_CATEGORY } from './constants/category';
import { DATA_TYPE_OPTIONS, DEFAULT_DATA_TYPE } from './constants/dataType';
import { MOCK_OWNER_ID, MOCK_ORG_ID } from './constants/mockUser';


const { Title, Text } = Typography;

// 3. /upload - Data Upload & Dynamic Schema Definition & Async Progress Page
const DataUploadPage = () => {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState('excel');
  const [form] = Form.useForm();
  const [manualForm] = Form.useForm();
  const [currentStep, setCurrentStep] = useState(0);
  const [fileList, setFileList] = useState([]);
  const [parsing, setParsing] = useState(false);
  const [previewData, setPreviewData] = useState(null);
  const [columnsSchema, setColumnsSchema] = useState([]);
  const [manualLoading, setManualLoading] = useState(false);

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

  const duplicateColumnNames = (() => {
    const seen = new Set();
    const duplicated = new Set();
    columnsSchema.forEach((col) => {
      const name = (col.columnName || '').trim();
      if (!name) return;
      if (seen.has(name)) duplicated.add(name);
      seen.add(name);
    });
    return [...duplicated];
  })();
  const hasBlankColumnName = columnsSchema.some((col) => !(col.columnName || '').trim());
  const schemaInvalid = duplicateColumnNames.length > 0 || hasBlankColumnName;

  // Start Async Upload
  const handleStartAsyncUpload = async () => {
    try {
      if (schemaInvalid) {
        message.error('컬럼명이 비어 있거나 중복된 컬럼이 있습니다. 컬럼명을 수정해 주세요.');
        return;
      }
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
        category: values.category || DEFAULT_CATEGORY,
        ownerId: values.ownerId || MOCK_OWNER_ID,
        orgId: values.orgId || MOCK_ORG_ID,
        columns: columnsSchema.map((col) => ({ ...col, columnName: col.columnName.trim() })),
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

  // Submit Manual Schema Creation
  const handleManualCreate = async () => {
    try {
      const values = await manualForm.validateFields();
      if (!values.columns || values.columns.length === 0) {
        message.error('최소 1개 이상의 컬럼을 추가해 주세요.');
        return;
      }

      setManualLoading(true);
      const res = await createManualDataset(values);
      message.success('테이블 스키마가 성공적으로 등록되었습니다!');
      navigate(`/data/${res.datasetId}`);
    } catch (err) {
      message.error(err.message || '수동 스키마 생성에 실패했습니다.');
    } finally {
      setManualLoading(false);
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
          options={DATA_TYPE_OPTIONS}
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
    <Space orientation="vertical" size="large" style={{ width: '100%' }}>
      <Breadcrumb items={[{ title: '홈' }, { title: '데이터 적재 및 스키마 정의' }]} />

      <Card title={<Title level={4} style={{ margin: 0 }}>신규 데이터 적재 및 동적 스키마 정의</Title>}>
        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          style={{ marginBottom: 24 }}
          items={[
            {
              key: 'excel',
              label: <span><FileExcelOutlined /> 엑셀 파일 업로드 기반 적재</span>,
            },
            {
              key: 'manual',
              label: <span><EditOutlined /> 직접 테이블 설계 및 생성</span>,
            },
          ]}
        />

        {activeTab === 'manual' ? (
          <Spin spinning={manualLoading}>
            <Form
              form={manualForm}
              layout="vertical"
              initialValues={{
                category: DEFAULT_CATEGORY,
                ownerId: MOCK_OWNER_ID,
                orgId: MOCK_ORG_ID,
                columns: [
                  { columnName: '', dataType: DEFAULT_DATA_TYPE },
                ],
              }}
            >
              <Card size="small" title="1. 데이터셋 기본 메타데이터" style={{ marginBottom: 20 }}>
                <Form.Item
                  name="name"
                  label="데이터셋 명칭"
                  rules={[{ required: true, message: '데이터셋 이름을 입력해주세요.' }]}
                >
                  <Input placeholder="예: 2026년 공장 설비 가동 이력 데이터셋" />
                </Form.Item>

                <Form.Item name="description" label="데이터셋 설명">
                  <Input.TextArea rows={2} placeholder="데이터셋의 용도 및 설명" />
                </Form.Item>

                <Space size="large" style={{ display: 'flex' }}>
                  <Form.Item name="category" label="분류 카테고리" style={{ width: 240 }}>
                    <Select
                      options={CATEGORY_OPTIONS}
                    />
                  </Form.Item>

                  <Form.Item name="orgId" label="소속 조직 (org_id)" style={{ width: 200 }}>
                    <Input disabled />
                  </Form.Item>

                  <Form.Item name="ownerId" label="등록자 (owner_id)" style={{ width: 200 }}>
                    <Input disabled />
                  </Form.Item>
                </Space>
              </Card>

              <Card
                size="small"
                title="2. 동적 컬럼 스키마 정의"
                extra={<Text type="secondary">동적 테이블 구조에 등록될 컬럼명과 타입을 지정합니다.</Text>}
                style={{ marginBottom: 24 }}
              >
                <Form.List name="columns">
                  {(fields, { add, remove }) => (
                    <Space orientation="vertical" style={{ width: '100%' }} size="middle">
                      {fields.map(({ key, name, ...restField }, index) => (
                        <Space key={key} style={{ display: 'flex', alignItems: 'center' }} align="baseline">
                          <Tag color="blue" style={{ minWidth: 40, textAlign: 'center' }}>
                            #{index + 1}
                          </Tag>
                          <Form.Item
                            {...restField}
                            name={[name, 'columnName']}
                            rules={[{ required: true, message: '컬럼명을 입력하세요.' }]}
                            style={{ width: 260, marginBottom: 0 }}
                          >
                            <Input placeholder={`컬럼명 (예: 식별자, 일자, 수치 등)`} />
                          </Form.Item>
                          <Form.Item
                            {...restField}
                            name={[name, 'dataType']}
                            rules={[{ required: true, message: '타입 선택' }]}
                            style={{ width: 180, marginBottom: 0 }}
                          >
                            <Select
                              options={DATA_TYPE_OPTIONS}
                            />
                          </Form.Item>
                          <Button
                            type="text"
                            danger
                            icon={<DeleteOutlined />}
                            onClick={() => {
                              if (fields.length <= 1) {
                                message.warning('최소 1개의 컬럼은 존재해야 합니다.');
                                return;
                              }
                              remove(name);
                            }}
                          />
                        </Space>
                      ))}
                      <Button
                        type="dashed"
                        onClick={() => add({ columnName: '', dataType: DEFAULT_DATA_TYPE })}
                        block
                        icon={<PlusOutlined />}
                      >
                        컬럼 추가하기
                      </Button>
                    </Space>
                  )}
                </Form.List>
              </Card>

              <Button
                type="primary"
                size="large"
                icon={<CheckCircleOutlined />}
                onClick={handleManualCreate}
              >
                테이블 스키마 생성 완료
              </Button>
            </Form>
          </Spin>
        ) : (
          <div>
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
          <Spin spinning={parsing} description="엑셀 파일 분석 및 미리보기 파싱 중...">
            <Form
              form={form}
              layout="vertical"
              initialValues={{ category: CATEGORY_OPTIONS[0].value, ownerId: MOCK_OWNER_ID, orgId: MOCK_ORG_ID }}
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
                    options={CATEGORY_OPTIONS}
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
          <Space orientation="vertical" size="large" style={{ width: '100%' }}>
            {schemaInvalid && (
              <Alert
                title="컬럼명을 수정해 주세요."
                description={
                  <div>
                    {duplicateColumnNames.map((name) => (
                      <div key={name}>• 중복된 컬럼명: {name}</div>
                    ))}
                    {hasBlankColumnName && <div>• 컬럼명이 비어 있는 컬럼이 있습니다.</div>}
                  </div>
                }
                type="error"
                showIcon
              />
            )}
            {previewData && !previewData.valid ? (
              <Alert
                title="DB 적재 제한 / 데이터 구조 주의사항"
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
                title="자동 스키마 보정 안내"
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
                title="정형 DB 적재가 가능한 파일 구조입니다."
                description="추정된 컬럼 데이터 타입을 검토 및 수정하신 후 [비동기 적재 시작]을 클릭하세요."
                type="success"
                showIcon
              />
            )}

            <Card size="small" title={<Space><PlusOutlined /><span>데이터셋 기본 정보 및 컬럼 스키마 정의</span></Space>}>
              <Form
                form={form}
                layout="vertical"
                initialValues={{ category: CATEGORY_OPTIONS[0].value, ownerId: MOCK_OWNER_ID, orgId: MOCK_ORG_ID }}
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
                      options={CATEGORY_OPTIONS}
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
                disabled={(previewData && !previewData.valid) || schemaInvalid}
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
          <Route path="/" element={<DatasetListPage />} />
          <Route path="/data" element={<DatasetListPage />} />
          <Route path="/data/:id" element={<DatasetDetailPage />} />
          <Route path="/upload" element={<DataUploadPage />} />
        </Routes>
      </MainLayout>
    </BrowserRouter>
  );
}

