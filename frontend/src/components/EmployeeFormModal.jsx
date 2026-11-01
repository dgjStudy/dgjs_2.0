import { useEffect, useState } from 'react';
import { Form, Input, Modal, Select, message } from 'antd';
import { createEmployee, updateEmployee } from '../api/employeeApi';
import { JOB_TITLES, ROLE_OPTIONS, STATUS_OPTIONS } from '../constants/employee';

const JOB_TITLE_OPTIONS = JOB_TITLES.map((title) => ({ value: title, label: title }));
const REQUIRED = (text) => [{ required: true, message: text }];

// 계정 발급(employee 없음)과 수정(employee 있음)을 함께 처리하는 모달임
const EmployeeFormModal = ({ open, employee, departments, onClose, onSaved }) => {
  const [form] = Form.useForm();
  const [submitting, setSubmitting] = useState(false);
  const isEdit = Boolean(employee);

  useEffect(() => {
    if (!open) {
      return;
    }
    form.resetFields();
    if (employee) {
      form.setFieldsValue({
        name: employee.name,
        email: employee.email,
        deptCode: employee.deptCode,
        jobTitle: employee.jobTitle,
        role: employee.role,
        status: employee.status,
      });
    } else {
      form.setFieldsValue({ jobTitle: JOB_TITLES[0], role: 'USER' });
    }
  }, [open, employee, form]);

  const handleFinish = async (values) => {
    const payload = { ...values, name: values.name.trim(), email: values.email.trim() };
    setSubmitting(true);
    try {
      if (isEdit) {
        await updateEmployee(employee.empNo, payload);
        message.success('사원 정보를 수정했습니다.');
        onSaved(null);
      } else {
        const created = await createEmployee(payload);
        message.success('계정을 발급했습니다.');
        onSaved({
          title: '계정 발급 완료',
          empNo: created.employee.empNo,
          name: created.employee.name,
          tempPassword: created.tempPassword,
        });
      }
    } catch (err) {
      message.error(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      title={isEdit ? `사원 수정 - ${employee.empNo}` : '계정 발급'}
      open={open}
      okText={isEdit ? '저장' : '발급'}
      cancelText="취소"
      confirmLoading={submitting}
      onOk={() => form.submit()}
      onCancel={onClose}
      destroyOnHidden
      forceRender
    >
      <Form form={form} layout="vertical" onFinish={handleFinish}>
        <Form.Item name="name" label="이름" rules={[...REQUIRED('이름을 입력하세요.'), { max: 50, message: '50자 이하로 입력하세요.' }]}>
          <Input maxLength={50} />
        </Form.Item>
        <Form.Item
          name="email"
          label="이메일 (로그인 ID)"
          rules={[...REQUIRED('이메일을 입력하세요.'), { type: 'email', message: '이메일 형식이 올바르지 않습니다.' }]}
        >
          <Input />
        </Form.Item>
        <Form.Item name="deptCode" label="부서" rules={REQUIRED('부서를 선택하세요.')}>
          <Select
            placeholder="부서 선택"
            options={departments.map((d) => ({ value: d.deptCode, label: d.name }))}
          />
        </Form.Item>
        <Form.Item name="jobTitle" label="직급" rules={REQUIRED('직급을 선택하세요.')}>
          <Select options={JOB_TITLE_OPTIONS} />
        </Form.Item>
        <Form.Item name="role" label="역할" rules={REQUIRED('역할을 선택하세요.')}>
          <Select options={ROLE_OPTIONS} />
        </Form.Item>
        {isEdit && (
          <Form.Item name="status" label="상태" rules={REQUIRED('상태를 선택하세요.')}>
            <Select options={STATUS_OPTIONS} />
          </Form.Item>
        )}
      </Form>
    </Modal>
  );
};

export default EmployeeFormModal;
