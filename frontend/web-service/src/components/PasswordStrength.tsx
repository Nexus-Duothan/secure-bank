import React from 'react';
import { Flex, Typography, theme } from 'antd';
import { PASSWORD_RULES } from './passwordRules';

const { Text } = Typography;

const STRENGTH_LABELS = ['Too weak', 'Weak', 'Fair', 'Good', 'Strong'];

export const PasswordStrengthMeter: React.FC<{ password: string }> = ({ password }) => {
  const { token } = theme.useToken();
  const score = PASSWORD_RULES.filter((rule) => rule.test(password)).length;
  const color =
    score <= 1 ? token.colorError : score <= 2 ? token.colorWarning : token.colorPrimary;

  return (
    <div style={{ marginTop: -8, marginBottom: 24 }}>
      <Flex gap={6}>
        {PASSWORD_RULES.map((rule) => (
          <div
            key={rule.label}
            style={{
              height: 4,
              flex: 1,
              borderRadius: 2,
              background: rule.test(password) ? color : token.colorBorder,
              transition: 'background-color 0.2s ease',
            }}
          />
        ))}
      </Flex>
      {password && (
        <Text style={{ fontSize: 12, color, marginTop: 4, display: 'block' }}>
          {STRENGTH_LABELS[score]}
        </Text>
      )}
    </div>
  );
};
