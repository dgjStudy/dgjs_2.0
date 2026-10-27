import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Alert, Button, Card, Descriptions, Form, Input, Space, message } from 'antd';
import { changePassword } from '../api/authApi';
import { useAuth } from '../auth/useAuth';
import PageTitle from '../components/PageTitle';

const ROLE_LABELS = { ADMIN: '관리자', USER: '일반 사용자' };
const PASSWORD_HINT = '8자 이상 64자 이하, 영문과 숫자를 모두 포함해야 합니다.';
const FORM_STYLE = { maxWidth: 420 };

const MyPage = () => {
  const navigate = useNavigate();
  const { user, updateUser } = useAuth();
  const [form] = Form.useForm();
  const [submitting, setSubmitting] = useState(false);

  const handleFinish = async (values) => {
    setSubmitting(true);
    try {
      const wasRequired = user.mustChangePassword;
      const updated = await changePassword(values.currentPassword, values.newPassword);
      updateUser(updated);
      form.resetFields();
      message.success('비밀번호를 변경했습니다.');
      if (wasRequired) {
        navigate('/data', { replace: true });
      }
    } catch (err) {
      message.error(err.message || '비밀번호 변경에 실패했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Space orientation="vertical" size="large" style={{ width: '100%' }}>
      <PageTitle title="내 정보" description="계정 정보를 확인하고 비밀번호를 변경합니다." />

      {user.mustChangePassword && (
        <Alert
          type="warning"
          showIcon
          title="임시 비밀번호를 사용 중입니다."
          description="새 비밀번호로 변경해야 다른 화면을 사용할 수 있습니다."
        />
      )}

      <Card title="계정 정보">
        <Descriptions bordered column={{ xs: 1, sm: 1, md: 2 }}>
          <Descriptions.Item label="사번">{user.empNo}</Descriptions.Item>
          <Descriptions.Item label="이름">{user.name}</Descriptions.Item>
          <Descriptions.Item label="이메일">{user.email}</Descriptions.Item>
          <Descriptions.Item label="부서">{user.deptName}</Descriptions.Item>
          <Descriptions.Item label="직급">{user.jobTitle}</Descriptions.Item>
          <Descriptions.Item label="역할">{ROLE_LABELS[user.role] || user.role}</Descriptions.Item>
        </Descriptions>
      </Card>

      <Card title="비밀번호 변경">
        <Form form={form} layout="vertical" onFinish={handleFinish} style={FORM_STYLE}>
          <Form.Item
            name="currentPassword"
            label="현재 비밀번호"
            rules={[{ required: true, message: '현재 비밀번호를 입력하세요.' }]}
          >
            <Input.Password autoComplete="current-password" />
          </Form.Item>
          <Form.Item
            name="newPassword"
            label="새 비밀번호"
            extra={PASSWORD_HINT}
            rules={[
              { required: true, message: '새 비밀번호를 입력하세요.' },
              { min: 8, max: 64, message: '8자 이상 64자 이하로 입력하세요.' },
              { pattern: /^(?=.*[A-Za-z])(?=.*[0-9]).+$/, message: '영문과 숫자를 모두 포함해야 합니다.' },
            ]}
          >
            <Input.Password autoComplete="new-password" />
          </Form.Item>
          <Form.Item
            name="confirmPassword"
            label="새 비밀번호 확인"
            dependencies={['newPassword']}
            rules={[
              { required: true, message: '새 비밀번호를 한 번 더 입력하세요.' },
              ({ getFieldValue }) => ({
                validator: (_, value) =>
                  !value || getFieldValue('newPassword') === value
                    ? Promise.resolve()
                    : Promise.reject(new Error('새 비밀번호가 일치하지 않습니다.')),
              }),
            ]}
          >
            <Input.Password autoComplete="new-password" />
          </Form.Item>
          <Button type="primary" htmlType="submit" loading={submitting}>
            비밀번호 변경
          </Button>
        </Form>
      </Card>
    </Space>
  );
};

export default MyPage;
