import { Button, Progress, Typography } from 'antd';

const { Title, Text } = Typography;

const UploadProgressStep = ({ statusMsg, progress, taskStatus, onBackToSchema }) => (
  <div style={{ textAlign: 'center', padding: '40px 20px' }}>
    <Title level={4}>{statusMsg}</Title>
    <Progress
      type="circle"
      percent={progress}
      status={taskStatus === 'FAILED' ? 'exception' : progress === 100 ? 'success' : 'active'}
      size={140}
      style={{ margin: '24px 0' }}
    />
    <div>
      {taskStatus === 'PROCESSING' && (
        <Text type="secondary">서버에서 비동기로 대용량 엑셀 행을 읽어 DB에 적재 중입니다. 화면을 이탈해도 백그라운드에서 계속 진행됩니다.</Text>
      )}
      {taskStatus === 'COMPLETED' && (
        <Text type="success">적재 완료! 잠시 후 데이터 상세 화면으로 이동합니다.</Text>
      )}
      {taskStatus === 'FAILED' && (
        <div>
          <Text type="danger">적재 실패 사유: {statusMsg}</Text>
          <br />
          <Button style={{ marginTop: 16 }} onClick={onBackToSchema}>스키마 다시 확인</Button>
        </div>
      )}
    </div>
  </div>
);

export default UploadProgressStep;
