import { Form, Input, Modal, Select } from 'antd';
import { CATEGORY_OPTIONS } from '../constants/category';

const MetaEditModal = ({ open, form, onOk, onCancel, projectOptions = [], projectDisabled = false }) => (
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
      <Form.Item
        name="projectCode"
        label="프로젝트"
        extra={
          projectDisabled
            ? '프로젝트 연결은 소유자와 관리자만 바꿀 수 있습니다.'
            : '참여 중인 프로젝트만 선택할 수 있습니다. 비워 두면 미지정입니다.'
        }
      >
        <Select
          allowClear
          showSearch
          optionFilterProp="label"
          placeholder="미지정"
          options={projectOptions}
          disabled={projectDisabled}
        />
      </Form.Item>
    </Form>
  </Modal>
);

export default MetaEditModal;
