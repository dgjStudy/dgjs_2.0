import { Form, Input, Modal, Space, Tag } from 'antd';

const RowFormModal = ({ open, form, columns, title, okText, placeholderSuffix, onOk, onCancel }) => (
  <Modal
    title={title}
    open={open}
    onOk={onOk}
    onCancel={onCancel}
    okText={okText}
    cancelText="취소"
    width={600}
  >
    <Form form={form} layout="vertical">
      {columns.map((col) => (
        <Form.Item
          key={col.key}
          name={col.key}
          label={
            <Space>
              <span>{col.title}</span>
              <Tag color="geekblue" style={{ fontSize: '11px' }}>{col.dataType}</Tag>
            </Space>
          }
        >
          <Input placeholder={`${col.title} ${placeholderSuffix}`} />
        </Form.Item>
      ))}
    </Form>
  </Modal>
);

export default RowFormModal;
