import { Table, Tag } from 'antd';
import { DATA_TYPE_OPTIONS } from '../constants/dataType';

const getTypeLabel = (value) => DATA_TYPE_OPTIONS.find((o) => o.value === value)?.label || value;

const DatasetSchemaTab = ({ columns }) => {
  const tableColumns = [
    { title: '순번', key: 'order', width: 80, render: (_, __, index) => index + 1 },
    { title: '컬럼명', dataIndex: 'title', key: 'title' },
    { title: '컬럼 키', dataIndex: 'key', key: 'key' },
    {
      title: '타입',
      dataIndex: 'dataType',
      key: 'dataType',
      render: (value) => <Tag>{getTypeLabel(value)}</Tag>,
    },
  ];

  return <Table columns={tableColumns} dataSource={columns} rowKey="key" pagination={false} />;
};

export default DatasetSchemaTab;
