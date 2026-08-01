import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Alert, Button, Card, Flex, Form, Segmented, Select, Typography, theme } from 'antd';
import { LeftOutlined, SafetyCertificateOutlined } from '@ant-design/icons';
import accountsService, {
  type AccountProduct,
  type AccountType,
  type OpenAccountPayload,
} from '../../api/accountsService';
import { getApiErrorMessage } from '../../api/apiError';
import { DEMO_ACCOUNT_PRODUCTS } from '../../mocks/demoCustomer';

const { Text, Title } = Typography;

const formatMoney = (value: number, currency: string) =>
  new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency,
    minimumFractionDigits: 2,
  }).format(value);

const OpenAccount: React.FC = () => {
  const { token } = theme.useToken();
  const navigate = useNavigate();
  const [form] = Form.useForm<OpenAccountPayload>();
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [products, setProducts] = useState<AccountProduct[]>(DEMO_ACCOUNT_PRODUCTS);
  const [accountType, setAccountType] = useState<AccountType>('SAVINGS');
  const [productCode, setProductCode] = useState<string | undefined>(undefined);

  useEffect(() => {
    let cancelled = false;
    accountsService
      .getAccountProducts()
      .then((data) => {
        if (!cancelled && data.length > 0) setProducts(data);
      })
      .catch(() => {
        // Endpoint not available yet — fall back to the placeholder shown above.
      });
    return () => {
      cancelled = true;
    };
  }, []);

  // Only products the bank offers for the chosen account type can be picked.
  const availableProducts = useMemo(
    () => products.filter((product) => product.accountType === accountType),
    [products, accountType]
  );

  const selectedProduct = availableProducts.find((product) => product.code === productCode);

  const handleAccountTypeChange = (value: AccountType) => {
    setAccountType(value);
    setProductCode(undefined);
    form.setFieldsValue({ accountType: value, productCode: undefined });
  };

  const handleFinish = async (values: OpenAccountPayload) => {
    setSubmitting(true);
    setError(null);
    try {
      const challenge = await accountsService.requestAccountOpening(values);
      navigate('/verify-otp', {
        state: {
          flow: 'account-opening',
          challenge,
          successMessage: 'New account opened and selected',
          returnTo: '/accounts',
        },
      });
    } catch (err) {
      setError(getApiErrorMessage(err, 'We could not open this account right now.'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div style={{ minHeight: '100vh', background: token.colorBgLayout }}>
      <div style={{ maxWidth: 480, margin: '0 auto', padding: '32px 20px 48px' }}>
        <Flex align="center" gap={16} style={{ marginBottom: 28 }}>
          <Button
            type="text"
            shape="circle"
            icon={<LeftOutlined />}
            onClick={() => navigate('/accounts')}
          />
          <Title level={3} className="font-display" style={{ margin: 0, fontWeight: 600 }}>
            Open new account
          </Title>
        </Flex>

        {error && <Alert type="error" message={error} showIcon style={{ marginBottom: 20 }} />}

        <Card styles={{ body: { padding: 28 } }}>
          <Form<OpenAccountPayload>
            form={form}
            layout="vertical"
            requiredMark={false}
            initialValues={{ accountType: 'SAVINGS', ownershipType: 'INDIVIDUAL' }}
            disabled={submitting}
            onFinish={handleFinish}
          >
            <Form.Item label="Account type" name="accountType">
              <Segmented
                block
                onChange={(value) => handleAccountTypeChange(value as AccountType)}
                options={[
                  { label: 'Savings', value: 'SAVINGS' },
                  { label: 'Current', value: 'CURRENT' },
                ]}
              />
            </Form.Item>

            <Form.Item
              label="Account product"
              name="productCode"
              rules={[{ required: true, message: 'Choose one of the bank’s account products' }]}
            >
              <Select
                size="large"
                placeholder="Choose an account product"
                onChange={(value: string) => setProductCode(value)}
                options={availableProducts.map((product) => ({
                  value: product.code,
                  label:
                    product.interestRate > 0
                      ? `${product.name} — ${product.interestRate}% p.a.`
                      : product.name,
                }))}
              />
            </Form.Item>

            {selectedProduct && (
              <div
                style={{
                  background: token.colorFillQuaternary,
                  border: `1px solid ${token.colorBorderSecondary}`,
                  borderRadius: 12,
                  padding: 16,
                  marginBottom: 24,
                }}
              >
                <Text style={{ display: 'block', fontSize: 13, marginBottom: 12 }}>
                  {selectedProduct.description}
                </Text>
                <Flex justify="space-between" style={{ marginBottom: 6 }}>
                  <Text style={{ fontSize: 12, color: token.colorTextSecondary }}>
                    Interest rate
                  </Text>
                  <Text className="font-mono" style={{ fontSize: 12, fontWeight: 600 }}>
                    {selectedProduct.interestRate > 0
                      ? `${selectedProduct.interestRate}% p.a.`
                      : 'No interest'}
                  </Text>
                </Flex>
                <Flex justify="space-between" style={{ marginBottom: 6 }}>
                  <Text style={{ fontSize: 12, color: token.colorTextSecondary }}>
                    Minimum balance
                  </Text>
                  <Text className="font-mono" style={{ fontSize: 12, fontWeight: 600 }}>
                    {formatMoney(selectedProduct.minimumBalance, selectedProduct.currency)}
                  </Text>
                </Flex>
                <Flex justify="space-between">
                  <Text style={{ fontSize: 12, color: token.colorTextSecondary }}>Monthly fee</Text>
                  <Text className="font-mono" style={{ fontSize: 12, fontWeight: 600 }}>
                    {selectedProduct.monthlyFee > 0
                      ? formatMoney(selectedProduct.monthlyFee, selectedProduct.currency)
                      : 'Free'}
                  </Text>
                </Flex>
              </div>
            )}

            <Form.Item label="Ownership" name="ownershipType">
              <Select
                size="large"
                options={[
                  { label: 'Individual account', value: 'INDIVIDUAL' },
                  { label: 'Joint account', value: 'JOINT' },
                ]}
              />
            </Form.Item>

            <Button type="primary" size="large" block htmlType="submit" loading={submitting}>
              Review and verify
            </Button>
          </Form>
        </Card>

        <Flex gap={10} style={{ margin: '24px 4px 0' }}>
          <SafetyCertificateOutlined style={{ color: token.colorPrimary, marginTop: 3 }} />
          <Text style={{ color: token.colorTextSecondary, fontSize: 13 }}>
            A debit card is issued for the new account after mobile verification.
          </Text>
        </Flex>
      </div>
    </div>
  );
};

export default OpenAccount;
