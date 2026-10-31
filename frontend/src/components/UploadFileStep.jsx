import { Form, Spin, Upload } from 'antd';
import { FileExcelOutlined } from '@ant-design/icons';
import { CATEGORY_OPTIONS } from '../constants/category';
import DatasetMetaFields from './DatasetMetaFields';

const UploadFileStep = ({ form, parsing, fileList, onChange, projectOptions }) => (
  <Spin spinning={parsing} description="엑셀 파일 분석 및 미리보기 파싱 중...">
    <Form
      form={form}
      layout="vertical"
      initialValues={{ category: CATEGORY_OPTIONS[0].value }}
    >
      <DatasetMetaFields
        namePlaceholder="예: 2026년 3분기 공장별 에너지 사용량"
        nameRequiredMessage="데이터셋 이름을 입력하세요."
        descriptionPlaceholder="데이터셋의 목적 및 수집 경로 설명"
        projectOptions={projectOptions}
      />

      <Form.Item label="엑셀/CSV 데이터 파일 첨부" required>
        <Upload.Dragger
          beforeUpload={() => false}
          fileList={fileList}
          onChange={onChange}
          maxCount={1}
          accept=".xlsx,.xls,.csv"
        >
          <p className="ant-upload-drag-icon">
            <FileExcelOutlined style={{ fontSize: 36, color: '#1890ff' }} />
          </p>
          <p className="ant-upload-text">클릭하거나 엑셀/CSV 파일을 이곳으로 드래그하세요.</p>
          <p className="ant-upload-hint">파일 첨부 즉시 하위 샘플 10행 파싱 및 컬럼 스키마가 자동 생성됩니다.</p>
        </Upload.Dragger>
      </Form.Item>
    </Form>
  </Spin>
);

export default UploadFileStep;
