import { Form, Input, Modal, Select } from 'antd';
import { CATEGORY_OPTIONS } from '../constants/category';

const MetaEditModal = ({ open, form, onOk, onCancel }) => (
  <Modal
    title="데이터셋 메타데이터 수정"
    open={open}
    onOk={onOk}
    onCancel={onCancel}
    okText="저장"
    cancelText="취소"
  >
    <Form form={form} layout="vertical">
      <Form.Item
        name="name"
        label="데이터셋 명칭"
        rules={[{ required: true, message: '데이터셋 이름을 입력하세요.' }]}
      >
        <Input />
      </Form.Item>
      <Form.Item name="description" label="데이터셋 설명">
        <Input.TextArea rows={3} />
      </Form.Item>
      <Form.Item name="category" label="분류 카테고리">
        <Select options={CATEGORY_OPTIONS} />
      </Form.Item>
    </Form>
  </Modal>
);

export default MetaEditModal;
