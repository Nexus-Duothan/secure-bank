import React from 'react';
import ReactDOM from 'react-dom/client';
import { ConfigProvider } from 'antd';
import '@fontsource/newsreader/400.css';
import '@fontsource/newsreader/500.css';
import '@fontsource/ibm-plex-mono';
import '@fontsource/ibm-plex-sans';
import App from './App';
import './index.css';
import { secureBankTheme } from './theme/secureBankTheme';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ConfigProvider theme={secureBankTheme}>
      <App />
    </ConfigProvider>
  </React.StrictMode>
);
