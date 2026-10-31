import { Descriptions } from 'antd';
import { formatDateTime } from '../utils/formatDate';

const DatasetInfoTab = ({ detail, totalRows }) => (
  <Descriptions bordered column={{ xs: 1, sm: 1, md: 2 }}>
    <Descriptions.Item label="데이터셋 ID">{detail.id}</Descriptions.Item>
    <Descriptions.Item label="분류">{detail.category}</Descriptions.Item>
    <Descriptions.Item label="총 레코드 수">{totalRows.toLocaleString()} 행</Descriptions.Item>
    <Descriptions.Item label="컬럼 수">{detail.columns ? detail.columns.length : 0} 개</Descriptions.Item>
    <Descriptions.Item label="등록일">{formatDateTime(detail.createdAt)}</Descriptions.Item>
    <Descriptions.Item label="최근 적재일">{formatDateTime(detail.lastLoadedAt)}</Descriptions.Item>
    <Descriptions.Item label="소유자">
      {detail.ownerName ? `${detail.ownerName} (${detail.ownerId})` : detail.ownerId || '-'}
    </Descriptions.Item>
    <Descriptions.Item label="프로젝트">
      {detail.projectName ? `${detail.projectName} (${detail.projectCode})` : '미지정'}
    </Descriptions.Item>
  </Descriptions>
);

export default DatasetInfoTab;
