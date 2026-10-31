import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, Form, message, Space, Steps, Tabs } from 'antd';
import { EditOutlined, FileExcelOutlined } from '@ant-design/icons';
import {
  previewDatasetFile,
  uploadDatasetAsync,
  fetchTaskProgress,
  createManualDataset,
} from '../api/datasetApi';
import { fetchMyProjects } from '../api/projectApi';
import PageTitle from '../components/PageTitle';
import ManualSchemaForm from '../components/ManualSchemaForm';
import SchemaReviewStep from '../components/SchemaReviewStep';
import UploadFileStep from '../components/UploadFileStep';
import UploadProgressStep from '../components/UploadProgressStep';
import { DEFAULT_CATEGORY } from '../constants/category';
import { toProjectOptions } from '../utils/projectOptions';

const UploadPage = () => {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState('excel');
  const [form] = Form.useForm();
  const [manualForm] = Form.useForm();
  const [currentStep, setCurrentStep] = useState(0);
  const [fileList, setFileList] = useState([]);
  const [parsing, setParsing] = useState(false);
  const [previewData, setPreviewData] = useState(null);
  const [columnsSchema, setColumnsSchema] = useState([]);
  const [manualLoading, setManualLoading] = useState(false);
  const [projectOptions, setProjectOptions] = useState([]);

  // Async task states
  const [uploadTaskId, setUploadTaskId] = useState(null);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [uploadStatusMsg, setUploadStatusMsg] = useState('적재 준비 중...');
  const [taskStatus, setTaskStatus] = useState('IDLE'); // IDLE, PROCESSING, COMPLETED, FAILED

  useEffect(() => {
    fetchMyProjects()
      .then((projects) => setProjectOptions(toProjectOptions(projects)))
      .catch((err) => message.error(err.message || '프로젝트 목록을 불러오지 못했습니다.'));
  }, []);

  // File Upload -> Preview API Call
  const handleFileChange = async ({ fileList }) => {
    setFileList(fileList);
    if (fileList.length > 0) {
      const rawFile = fileList[0].originFileObj || fileList[0];
      setParsing(true);
      try {
        const formData = new FormData();
        formData.append('file', rawFile);
        const res = await previewDatasetFile(formData);
        setPreviewData(res);
        setColumnsSchema(res.columns || []);

        // 파일명에서 확장자를 제외한 명칭을 기본 데이터셋 이름으로 설정
        const fileNameWithoutExt = rawFile.name ? rawFile.name.replace(/\.[^/.]+$/, '') : '';
        form.setFieldsValue({
          name: form.getFieldValue('name') || fileNameWithoutExt,
        });

        if (!res.valid) {
          message.warning('엑셀 파일 구조에 적재 주의/불가 사항이 있습니다. 안내 메시지를 확인해 주세요.');
        } else {
          message.success('엑셀 미리보기 및 타입 자동 추정이 완료되었습니다.');
        }
        setCurrentStep(1);
      } catch (err) {
        message.error(err.message || '파일 파싱 실패');
      } finally {
        setParsing(false);
      }
    } else {
      setPreviewData(null);
      setColumnsSchema([]);
      setCurrentStep(0);
    }
  };

  const handleColumnNameChange = (columnKey, newTitle) => {
    setColumnsSchema((prev) =>
      prev.map((col) => (col.columnKey === columnKey ? { ...col, columnName: newTitle } : col))
    );
  };

  const handleDataTypeChange = (columnKey, newType) => {
    setColumnsSchema((prev) =>
      prev.map((col) => (col.columnKey === columnKey ? { ...col, dataType: newType } : col))
    );
  };

  const duplicateColumnNames = (() => {
    const seen = new Set();
    const duplicated = new Set();
    columnsSchema.forEach((col) => {
      const name = (col.columnName || '').trim();
      if (!name) return;
      if (seen.has(name)) duplicated.add(name);
      seen.add(name);
    });
    return [...duplicated];
  })();
  const hasBlankColumnName = columnsSchema.some((col) => !(col.columnName || '').trim());
  const schemaInvalid = duplicateColumnNames.length > 0 || hasBlankColumnName;

  // Start Async Upload
  const handleStartAsyncUpload = async () => {
    try {
      if (schemaInvalid) {
        message.error('컬럼명이 비어 있거나 중복된 컬럼이 있습니다. 컬럼명을 수정해 주세요.');
        return;
      }
      const values = await form.validateFields();
      if (fileList.length === 0) {
        message.error('첨부된 파일이 없습니다.');
        return;
      }

      const rawFile = fileList[0].originFileObj || fileList[0];
      const formData = new FormData();
      formData.append('file', rawFile);

      const metadataPayload = {
        name: values.name,
        description: values.description || '',
        category: values.category || DEFAULT_CATEGORY,
        projectCode: values.projectCode || '',
        columns: columnsSchema.map((col) => ({ ...col, columnName: col.columnName.trim() })),
      };

      formData.append('metadata', JSON.stringify(metadataPayload));

      const res = await uploadDatasetAsync(formData);
      setUploadTaskId(res.taskId);
      setTaskStatus('PROCESSING');
      setCurrentStep(2);
    } catch (err) {
      message.error(err.message || '비동기 적재 요청에 실패했습니다.');
    }
  };

  // Submit Manual Schema Creation
  const handleManualCreate = async () => {
    try {
      const values = await manualForm.validateFields();
      if (!values.columns || values.columns.length === 0) {
        message.error('최소 1개 이상의 컬럼을 추가해 주세요.');
        return;
      }

      setManualLoading(true);
      const res = await createManualDataset(values);
      message.success('테이블 스키마가 성공적으로 등록되었습니다!');
      navigate(`/data/${res.datasetId}`);
    } catch (err) {
      message.error(err.message || '수동 스키마 생성에 실패했습니다.');
    } finally {
      setManualLoading(false);
    }
  };

  // Polling task progress
  useEffect(() => {
    let timer = null;
    if (uploadTaskId && taskStatus === 'PROCESSING') {
      timer = setInterval(async () => {
        try {
          const res = await fetchTaskProgress(uploadTaskId);
          setUploadProgress(res.progress || 0);
          setUploadStatusMsg(res.message || '진행 중...');

          if (res.status === 'COMPLETED') {
            setTaskStatus('COMPLETED');
            clearInterval(timer);
            message.success('데이터 적재가 완전히 완료되었습니다!');
            setTimeout(() => {
              navigate(`/data/${res.datasetId}`);
            }, 1500);
          } else if (res.status === 'FAILED') {
            setTaskStatus('FAILED');
            clearInterval(timer);
            message.error(res.message || '적재 중 오류가 발생했습니다.');
          }
        } catch (err) {
          console.error(err);
        }
      }, 1000);
    }
    return () => {
      if (timer) clearInterval(timer);
    };
  }, [uploadTaskId, taskStatus, navigate]);

  return (
    <Space orientation="vertical" size="large" style={{ width: '100%' }}>
      <PageTitle title="신규 데이터 적재" description="엑셀·CSV 파일을 올리거나 스키마를 직접 설계해 데이터셋을 만듭니다." />

      <Card>
        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          style={{ marginBottom: 24 }}
          items={[
            {
              key: 'excel',
              label: <span><FileExcelOutlined /> 엑셀·CSV 파일 적재</span>,
            },
            {
              key: 'manual',
              label: <span><EditOutlined /> 직접 스키마 설계</span>,
            },
          ]}
        />

        {activeTab === 'manual' ? (
          <ManualSchemaForm
            form={manualForm}
            loading={manualLoading}
            onSubmit={handleManualCreate}
            projectOptions={projectOptions}
          />
        ) : (
          <div>
            <Steps
              current={currentStep}
              style={{ marginBottom: 32 }}
              items={[
                { title: '파일 업로드 & 미리보기' },
                { title: '동적 스키마 정의 & 유효성 검증' },
                { title: '비동기 적재 & 진행률' },
              ]}
            />

            {currentStep === 0 && (
              <UploadFileStep
                form={form}
                parsing={parsing}
                fileList={fileList}
                onChange={handleFileChange}
                projectOptions={projectOptions}
              />
            )}

            {currentStep === 1 && (
              <SchemaReviewStep
                form={form}
                previewData={previewData}
                columnsSchema={columnsSchema}
                duplicateColumnNames={duplicateColumnNames}
                hasBlankColumnName={hasBlankColumnName}
                schemaInvalid={schemaInvalid}
                onColumnNameChange={handleColumnNameChange}
                onDataTypeChange={handleDataTypeChange}
                onStart={handleStartAsyncUpload}
                onBack={() => setCurrentStep(0)}
                projectOptions={projectOptions}
              />
            )}

            {currentStep === 2 && (
              <UploadProgressStep
                statusMsg={uploadStatusMsg}
                progress={uploadProgress}
                taskStatus={taskStatus}
                onBackToSchema={() => setCurrentStep(1)}
              />
            )}
          </div>
        )}
      </Card>
    </Space>
  );
};

export default UploadPage;
