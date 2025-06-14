import React from 'react';
import { Button } from 'antd';

export default {
  title: '示例/按钮',
  component: Button,
};

const Template = (args) => <Button {...args} />;

export const 默认 = Template.bind({});
默认.args = {
  children: '点击',
};
