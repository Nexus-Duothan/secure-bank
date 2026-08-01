export const PASSWORD_RULES = [
  { test: (pw: string) => pw.length >= 8, label: 'At least 8 characters' },
  { test: (pw: string) => /[A-Z]/.test(pw), label: 'One uppercase letter' },
  { test: (pw: string) => /[0-9]/.test(pw), label: 'One number' },
  { test: (pw: string) => /[^A-Za-z0-9]/.test(pw), label: 'One symbol' },
];

export const PASSWORD_PATTERN = /^(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{8,}$/;

export const PASSWORD_COMPLEXITY_MESSAGE =
  'Use 8+ characters with an uppercase letter, a number, and a symbol';
