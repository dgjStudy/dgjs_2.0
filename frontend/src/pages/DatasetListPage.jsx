import { useState, useEffect, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { Table, Button, Card, Tag, Space, Input, Typography, message, Popconfirm } from 'antd';
import { FileExcelOutlined, EyeOutlined, PlusOutlined, DeleteOutlined, SearchOutlined } from '@ant-design/icons';
import { fetchDatasets, deleteDataset } from '../api/datasetApi';
import { DEFAULT_CATEGORY } from '../constants/category';
import { formatDateTime, toTimestamp } from '../utils/formatDate';
import PageTitle from '../components/PageTitle';

const { Text } = Typography;

const SEARCH_STYLE = { maxWidth: 360 };

const DatasetListPage = () => {
  const navigate = useNavigate();
  const [datasets, setDatasets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [keyword, setKeyword] = useState('');

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

  const filteredDatasets = useMemo(() => {
    const word = keyword.trim().toLowerCase();
    if (!word) return datasets;
    return datasets.filter((d) => (d.name || '').toLowerCase().includes(word));
  }, [datasets, keyword]);

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
          <FileExcelOutlined style={{ marginRight: 8 }} />
          {text}
        </a>
      ),
    },
    {
      title: '프로젝트',
      dataIndex: 'projectName',
      key: 'projectName',
      render: (name) => (name ? <Tag color="geekblue">{name}</Tag> : <Text type="secondary">미지정</Text>),
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
      sorter: (a, b) => (a.rowCount || 0) - (b.rowCount || 0),
      render: (val) => `${(val || 0).toLocaleString()} 행`,
    },
    {
      title: '소유자',
      dataIndex: 'ownerName',
      key: 'ownerName',
      render: (name) => name || '-',
    },
    {
      title: '등록일',
      dataIndex: 'createdAt',
      key: 'createdAt',
      sorter: (a, b) => toTimestamp(a.createdAt) - toTimestamp(b.createdAt),
      render: formatDateTime,
    },
    {
      title: '최근 적재일',
      dataIndex: 'lastLoadedAt',
      key: 'lastLoadedAt',
      defaultSortOrder: 'descend',
      sorter: (a, b) => toTimestamp(a.lastLoadedAt) - toTimestamp(b.lastLoadedAt),
      render: formatDateTime,
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
          {record.canDelete && (
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
          )}
        </Space>
      ),
    },
  ];

  return (
    <Space orientation="vertical" size="large" style={{ width: '100%' }}>
      <PageTitle
        title="데이터셋"
        description="적재된 데이터를 검색하고 관리합니다."
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/upload')}>
            신규 데이터 적재
          </Button>
        }
      />
      <Card>
        <Space orientation="vertical" size="middle" style={{ width: '100%' }}>
          <Input
            allowClear
            prefix={<SearchOutlined />}
            placeholder="데이터셋명 검색"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            style={SEARCH_STYLE}
          />
          <Table
            columns={columns}
            dataSource={filteredDatasets}
            rowKey="id"
            loading={loading}
            pagination={{ pageSize: 10 }}
          />
        </Space>
      </Card>
    </Space>
  );
};

export default DatasetListPage;
