import type { ThemeConfig } from 'antd';

export const secureBankTheme: ThemeConfig = {
  token: {
    colorPrimary: '#1F7A6C',
    colorText: '#0B1B2B',
    colorTextSecondary: '#3C4A5C',
    colorTextTertiary: '#7C8AA0',
    colorBgLayout: '#EEF1F5',
    colorBorder: '#DCE1E8',
    colorError: '#B3423A',
    colorWarning: '#B7791F',
    borderRadius: 8,
    fontFamily: "'IBM Plex Sans', sans-serif",
  },
  components: {
    Button: {
      borderRadius: 8,
      controlHeight: 48,
    },
    Input: {
      borderRadius: 8,
      controlHeight: 44,
    },
    Card: {
      borderRadiusLG: 14,
    },
  },
};

export default secureBankTheme;
