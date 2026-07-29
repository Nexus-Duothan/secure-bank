import React from 'react';
import { Flex, Typography, theme } from 'antd';

const { Text } = Typography;

export const PASSWORD_RULES = [
  { test: (pw: string) => pw.length >= 8, label: 'At least 8 characters' },
  { test: (pw: string) => /[A-Z]/.test(pw), label: 'One uppercase letter' },
  { test: (pw: string) => /[0-9]/.test(pw), label: 'One number' },
  { test: (pw: string) => /[^A-Za-z0-9]/.test(pw), label: 'One symbol' },
];

export const PASSWORD_PATTERN = /^(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{8,}$/;

export const PASSWORD_COMPLEXITY_MESSAGE =
  'Use 8+ characters with an uppercase letter, a number, and a symbol';

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
