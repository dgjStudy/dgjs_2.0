import { Alert, Button, Card, Divider, Form, Input, Select, Space, Table, Typography } from 'antd';
import { CheckCircleOutlined, EyeOutlined, PlusOutlined } from '@ant-design/icons';
import { CATEGORY_OPTIONS } from '../constants/category';
import { DATA_TYPE_OPTIONS } from '../constants/dataType';
import DatasetMetaFields from './DatasetMetaFields';

const { Text } = Typography;

const SchemaReviewStep = ({
  form,
  previewData,
  columnsSchema,
  duplicateColumnNames,
  hasBlankColumnName,
  schemaInvalid,
  onColumnNameChange,
  onDataTypeChange,
  onStart,
  onBack,
  projectOptions,
}) => {
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
          onChange={(e) => onColumnNameChange(record.columnKey, e.target.value)}
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
          onChange={(val) => onDataTypeChange(record.columnKey, val)}
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
          initialValues={{ category: CATEGORY_OPTIONS[0].value }}
        >
          <DatasetMetaFields
            namePlaceholder="예: 2026년 3분기 공장별 에너지 사용량"
            nameRequiredMessage="데이터셋 이름을 입력해주세요."
            descriptionPlaceholder="데이터셋의 목적 및 수집 경로 설명"
            projectOptions={projectOptions}
          />
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
          onClick={onStart}
        >
          비동기 적재 시작
        </Button>
        <Button size="large" onClick={onBack}>
          이전 단계 (파일 재선택)
        </Button>
      </Space>
    </Space>
  );
};

export default SchemaReviewStep;
