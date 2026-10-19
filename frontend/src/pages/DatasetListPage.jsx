import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Table, Button, Card, Tag, Space, Typography, Breadcrumb, message, Popconfirm } from 'antd';
import { FileExcelOutlined, EyeOutlined, PlusOutlined, DeleteOutlined } from '@ant-design/icons';
import { fetchDatasets, deleteDataset } from '../api/datasetApi';
import { DEFAULT_CATEGORY } from '../constants/category';

const { Title, Text } = Typography;

const DatasetListPage = () => {
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
      render: (cat) => <Tag color="blue">{cat || DEFAULT_CATEGORY}</Tag>,
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
    <Space orientation="vertical" size="large" style={{ width: '100%' }}>
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

export default DatasetListPage;
