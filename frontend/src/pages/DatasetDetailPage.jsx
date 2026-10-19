import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  Table,
  Button,
  Card,
  Tag,
  Space,
  Form,
  Typography,
  Breadcrumb,
  Descriptions,
  message,
  Spin,
  Popconfirm,
  Popover,
} from 'antd';
import {
  ArrowLeftOutlined,
  PlusOutlined,
  DeleteOutlined,
  SettingOutlined,
  EditOutlined,
  FileExcelOutlined,
} from '@ant-design/icons';
import {
  fetchDatasetDetail,
  fetchDatasetRows,
  updateDatasetMeta,
  addDatasetRow,
  updateDatasetRow,
  deleteDatasetRow,
} from '../api/datasetApi';
import ColumnSettingsContent from '../components/ColumnSettingsContent';
import MetaEditModal from '../components/MetaEditModal';
import RowFormModal from '../components/RowFormModal';

const { Title, Text } = Typography;

const DatasetDetailPage = () => {
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
        <Spin size="large" description="데이터셋 정보를 불러오는 중..." />
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
    <Space orientation="vertical" size="large" style={{ width: '100%' }}>
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
                content={
                  <ColumnSettingsContent
                    columns={allColumns}
                    selectedKeys={selectedColumnKeys}
                    onToggle={handleToggleColumn}
                    onSelectAll={handleSelectAllColumns}
                    onDeselectAll={handleDeselectAllColumns}
                  />
                }
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

      <MetaEditModal
        open={metaModalOpen}
        form={metaForm}
        onOk={handleSaveMeta}
        onCancel={() => setMetaModalOpen(false)}
      />

      <RowFormModal
        open={addRowModalOpen}
        form={addRowForm}
        columns={allColumns}
        title="신규 데이터 행 추가"
        okText="추가"
        placeholderSuffix="값 입력"
        onOk={handleSaveAddRow}
        onCancel={() => setAddRowModalOpen(false)}
      />

      <RowFormModal
        open={editRowModalOpen}
        form={editRowForm}
        columns={allColumns}
        title="데이터 행 수정"
        okText="수정 완료"
        placeholderSuffix="값 수정"
        onOk={handleSaveEditRow}
        onCancel={() => setEditRowModalOpen(false)}
      />
    </Space>
  );
};

export default DatasetDetailPage;
