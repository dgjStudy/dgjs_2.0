import { Flex, Typography } from 'antd';

const { Title, Text } = Typography;

const PageTitle = ({ title, description, extra }) => (
  <Flex justify="space-between" align="center" gap={16} wrap>
    <div>
      <Title level={4} style={{ margin: 0 }}>{title}</Title>
      {description && <Text type="secondary">{description}</Text>}
    </div>
    {extra}
  </Flex>
);

export default PageTitle;
