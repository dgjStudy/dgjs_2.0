import { useState } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { Button, Card, Flex, Form, Input, Typography, message, theme } from 'antd';
import { DatabaseOutlined, LockOutlined, UserOutlined } from '@ant-design/icons';
import { useAuth } from '../auth/useAuth';

const { Title, Text } = Typography;

const CARD_STYLE = { width: 380, maxWidth: '100%' };

const LoginPage = () => {
  const { user, loading, signIn } = useAuth();
  const location = useLocation();
  const { token } = theme.useToken();
  const [form] = Form.useForm();
  const [submitting, setSubmitting] = useState(false);

  const from = location.state?.from?.pathname || '/data';

  if (!loading && user) {
    return <Navigate to={from} replace />;
  }

  const handleFinish = async (values) => {
    setSubmitting(true);
    try {
      await signIn(values.email, values.password);
    } catch (err) {
      message.error(err.message || '로그인에 실패했습니다.');
      form.setFieldValue('password', '');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Flex
      justify="center"
      align="center"
      style={{ minHeight: '100vh', padding: 24, background: token.colorBgLayout }}
    >
      <Card style={CARD_STYLE}>
        <Flex vertical align="center" gap={4} style={{ marginBottom: 24 }}>
          <DatabaseOutlined style={{ fontSize: 32, color: token.colorPrimary }} />
          <Title level={4} style={{ margin: 0 }}>DGJS 2.0</Title>
          <Text type="secondary">계정으로 로그인하세요.</Text>
        </Flex>
        <Form form={form} layout="vertical" onFinish={handleFinish} requiredMark={false}>
          <Form.Item
            name="email"
            label="이메일"
            rules={[
              { required: true, message: '이메일을 입력하세요.' },
              { type: 'email', message: '이메일 형식이 올바르지 않습니다.' },
            ]}
          >
            <Input prefix={<UserOutlined />} placeholder="name@company.com" autoComplete="username" autoFocus />
          </Form.Item>
          <Form.Item name="password" label="비밀번호" rules={[{ required: true, message: '비밀번호를 입력하세요.' }]}>
            <Input.Password prefix={<LockOutlined />} autoComplete="current-password" />
          </Form.Item>
          <Button type="primary" htmlType="submit" block loading={submitting}>
            로그인
          </Button>
        </Form>
        <Text type="secondary" style={{ display: 'block', marginTop: 16, textAlign: 'center', fontSize: 12 }}>
          계정은 관리자에게 발급받을 수 있습니다.
        </Text>
      </Card>
    </Flex>
  );
};

export default LoginPage;
