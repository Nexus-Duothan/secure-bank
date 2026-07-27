import React from 'react';
import { Layout, Typography, Card, Row, Col, Badge } from 'antd';
import { SecurityScanOutlined, BankOutlined, SafetyCertificateOutlined } from '@ant-design/icons';

const { Header, Content, Footer } = Layout;
const { Title, Paragraph, Text } = Typography;

const App: React.FC = () => {
  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header style={{ display: 'flex', alignItems: 'center', background: '#001529' }}>
        <div
          style={{
            color: '#fff',
            fontSize: '20px',
            fontWeight: 'bold',
            display: 'flex',
            alignItems: 'center',
            gap: '10px',
          }}
        >
          <BankOutlined style={{ fontSize: '24px', color: '#1890ff' }} />
          SecureBank Platform
        </div>
      </Header>
      <Content style={{ padding: '40px 50px' }}>
        <div style={{ background: '#fff', padding: 24, minHeight: 380, borderRadius: 8 }}>
          <Title level={2}>
            <SafetyCertificateOutlined style={{ color: '#52c41a', marginRight: 10 }} />
            SecureBank Resilient Digital Banking Platform
          </Title>
          <Paragraph>
            Welcome to the post-recovery SecureBank platform. Built on zero-trust microservice
            architecture with automated threat containment and immutable audit journaling.
          </Paragraph>

          <Row gutter={[16, 16]} style={{ marginTop: 24 }}>
            <Col xs={24} md={8}>
              <Card
                title="Core Banking Microservices"
                extra={<Badge status="processing" text="Active" />}
              >
                Accounts, Transfers (A2A), Payments (A2V), and Lending services running Spring Boot
                3 &amp; JDK 21.
              </Card>
            </Col>
            <Col xs={24} md={8}>
              <Card
                title="Resilience &amp; Audit Engine"
                extra={<Badge status="success" text="Rust Engine" />}
              >
                Memory-safe transaction journaling service providing continuous state replay and
                recovery.
              </Card>
            </Col>
            <Col xs={24} md={8}>
              <Card
                title="Unified API Gateway"
                extra={<Badge status="warning" text="Rate Limited" />}
              >
                Hardened perimeter handling mTLS, zero-trust authentication, and request routing.
              </Card>
            </Col>
          </Row>
        </div>
      </Content>
      <Footer style={{ textAlign: 'center' }}>
        SecureBank ©2026 Prepared by Team Nexus - University of Moratuwa
      </Footer>
    </Layout>
  );
};

export default App;
