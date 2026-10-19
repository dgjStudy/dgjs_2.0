import { Button, Checkbox, Divider, Space, Tag, Typography } from 'antd';

const ColumnSettingsContent = ({ columns, selectedKeys, onToggle, onSelectAll, onDeselectAll }) => (
  <div style={{ width: 280, maxHeight: 350, overflowY: 'auto' }}>
    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 12, borderBottom: '1px solid #f0f0f0', paddingBottom: 8 }}>
      <Typography.Text strong>컬럼 표시 설정</Typography.Text>
      <Space size="small">
        <Button type="link" size="small" onClick={onSelectAll} style={{ padding: 0 }}>전체 선택</Button>
        <Divider orientation="vertical" />
        <Button type="link" size="small" onClick={onDeselectAll} style={{ padding: 0 }}>기본 1개만</Button>
      </Space>
    </div>
    <Space orientation="vertical" style={{ width: '100%' }}>
      {columns.map((col) => (
        <Checkbox
          key={col.key}
          checked={selectedKeys.includes(col.key)}
          onChange={() => onToggle(col.key)}
          style={{ width: '100%', marginInlineStart: 0 }}
        >
          <Space orientation="horizontal" size="small">
            <span>{col.title}</span>
            <Tag color="geekblue" style={{ fontSize: '11px', margin: 0 }}>{col.dataType}</Tag>
          </Space>
        </Checkbox>
      ))}
    </Space>
  </div>
);

export default ColumnSettingsContent;
