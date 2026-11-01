import { Alert, Button, Descriptions, Modal, Typography, message } from 'antd';

const { Text } = Typography;

// 임시 비밀번호를 한 번만 보여 주는 모달임. 닫으면 다시 확인할 수 없음
const TempPasswordModal = ({ info, onClose }) => {
  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(info.tempPassword);
      message.success('임시 비밀번호를 복사했습니다.');
    } catch {
      message.error('복사하지 못했습니다. 직접 선택해 복사하세요.');
    }
  };

  return (
    <Modal
      title={info?.title}
      open={Boolean(info)}
      onCancel={onClose}
      maskClosable={false}
      footer={<Button type="primary" onClick={onClose}>확인</Button>}
      destroyOnHidden
    >
      <Alert
        type="warning"
        showIcon
        title="임시 비밀번호는 이 화면에서만 확인할 수 있습니다."
        description="창을 닫으면 다시 볼 수 없으니 복사해서 사원에게 전달하세요. 사원은 첫 로그인 시 비밀번호를 변경해야 합니다."
        style={{ marginBottom: 16 }}
      />
      <Descriptions bordered column={1} size="small">
        <Descriptions.Item label="사원">{info?.name} ({info?.empNo})</Descriptions.Item>
        <Descriptions.Item label="임시 비밀번호">
          <Text code copyable={false}>{info?.tempPassword}</Text>
          <Button size="small" style={{ marginLeft: 12 }} onClick={handleCopy}>복사</Button>
        </Descriptions.Item>
      </Descriptions>
    </Modal>
  );
};

export default TempPasswordModal;
