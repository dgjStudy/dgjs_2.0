import { useCallback, useEffect, useRef, useState } from 'react';
import { Button, Flex, Modal, Popconfirm, Select, Table, message } from 'antd';
import { searchEmployees } from '../api/employeeApi';
import { addProjectMember, fetchProjectMembers, removeProjectMember } from '../api/projectApi';

const SEARCH_DELAY_MS = 300;

// 프로젝트 참여자 목록을 보고, 활성 사원을 검색해 추가하거나 제거하는 모달임
const ProjectMembersModal = ({ project, onClose, onChanged }) => {
  const [members, setMembers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [options, setOptions] = useState([]);
  const [searching, setSearching] = useState(false);
  const [selected, setSelected] = useState(null);
  const [adding, setAdding] = useState(false);
  const timerRef = useRef(null);
  const projectCode = project?.projectCode;

  const loadMembers = useCallback(async () => {
    setLoading(true);
    try {
      setMembers(await fetchProjectMembers(projectCode));
    } catch (err) {
      message.error(err.message);
    } finally {
      setLoading(false);
    }
  }, [projectCode]);

  useEffect(() => {
    if (projectCode) {
      setSelected(null);
      setOptions([]);
      loadMembers();
    }
    return () => clearTimeout(timerRef.current);
  }, [projectCode, loadMembers]);

  const handleSearch = (keyword) => {
    clearTimeout(timerRef.current);
    if (!keyword.trim()) {
      setOptions([]);
      return;
    }
    timerRef.current = setTimeout(async () => {
      setSearching(true);
      try {
        const result = await searchEmployees({ keyword: keyword.trim(), size: 20 });
        const memberNos = new Set(members.map((m) => m.empNo));
        setOptions(
          result.employees.map((e) => {
            const note = memberNos.has(e.empNo) ? ' - 참여 중' : e.status !== 'ACTIVE' ? ' - 비활성' : '';
            return {
              value: e.empNo,
              label: `${e.name} (${e.empNo}, ${e.deptName})${note}`,
              disabled: note !== '',
            };
          }),
        );
      } catch (err) {
        message.error(err.message);
      } finally {
        setSearching(false);
      }
    }, SEARCH_DELAY_MS);
  };

  const handleAdd = async () => {
    setAdding(true);
    try {
      await addProjectMember(projectCode, selected);
      message.success('참여자를 추가했습니다.');
      setSelected(null);
      setOptions([]);
      await loadMembers();
      onChanged();
    } catch (err) {
      message.error(err.message);
    } finally {
      setAdding(false);
    }
  };

  const handleRemove = async (empNo) => {
    try {
      await removeProjectMember(projectCode, empNo);
      message.success('참여자를 제거했습니다.');
      await loadMembers();
      onChanged();
    } catch (err) {
      message.error(err.message);
    }
  };

  const columns = [
    { title: '사번', dataIndex: 'empNo', width: 100 },
    { title: '이름', dataIndex: 'name' },
    { title: '부서', dataIndex: 'deptName' },
    { title: '직급', dataIndex: 'jobTitle', width: 90 },
    {
      title: '관리',
      width: 90,
      render: (_, record) => (
        <Popconfirm
          title="참여자에서 제거할까요?"
          okText="제거"
          cancelText="취소"
          okButtonProps={{ danger: true }}
          onConfirm={() => handleRemove(record.empNo)}
        >
          <Button size="small" danger>제거</Button>
        </Popconfirm>
      ),
    },
  ];

  return (
    <Modal
      title={project ? `참여자 관리 - ${project.name}` : ''}
      open={Boolean(project)}
      footer={null}
      width={720}
      onCancel={onClose}
      destroyOnHidden
    >
      <Flex gap={8} style={{ marginBottom: 16 }}>
        <Select
          showSearch
          allowClear
          filterOption={false}
          placeholder="이름 또는 이메일로 활성 사원 검색"
          style={{ flex: 1 }}
          options={options}
          loading={searching}
          value={selected}
          onSearch={handleSearch}
          onChange={setSelected}
          notFoundContent="검색어를 입력하세요."
        />
        <Button type="primary" disabled={!selected} loading={adding} onClick={handleAdd}>
          추가
        </Button>
      </Flex>
      <Table
        rowKey="empNo"
        size="small"
        columns={columns}
        dataSource={members}
        loading={loading}
        pagination={false}
        locale={{ emptyText: '참여자가 없습니다.' }}
      />
    </Modal>
  );
};

export default ProjectMembersModal;
