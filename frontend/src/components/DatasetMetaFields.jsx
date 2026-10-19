import { Form, Input, Select, Space } from 'antd';
import { CATEGORY_OPTIONS } from '../constants/category';

const DatasetMetaFields = ({ namePlaceholder, nameRequiredMessage, descriptionPlaceholder }) => (
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

    <Space size="large" style={{ display: 'flex' }}>
      <Form.Item name="category" label="분류 카테고리" style={{ width: 240 }}>
        <Select options={CATEGORY_OPTIONS} />
      </Form.Item>

      <Form.Item name="orgId" label="소속 조직 (org_id)" style={{ width: 200 }}>
        <Input disabled />
      </Form.Item>

      <Form.Item name="ownerId" label="등록자 (owner_id)" style={{ width: 200 }}>
        <Input disabled />
      </Form.Item>
    </Space>
  </>
);

export default DatasetMetaFields;
