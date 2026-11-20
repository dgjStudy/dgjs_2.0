import { useState, useEffect, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { Table, Button, Card, Tag, Space, Input, Select, Switch, Flex, Typography, message, Popconfirm } from 'antd';
import { FileExcelOutlined, EyeOutlined, PlusOutlined, DeleteOutlined, SearchOutlined } from '@ant-design/icons';
import { fetchDatasets, deleteDataset } from '../api/datasetApi';
import { useAuth } from '../auth/useAuth';
import { DEFAULT_CATEGORY } from '../constants/category';
import { formatDateTime, toTimestamp } from '../utils/formatDate';
import PageTitle from '../components/PageTitle';

const { Text } = Typography;

const SEARCH_STYLE = { width: 280 };
const PROJECT_SELECT_STYLE = { width: 240 };
const NO_PROJECT = '__none__';
const PAGE_SIZE = 10;

const DatasetListPage = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [datasets, setDatasets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [keyword, setKeyword] = useState('');
  const [projectFilter, setProjectFilter] = useState(null);
  const [mineOnly, setMineOnly] = useState(false);
  const [page, setPage] = useState(1);

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

  const projectOptions = useMemo(() => {
    const map = new Map();
    datasets.forEach((d) => {
      if (d.projectCode) map.set(d.projectCode, d.projectName || d.projectCode);
    });
    const options = [...map].map(([value, label]) => ({ value, label }));
    return [{ value: NO_PROJECT, label: '프로젝트 미지정' }, ...options];
  }, [datasets]);

  const filteredDatasets = useMemo(() => {
    const word = keyword.trim().toLowerCase();
    return datasets.filter((d) => {
      if (word && !(d.name || '').toLowerCase().includes(word)) return false;
      if (projectFilter === NO_PROJECT && d.projectCode) return false;
      if (projectFilter && projectFilter !== NO_PROJECT && d.projectCode !== projectFilter) return false;
      if (mineOnly && d.ownerId !== user?.empNo) return false;
      return true;
    });
  }, [datasets, keyword, projectFilter, mineOnly, user]);

  const changeFilter = (setter) => (value) => {
    setter(value);
    setPage(1);
  };

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
          <Flex gap="middle" align="center" wrap>
            <Input
              allowClear
              prefix={<SearchOutlined />}
              placeholder="데이터셋명 검색"
              value={keyword}
              onChange={(e) => changeFilter(setKeyword)(e.target.value)}
              style={SEARCH_STYLE}
            />
            <Select
              allowClear
              showSearch={{ optionFilterProp: 'label' }}
              placeholder="프로젝트 선택"
              options={projectOptions}
              value={projectFilter}
              onChange={(value) => changeFilter(setProjectFilter)(value ?? null)}
              style={PROJECT_SELECT_STYLE}
            />
            <Space>
              <Switch checked={mineOnly} onChange={changeFilter(setMineOnly)} />
              <Text>내 것만</Text>
            </Space>
          </Flex>
          <Table
            columns={columns}
            dataSource={filteredDatasets}
            rowKey="id"
            loading={loading}
            pagination={{ pageSize: PAGE_SIZE, current: page, onChange: setPage }}
          />
        </Space>
      </Card>
    </Space>
  );
};

export default DatasetListPage;
