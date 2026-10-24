import { useState, useEffect } from 'react';
import { Table, Button, Tag, Space, Flex, Form, Typography, message, Popconfirm, Popover } from 'antd';
import { PlusOutlined, DeleteOutlined, SettingOutlined, EditOutlined } from '@ant-design/icons';
import {
  fetchDatasetRows,
  addDatasetRow,
  updateDatasetRow,
  deleteDatasetRow,
} from '../api/datasetApi';
import ColumnSettingsContent from './ColumnSettingsContent';
import RowFormModal from './RowFormModal';

const { Text } = Typography;

const DatasetDataTab = ({ datasetId, columns, onTotalRowsChange, onRowAdded }) => {
  const [selectedColumnKeys, setSelectedColumnKeys] = useState(() => columns.map((c) => c.key));

  const [tableRows, setTableRows] = useState([]);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [totalRows, setTotalRows] = useState(0);
  const [tableLoading, setTableLoading] = useState(false);

  const [addRowModalOpen, setAddRowModalOpen] = useState(false);
  const [addRowForm] = Form.useForm();
  const [editRowModalOpen, setEditRowModalOpen] = useState(false);
  const [editingRow, setEditingRow] = useState(null);
  const [editRowForm] = Form.useForm();

  const loadRows = async (pageToFetch, sizeToFetch) => {
    setTableLoading(true);
    try {
      const pagedData = await fetchDatasetRows(datasetId, pageToFetch - 1, sizeToFetch);
      const total = pagedData.totalElements || 0;
      setTableRows(pagedData.rows || []);
      setTotalRows(total);
      onTotalRowsChange(total);
      setCurrentPage(pageToFetch);
      setPageSize(sizeToFetch);
    } catch (err) {
      message.error(err.message || '행 데이터 페이징 조회 실패');
    } finally {
      setTableLoading(false);
    }
  };

  useEffect(() => {
    loadRows(1, 10);
  }, [datasetId]);

  const handleSelectAllColumns = () => {
    setSelectedColumnKeys(columns.map((c) => c.key));
  };

  const handleDeselectAllColumns = () => {
    if (columns.length > 0) {
      setSelectedColumnKeys([columns[0].key]);
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

  const openAddRowModal = () => {
    addRowForm.resetFields();
    setAddRowModalOpen(true);
  };

  const handleSaveAddRow = async () => {
    try {
      const values = await addRowForm.validateFields();
      await addDatasetRow(datasetId, values);
      message.success('새로운 데이터 행이 추가되었습니다.');
      setAddRowModalOpen(false);
      await loadRows(currentPage, pageSize);
      onRowAdded();
    } catch (err) {
      message.error(err.message || '행 추가 실패');
    }
  };

  const openEditRowModal = (record) => {
    setEditingRow(record);
    editRowForm.setFieldsValue(record);
    setEditRowModalOpen(true);
  };

  const handleSaveEditRow = async () => {
    try {
      const values = await editRowForm.validateFields();
      await updateDatasetRow(datasetId, editingRow.key, values);
      message.success('데이터 행이 수정되었습니다.');
      setEditRowModalOpen(false);
      await loadRows(currentPage, pageSize);
    } catch (err) {
      message.error(err.message || '행 수정 실패');
    }
  };

  const handleDeleteRow = async (rowId) => {
    try {
      await deleteDatasetRow(datasetId, rowId);
      message.success('데이터 행이 삭제되었습니다.');
      await loadRows(currentPage, pageSize);
    } catch (err) {
      message.error(err.message || '행 삭제 실패');
    }
  };

  const gridColumns = [
    ...columns
      .filter((col) => selectedColumnKeys.includes(col.key))
      .map((col) => ({
        title: (
          <div>
            <span>{col.title}</span>
            <div>
              <Text type="secondary" style={{ fontSize: 11, fontWeight: 'normal' }}>{col.dataType}</Text>
            </div>
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
    <Space orientation="vertical" size="middle" style={{ width: '100%' }}>
      <Flex justify="space-between" align="center" wrap gap={8}>
        <Tag color="cyan">{selectedColumnKeys.length} / {columns.length} 컬럼 표시 중</Tag>
        <Space>
          <Button type="primary" icon={<PlusOutlined />} onClick={openAddRowModal}>
            데이터 행 추가
          </Button>
          <Popover
            content={
              <ColumnSettingsContent
                columns={columns}
                selectedKeys={selectedColumnKeys}
                onToggle={handleToggleColumn}
                onSelectAll={handleSelectAllColumns}
                onDeselectAll={handleDeselectAllColumns}
              />
            }
            trigger="click"
            placement="bottomRight"
          >
            <Button icon={<SettingOutlined />}>컬럼 표시 설정</Button>
          </Popover>
        </Space>
      </Flex>

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

      <RowFormModal
        open={addRowModalOpen}
        form={addRowForm}
        columns={columns}
        title="신규 데이터 행 추가"
        okText="추가"
        placeholderSuffix="값 입력"
        onOk={handleSaveAddRow}
        onCancel={() => setAddRowModalOpen(false)}
      />

      <RowFormModal
        open={editRowModalOpen}
        form={editRowForm}
        columns={columns}
        title="데이터 행 수정"
        okText="수정 완료"
        placeholderSuffix="값 수정"
        onOk={handleSaveEditRow}
        onCancel={() => setEditRowModalOpen(false)}
      />
    </Space>
  );
};

export default DatasetDataTab;
