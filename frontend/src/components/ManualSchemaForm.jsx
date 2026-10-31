import { Button, Card, Form, Input, message, Select, Space, Spin, Tag, Typography } from 'antd';
import { CheckCircleOutlined, DeleteOutlined, PlusOutlined } from '@ant-design/icons';
import { DEFAULT_CATEGORY } from '../constants/category';
import { DATA_TYPE_OPTIONS, DEFAULT_DATA_TYPE } from '../constants/dataType';
import DatasetMetaFields from './DatasetMetaFields';

const { Text } = Typography;

const ManualSchemaForm = ({ form, loading, onSubmit, projectOptions }) => (
  <Spin spinning={loading}>
    <Form
      form={form}
      layout="vertical"
      initialValues={{
        category: DEFAULT_CATEGORY,
        columns: [
          { columnName: '', dataType: DEFAULT_DATA_TYPE },
        ],
      }}
    >
      <Card size="small" title="1. 데이터셋 기본 메타데이터" style={{ marginBottom: 20 }}>
        <DatasetMetaFields
          namePlaceholder="예: 2026년 공장 설비 가동 이력 데이터셋"
          nameRequiredMessage="데이터셋 이름을 입력해주세요."
          descriptionPlaceholder="데이터셋의 용도 및 설명"
          projectOptions={projectOptions}
        />
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
                    <Select options={DATA_TYPE_OPTIONS} />
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
        onClick={onSubmit}
      >
        테이블 스키마 생성 완료
      </Button>
    </Form>
  </Spin>
);

export default ManualSchemaForm;
