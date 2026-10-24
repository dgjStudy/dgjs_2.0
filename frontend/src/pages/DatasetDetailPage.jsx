import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Button, Card, Space, Form, Typography, Breadcrumb, Tabs, message, Spin } from 'antd';
import { EditOutlined } from '@ant-design/icons';
import { fetchDatasetDetail, updateDatasetMeta } from '../api/datasetApi';
import PageTitle from '../components/PageTitle';
import MetaEditModal from '../components/MetaEditModal';
import DatasetDataTab from '../components/DatasetDataTab';
import DatasetSchemaTab from '../components/DatasetSchemaTab';
import DatasetInfoTab from '../components/DatasetInfoTab';

const DatasetDetailPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [detail, setDetail] = useState(null);
  const [loading, setLoading] = useState(true);
  const [totalRows, setTotalRows] = useState(0);
  const [metaModalOpen, setMetaModalOpen] = useState(false);
  const [metaForm] = Form.useForm();

  const loadDetail = async () => {
    setLoading(true);
    try {
      setDetail(await fetchDatasetDetail(id));
    } catch (err) {
      message.error(err.message || '상세 정보 조회 실패');
    } finally {
      setLoading(false);
    }
  };

  // 화면 깜박임 없이 최근 적재일 등 변경된 값만 다시 받아옴
  const refreshDetail = async () => {
    try {
      setDetail(await fetchDatasetDetail(id));
    } catch (err) {
      message.error(err.message || '상세 정보 갱신 실패');
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

  const allColumns = detail.columns || [];

  return (
    <Space orientation="vertical" size="large" style={{ width: '100%' }}>
      <Breadcrumb
        items={[
          { title: <a onClick={() => navigate('/data')}>데이터셋</a> },
          { title: detail.name },
        ]}
      />

      <PageTitle
        title={detail.name}
        description={detail.description || '등록된 설명이 없습니다.'}
        extra={
          <Button icon={<EditOutlined />} onClick={openMetaModal}>
            메타데이터 수정
          </Button>
        }
      />

      <Card>
        <Tabs
          items={[
            {
              key: 'data',
              label: '데이터',
              children: (
                <DatasetDataTab
                  datasetId={id}
                  columns={allColumns}
                  onTotalRowsChange={setTotalRows}
                  onRowAdded={refreshDetail}
                />
              ),
            },
            { key: 'schema', label: '스키마', children: <DatasetSchemaTab columns={allColumns} /> },
            { key: 'info', label: '정보', children: <DatasetInfoTab detail={detail} totalRows={totalRows} /> },
          ]}
        />
      </Card>

      <MetaEditModal
        open={metaModalOpen}
        form={metaForm}
        onOk={handleSaveMeta}
        onCancel={() => setMetaModalOpen(false)}
      />
    </Space>
  );
};

export default DatasetDetailPage;
