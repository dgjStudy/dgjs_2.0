import { Form, Input, Select, Space } from 'antd';
import { CATEGORY_OPTIONS } from '../constants/category';

const DatasetMetaFields = ({
  namePlaceholder,
  nameRequiredMessage,
  descriptionPlaceholder,
  projectOptions = [],
}) => (
  <>
    <Form.Item
      name="name"
      label="데이터셋 명칭"
      rules={[{ required: true, message: nameRequiredMessage }]}
    >
      <Input placeholder={namePlaceholder} />
    </Form.Item>

    <Form.Item name="description" label="데이터셋 설명">
      <Input.TextArea rows={2} placeholder={descriptionPlaceholder} />
    </Form.Item>

    <Space size="large" align="start" style={{ display: 'flex' }}>
      <Form.Item name="category" label="분류 카테고리" style={{ width: 240 }}>
        <Select options={CATEGORY_OPTIONS} />
      </Form.Item>

      <Form.Item
        name="projectCode"
        label="프로젝트"
        extra="참여 중인 프로젝트만 선택할 수 있습니다. 비워 두면 미지정입니다."
        style={{ width: 360 }}
      >
        <Select allowClear showSearch optionFilterProp="label" placeholder="미지정" options={projectOptions} />
      </Form.Item>
    </Space>
  </>
);

export default DatasetMetaFields;
