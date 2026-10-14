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
  Checkbox,
  Popover,
  Tabs,
  Modal,
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
  SettingOutlined,
  EditOutlined,
} from '@ant-design/icons';
import {
  fetchDatasets,
  fetchDatasetDetail,
  fetchDatasetRows,
  deleteDataset,
  previewDatasetFile,
  uploadDatasetAsync,
  fetchTaskProgress,
  createManualDataset,
  updateDatasetMeta,
  addDatasetRow,
  updateDatasetRow,
  deleteDatasetRow,
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
  const [selectedColumnKeys, setSelectedColumnKeys] = useState([]);

  // Server-side pagination states
  const [tableRows, setTableRows] = useState([]);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [totalRows, setTotalRows] = useState(0);
  const [tableLoading, setTableLoading] = useState(false);

  // Modal & Form states
  const [metaModalOpen, setMetaModalOpen] = useState(false);
  const [metaForm] = Form.useForm();
  const [addRowModalOpen, setAddRowModalOpen] = useState(false);
  const [addRowForm] = Form.useForm();
  const [editRowModalOpen, setEditRowModalOpen] = useState(false);
  const [editingRow, setEditingRow] = useState(null);
  const [editRowForm] = Form.useForm();

  const loadRows = async (pageToFetch, sizeToFetch) => {
    setTableLoading(true);
    try {
      const pagedData = await fetchDatasetRows(id, pageToFetch - 1, sizeToFetch);
      setTableRows(pagedData.rows || []);
      setTotalRows(pagedData.totalElements || 0);
      setCurrentPage(pageToFetch);
      setPageSize(sizeToFetch);
    } catch (err) {
      message.error(err.message || '행 데이터 페이징 조회 실패');
    } finally {
      setTableLoading(false);
    }
  };

  const loadDetail = async () => {
    setLoading(true);
    try {
      const data = await fetchDatasetDetail(id);
      setDetail(data);
      if (data && data.columns) {
        setSelectedColumnKeys(data.columns.map((c) => c.key));
      }
      await loadRows(1, 10);
    } catch (err) {
      message.error(err.message || '상세 정보 조회 실패');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
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

  const allColumns = detail.columns || [];

  const handleSelectAllColumns = () => {
    setSelectedColumnKeys(allColumns.map((c) => c.key));
  };

  const handleDeselectAllColumns = () => {
    if (allColumns.length > 0) {
      setSelectedColumnKeys([allColumns[0].key]);
    }
  };

  const handleToggleColumn = (key) => {
    if (selectedColumnKeys.includes(key)) {
      if (selectedColumnKeys.length === 1) {
        message.warning('최소 1개 이상의 컬럼은 표시되어야 합니다.');
        return;
      }
      setSelectedColumnKeys(selectedColumnKeys.filter((k) => k !== key));
    } else {
      setSelectedColumnKeys([...selectedColumnKeys, key]);
    }
  };

  // 1. Metadata Update
  const openMetaModal = () => {
    metaForm.setFieldsValue({
      name: detail.name,
      description: detail.description,
      category: detail.category,
    });
    setMetaModalOpen(true);
  };

  const handleSaveMeta = async () => {
    try {
      const values = await metaForm.validateFields();
      await updateDatasetMeta(id, values);
      message.success('메타데이터가 성공적으로 수정되었습니다.');
      setDetail((prev) => ({ ...prev, ...values }));
      setMetaModalOpen(false);
    } catch (err) {
      message.error(err.message || '메타데이터 수정 실패');
    }
  };

  // 2. Add Row
  const openAddRowModal = () => {
    addRowForm.resetFields();
    setAddRowModalOpen(true);
  };

  const handleSaveAddRow = async () => {
    try {
      const values = await addRowForm.validateFields();
      await addDatasetRow(id, values);
      message.success('새로운 데이터 행이 추가되었습니다.');
      setAddRowModalOpen(false);
      await loadRows(currentPage, pageSize);
    } catch (err) {
      message.error(err.message || '행 추가 실패');
    }
  };

  // 3. Edit Row
  const openEditRowModal = (record) => {
    setEditingRow(record);
    editRowForm.setFieldsValue(record);
    setEditRowModalOpen(true);
  };

  const handleSaveEditRow = async () => {
    try {
      const values = await editRowForm.validateFields();
      await updateDatasetRow(id, editingRow.key, values);
      message.success('데이터 행이 수정되었습니다.');
      setEditRowModalOpen(false);
      await loadRows(currentPage, pageSize);
    } catch (err) {
      message.error(err.message || '행 수정 실패');
    }
  };

  // 4. Delete Row
  const handleDeleteRow = async (rowId) => {
    try {
      await deleteDatasetRow(id, rowId);
      message.success('데이터 행이 삭제되었습니다.');
      await loadRows(currentPage, pageSize);
    } catch (err) {
      message.error(err.message || '행 삭제 실패');
    }
  };

  const columnFilterContent = (
    <div style={{ width: 280, maxHeight: 350, overflowY: 'auto' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 12, borderBottom: '1px solid #f0f0f0', paddingBottom: 8 }}>
        <Typography.Text strong>컬럼 표시 설정</Typography.Text>
        <Space size="small">
          <Button type="link" size="small" onClick={handleSelectAllColumns} style={{ padding: 0 }}>전체 선택</Button>
          <Divider type="vertical" />
          <Button type="link" size="small" onClick={handleDeselectAllColumns} style={{ padding: 0 }}>기본 1개만</Button>
        </Space>
      </div>
      <Space direction="vertical" style={{ width: '100%' }}>
        {allColumns.map((col) => (
          <Checkbox
            key={col.key}
            checked={selectedColumnKeys.includes(col.key)}
            onChange={() => handleToggleColumn(col.key)}
            style={{ width: '100%', marginInlineStart: 0 }}
          >
            <Space orientation="horizontal" size="small">
              <span>{col.title}</span>
              <Tag color="geekblue" style={{ fontSize: '11px', margin: 0 }}>{col.dataType}</Tag>
            </Space>
          </Checkbox>
        ))}
      </Space>
    </div>
  );

  const gridColumns = [
    ...allColumns
      .filter((col) => selectedColumnKeys.includes(col.key))
      .map((col) => ({
        title: (
          <div>
            <span>{col.title}</span>
            <div style={{ fontSize: '11px', color: '#8c8c8c', fontWeight: 'normal' }}>{col.dataType}</div>
          </div>
        ),
        dataIndex: col.key,
        key: col.key,
        ellipsis: true,
        width: 160,
      })),
    {
      title: '작업',
      key: 'action',
      fixed: 'right',
      width: 130,
      render: (_, record) => (
        <Space size="small">
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => openEditRowModal(record)}
            style={{ padding: 0 }}
          >
            수정
          </Button>
          <Popconfirm
            title="데이터 행 삭제"
            description="이 데이터 행을 삭제하시겠습니까?"
            onConfirm={() => handleDeleteRow(record.key)}
            okText="삭제"
            cancelText="취소"
          >
            <Button type="link" danger size="small" icon={<DeleteOutlined />} style={{ padding: 0 }}>
              삭제
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

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
            <Button icon={<EditOutlined />} onClick={openMetaModal}>
              메타데이터 수정
            </Button>
            <Tag color="green">메타데이터 정상</Tag>
            <Tag color="purple">ID: {detail.id}</Tag>
          </Space>
        </div>

        <Descriptions bordered size="small" column={{ xs: 1, sm: 2, md: 4 }}>
          <Descriptions.Item label="카테고리">{detail.category}</Descriptions.Item>
          <Descriptions.Item label="총 레코드 수">{totalRows.toLocaleString()} 행</Descriptions.Item>
          <Descriptions.Item label="컬럼 수">{detail.columns ? detail.columns.length : 0} 개</Descriptions.Item>
          <Descriptions.Item label="등록일시">{detail.createdAt ? new Date(detail.createdAt).toLocaleString() : '-'}</Descriptions.Item>
          <Descriptions.Item label="소속 조직(org_id)">{detail.orgId}</Descriptions.Item>
          <Descriptions.Item label="소유자(owner_id)">{detail.ownerId}</Descriptions.Item>
        </Descriptions>
      </Card>

      <Card
        title={
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Space>
              <FileExcelOutlined />
              <span>데이터 상세 그리드 뷰</span>
              <Tag color="cyan">{selectedColumnKeys.length} / {allColumns.length} 컬럼 표시 중</Tag>
            </Space>
            <Space>
              <Button type="primary" icon={<PlusOutlined />} onClick={openAddRowModal}>
                데이터 행 추가
              </Button>
              <Popover
                content={columnFilterContent}
                trigger="click"
                placement="bottomRight"
              >
                <Button icon={<SettingOutlined />}>
                  컬럼 표시 설정
                </Button>
              </Popover>
            </Space>
          </div>
        }
      >
        <Table
          columns={gridColumns}
          dataSource={tableRows}
          rowKey="key"
          bordered
          loading={tableLoading}
          scroll={{ x: 'max-content' }}
          pagination={{
            current: currentPage,
            pageSize: pageSize,
            total: totalRows,
            showSizeChanger: true,
            pageSizeOptions: ['10', '20', '50', '100'],
            showTotal: (total) => `총 ${total.toLocaleString()}개 행`,
            onChange: (page, size) => loadRows(page, size),
          }}
        />
      </Card>

      {/* 1. 메타데이터 수정 모달 */}
      <Modal
        title="데이터셋 메타데이터 수정"
        open={metaModalOpen}
        onOk={handleSaveMeta}
        onCancel={() => setMetaModalOpen(false)}
        okText="저장"
        cancelText="취소"
      >
        <Form form={metaForm} layout="vertical">
          <Form.Item
            name="name"
            label="데이터셋 명칭"
            rules={[{ required: true, message: '데이터셋 이름을 입력하세요.' }]}
          >
            <Input />
          </Form.Item>
          <Form.Item name="description" label="데이터셋 설명">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item name="category" label="분류 카테고리">
            <Select
              options={[
                { value: '에너지/환경', label: '에너지/환경' },
                { value: '설비관리', label: '설비관리' },
                { value: '품질관리', label: '품질관리' },
                { value: '생산실적', label: '생산실적' },
                { value: '기타', label: '기타' },
              ]}
            />
          </Form.Item>
        </Form>
      </Modal>

      {/* 2. 신규 행 추가 모달 */}
      <Modal
        title="신규 데이터 행 추가"
        open={addRowModalOpen}
        onOk={handleSaveAddRow}
        onCancel={() => setAddRowModalOpen(false)}
        okText="추가"
        cancelText="취소"
        width={600}
      >
        <Form form={addRowForm} layout="vertical">
          {allColumns.map((col) => (
            <Form.Item
              key={col.key}
              name={col.key}
              label={
                <Space>
                  <span>{col.title}</span>
                  <Tag color="geekblue" style={{ fontSize: '11px' }}>{col.dataType}</Tag>
                </Space>
              }
            >
              <Input placeholder={`${col.title} 값 입력`} />
            </Form.Item>
          ))}
        </Form>
      </Modal>

      {/* 3. 행 수정 모달 */}
      <Modal
        title="데이터 행 수정"
        open={editRowModalOpen}
        onOk={handleSaveEditRow}
        onCancel={() => setEditRowModalOpen(false)}
        okText="수정 완료"
        cancelText="취소"
        width={600}
      >
        <Form form={editRowForm} layout="vertical">
          {allColumns.map((col) => (
            <Form.Item
              key={col.key}
              name={col.key}
              label={
                <Space>
                  <span>{col.title}</span>
                  <Tag color="geekblue" style={{ fontSize: '11px' }}>{col.dataType}</Tag>
                </Space>
              }
            >
              <Input placeholder={`${col.title} 값 수정`} />
            </Form.Item>
          ))}
        </Form>
      </Modal>
    </Space>
  );
};

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
        category: values.category || '기타',
        ownerId: values.ownerId || 'user_admin',
        orgId: values.orgId || 'org_hq',
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
                category: '기타',
                ownerId: 'user_admin',
                orgId: 'org_hq',
                columns: [
                  { columnName: '', dataType: 'STRING' },
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
                      options={[
                        { value: '에너지/환경', label: '에너지/환경' },
                        { value: '설비관리', label: '설비관리' },
                        { value: '품질관리', label: '품질관리' },
                        { value: '생산실적', label: '생산실적' },
                        { value: '기타', label: '기타' },
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
              </Card>

              <Card
                size="small"
                title="2. 동적 컬럼 스키마 정의"
                extra={<Text type="secondary">동적 테이블 구조에 등록될 컬럼명과 타입을 지정합니다.</Text>}
                style={{ marginBottom: 24 }}
              >
                <Form.List name="columns">
                  {(fields, { add, remove }) => (
                    <Space direction="vertical" style={{ width: '100%' }} size="middle">
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
                              options={[
                                { value: 'STRING', label: '문자형 (STRING)' },
                                { value: 'NUMBER', label: '숫자형 (NUMBER)' },
                                { value: 'DATE', label: '날짜형 (DATE)' },
                                { value: 'BOOLEAN', label: '불리언 (BOOLEAN)' },
                              ]}
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
                        onClick={() => add({ columnName: '', dataType: 'STRING' })}
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
          <Route path="/" element={<DataListPage />} />
          <Route path="/data" element={<DataListPage />} />
          <Route path="/data/:id" element={<DataDetailPage />} />
          <Route path="/upload" element={<DataUploadPage />} />
        </Routes>
      </MainLayout>
    </BrowserRouter>
  );
}

