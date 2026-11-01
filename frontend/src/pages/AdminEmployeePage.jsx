import { useCallback, useEffect, useState } from 'react';
import { Button, Flex, Input, Popconfirm, Select, Space, Table, Tag, message } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { fetchMe } from '../api/authApi';
import { fetchDepartments } from '../api/departmentApi';
import { resetEmployeePassword, searchEmployees } from '../api/employeeApi';
import EmployeeFormModal from '../components/EmployeeFormModal';
import PageTitle from '../components/PageTitle';
import TempPasswordModal from '../components/TempPasswordModal';
import { ROLE_OPTIONS, STATUS_OPTIONS, labelOf } from '../constants/employee';
import { useAuth } from '../auth/useAuth';

const PAGE_SIZE = 20;

const AdminEmployeePage = () => {
  const { user, updateUser } = useAuth();
  const [departments, setDepartments] = useState([]);
  const [data, setData] = useState({ employees: [], totalElements: 0 });
  const [loading, setLoading] = useState(false);
  const [query, setQuery] = useState({ keyword: '', deptCode: '', page: 0 });
  const [formState, setFormState] = useState({ open: false, employee: null });
  const [tempInfo, setTempInfo] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setData(await searchEmployees({ ...query, size: PAGE_SIZE }));
    } catch (err) {
      message.error(err.message);
    } finally {
      setLoading(false);
    }
  }, [query]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    fetchDepartments().then(setDepartments).catch((err) => message.error(err.message));
  }, []);

  const handleSaved = (issued) => {
    const editedSelf = formState.employee?.empNo === user.empNo;
    setFormState({ open: false, employee: null });
    if (issued) {
      setTempInfo(issued);
    }
    if (editedSelf) {
      fetchMe().then(updateUser).catch((err) => message.error(err.message));
    }
    load();
  };

  const handleReset = async (record) => {
    try {
      const result = await resetEmployeePassword(record.empNo);
      setTempInfo({
        title: '비밀번호 초기화 완료',
        empNo: record.empNo,
        name: record.name,
        tempPassword: result.tempPassword,
      });
    } catch (err) {
      message.error(err.message);
    }
  };

  const columns = [
    { title: '사번', dataIndex: 'empNo', width: 90 },
    { title: '이름', dataIndex: 'name', width: 110 },
    { title: '이메일', dataIndex: 'email' },
    { title: '부서', dataIndex: 'deptName', width: 130 },
    { title: '직급', dataIndex: 'jobTitle', width: 80 },
    {
      title: '역할',
      dataIndex: 'role',
      width: 110,
      render: (value) => (value === 'ADMIN' ? <Tag color="blue">관리자</Tag> : labelOf(ROLE_OPTIONS, value)),
    },
    {
      title: '상태',
      dataIndex: 'status',
      width: 90,
      render: (value) => <Tag color={value === 'ACTIVE' ? 'green' : 'default'}>{labelOf(STATUS_OPTIONS, value)}</Tag>,
    },
    {
      title: '관리',
      width: 190,
      render: (_, record) => (
        <Space>
          <Button size="small" onClick={() => setFormState({ open: true, employee: record })}>수정</Button>
          <Popconfirm
            title="비밀번호를 초기화할까요?"
            description="새 임시 비밀번호가 발급되고 기존 비밀번호는 사용할 수 없게 됩니다."
            okText="초기화"
            cancelText="취소"
            onConfirm={() => handleReset(record)}
          >
            <Button size="small">비밀번호 초기화</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <Space orientation="vertical" size="large" style={{ width: '100%' }}>
      <PageTitle
        title="사원 관리"
        description="계정을 발급하고 사원 정보와 비밀번호를 관리합니다."
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setFormState({ open: true, employee: null })}>
            계정 발급
          </Button>
        }
      />
      <Flex gap={8} wrap>
        <Input.Search
          allowClear
          placeholder="이름 또는 이메일 검색"
          style={{ width: 280 }}
          onSearch={(keyword) => setQuery((prev) => ({ ...prev, keyword: keyword.trim(), page: 0 }))}
        />
        <Select
          allowClear
          placeholder="부서 전체"
          style={{ width: 180 }}
          options={departments.map((d) => ({ value: d.deptCode, label: d.name }))}
          onChange={(deptCode) => setQuery((prev) => ({ ...prev, deptCode: deptCode ?? '', page: 0 }))}
        />
      </Flex>
      <Table
        rowKey="empNo"
        columns={columns}
        dataSource={data.employees}
        loading={loading}
        pagination={{
          current: query.page + 1,
          pageSize: PAGE_SIZE,
          total: data.totalElements,
          showSizeChanger: false,
          showTotal: (total) => `총 ${total}명`,
          onChange: (page) => setQuery((prev) => ({ ...prev, page: page - 1 })),
        }}
      />
      <EmployeeFormModal
        open={formState.open}
        employee={formState.employee}
        departments={departments}
        onClose={() => setFormState({ open: false, employee: null })}
        onSaved={handleSaved}
      />
      <TempPasswordModal info={tempInfo} onClose={() => setTempInfo(null)} />
    </Space>
  );
};

export default AdminEmployeePage;
