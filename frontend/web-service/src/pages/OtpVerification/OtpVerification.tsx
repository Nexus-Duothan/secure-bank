import React, { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Alert, Button, Card, Flex, Input, Typography, theme } from 'antd';
import type { AxiosError } from 'axios';
import { authService } from '../../api/authService';
import { getApiErrorMessage } from '../../api/apiError';
import accountsService from '../../api/accountsService';
import adminService from '../../api/adminService';
import tokenStorage from '../../api/tokenStorage';
import sessionUser, { homePathForRole } from '../../api/sessionUser';
import userService from '../../api/userService';
import accountSelection from '../../api/accountSelection';
import type { Role } from '../../types';
import type { OtpChallenge } from '../../types';
import AuthLayout from '../../components/AuthLayout';
import TrustIndicator from '../../components/TrustIndicator';
import { formatCountdown, useCountdown } from '../../hooks/useCountdown';

const { Text, Link } = Typography;

const OTP_LENGTH = 6;
const RESEND_SECONDS = 30;

interface LoginLocationState {
  preAuthToken?: string;
  usernameOrEmail?: string;
  /** Set only by sign-up, the one flow still confirmed by an SMS code to the mobile number. */
  registration?: boolean;
}

interface ProfileChangeLocationState {
  flow: 'profile-change';
  challenge: OtpChallenge;
  successMessage: string;
  returnTo: string;
}

interface AccountChangeLocationState {
  flow: 'account-change';
  challenge: OtpChallenge;
  successMessage: string;
  returnTo: string;
}

interface AccountLinkLocationState {
  flow: 'account-link';
  challenge: OtpChallenge;
  successMessage: string;
  returnTo: string;
}

interface AccountOpeningLocationState {
  flow: 'account-opening';
  challenge: OtpChallenge;
  successMessage: string;
  returnTo: string;
}

interface CreditCardLinkLocationState {
  flow: 'credit-card-link';
  challenge: OtpChallenge;
  successMessage: string;
  returnTo: string;
  accountId: string;
}

interface AdminChangeLocationState {
  flow: 'admin-change';
  challenge: OtpChallenge;
  successMessage: string;
  returnTo: string;
}

type LocationState =
  | LoginLocationState
  | ProfileChangeLocationState
  | AccountChangeLocationState
  | AccountLinkLocationState
  | AccountOpeningLocationState
  | CreditCardLinkLocationState
  | AdminChangeLocationState;

const OtpVerification: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { token } = theme.useToken();
  const routeState = (location.state as LocationState | null) ?? null;
  const changeRouteState = routeState && 'flow' in routeState ? routeState : null;
  const loginRouteState = routeState && !('flow' in routeState) ? routeState : null;
  const profileChangeFlow = changeRouteState?.flow === 'profile-change';
  const accountChangeFlow = changeRouteState?.flow === 'account-change';
  const accountLinkFlow = changeRouteState?.flow === 'account-link';
  const accountOpeningFlow = changeRouteState?.flow === 'account-opening';
  const creditCardLinkFlow = changeRouteState?.flow === 'credit-card-link';
  const adminChangeFlow = changeRouteState?.flow === 'admin-change';
  const accountOrProfileChangeFlow =
    profileChangeFlow ||
    accountChangeFlow ||
    accountLinkFlow ||
    accountOpeningFlow ||
    creditCardLinkFlow ||
    adminChangeFlow;
  const preAuthToken = !accountOrProfileChangeFlow ? loginRouteState?.preAuthToken : undefined;
  const registrationFlow = !accountOrProfileChangeFlow && loginRouteState?.registration === true;
  const accountOrProfileChallenge = changeRouteState?.challenge ?? null;
  const changeFlowBackPath = accountOpeningFlow
    ? '/accounts/open'
    : creditCardLinkFlow
      ? '/accounts/cards/link'
      : accountLinkFlow
        ? '/accounts/link'
        : (changeRouteState?.returnTo ?? '/profile');

  const [otp, setOtp] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { secondsLeft, restart, isFinished } = useCountdown(RESEND_SECONDS);

  useEffect(() => {
    if (accountOrProfileChangeFlow && !accountOrProfileChallenge) {
      const fallbackPath = accountOpeningFlow
        ? '/accounts/open'
        : creditCardLinkFlow
          ? '/accounts/cards/link'
          : accountLinkFlow
            ? '/accounts/link'
            : '/profile';
      navigate(fallbackPath, { replace: true });
      return;
    }

    if (!accountOrProfileChangeFlow && !preAuthToken) {
      navigate('/login', { replace: true });
    }
  }, [
    preAuthToken,
    accountOrProfileChallenge,
    accountOrProfileChangeFlow,
    accountLinkFlow,
    accountOpeningFlow,
    creditCardLinkFlow,
    navigate,
  ]);

  const handleResend = async () => {
    // Only sign-up sends a code anywhere; every other flow reads it from the authenticator app.
    if (!registrationFlow) return;
    if (!isFinished) return;
    setOtp('');
    setError(null);
    try {
      if (preAuthToken) {
        await authService.resendOtp(preAuthToken, loginRouteState?.usernameOrEmail);
      }
      restart();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Failed to resend verification code'));
    }
  };

  const handleSubmit = async () => {
    if (otp.length !== OTP_LENGTH) return;
    setSubmitting(true);
    setError(null);
    try {
      if (profileChangeFlow && accountOrProfileChallenge) {
        await userService.confirmChange(accountOrProfileChallenge.changeRequestId, otp);
        navigate(changeRouteState.returnTo, {
          replace: true,
          state: { otpSuccessMessage: changeRouteState.successMessage },
        });
      } else if (accountChangeFlow && accountOrProfileChallenge) {
        await accountsService.confirmChange(accountOrProfileChallenge.changeRequestId, otp);
        navigate(changeRouteState.returnTo, {
          replace: true,
          state: { otpSuccessMessage: changeRouteState.successMessage },
        });
      } else if (accountLinkFlow && accountOrProfileChallenge) {
        const response = await accountsService.confirmAccountLink(
          accountOrProfileChallenge.changeRequestId,
          otp
        );
        accountSelection.setSelectedAccountId(response.account.id);
        navigate(changeRouteState.returnTo, {
          replace: true,
          state: { otpSuccessMessage: changeRouteState.successMessage },
        });
      } else if (accountOpeningFlow && accountOrProfileChallenge) {
        const response = await accountsService.confirmAccountOpening(
          accountOrProfileChallenge.changeRequestId,
          otp
        );
        accountSelection.setSelectedAccountId(response.account.id);
        navigate(changeRouteState.returnTo, {
          replace: true,
          state: { otpSuccessMessage: changeRouteState.successMessage },
        });
      } else if (creditCardLinkFlow && accountOrProfileChallenge) {
        await accountsService.confirmCreditCardLink(accountOrProfileChallenge.changeRequestId, otp);
        accountSelection.setSelectedAccountId(changeRouteState.accountId);
        navigate(changeRouteState.returnTo, {
          replace: true,
          state: { otpSuccessMessage: changeRouteState.successMessage },
        });
      } else if (adminChangeFlow && accountOrProfileChallenge) {
        await adminService.confirmChange(accountOrProfileChallenge.changeRequestId, otp);
        navigate(changeRouteState.returnTo, {
          replace: true,
          state: { otpSuccessMessage: changeRouteState.successMessage },
        });
      } else if (preAuthToken) {
        const response = await authService.verifyOtp({ preAuthToken, totpCode: otp });
        tokenStorage.setTokens(response.accessToken, response.refreshToken);
        const role = (response.role as Role) || 'CUSTOMER';
        sessionUser.set({
          userId: response.userId,
          username: response.username,
          role,
          status: response.status,
        });
        // Staff roles land on their own portal, never the customer dashboard.
        navigate(homePathForRole(role));
      }
    } catch (err) {
      if (accountOrProfileChangeFlow) {
        const axiosError = err as AxiosError<{ message?: string }>;
        setError(
          axiosError.response?.status === 404
            ? 'This verification request is no longer active. Go back and request a new code.'
            : getApiErrorMessage(err, 'Invalid or expired code. Please try again.')
        );
      } else {
        const axiosError = err as AxiosError<{ message?: string }>;
        setError(
          axiosError.response?.data?.message ?? 'Invalid or expired code. Please try again.'
        );
      }
    } finally {
      setSubmitting(false);
    }
  };

  if (!accountOrProfileChangeFlow && !preAuthToken) {
    return null;
  }

  return (
    <AuthLayout heading="Verify it's you">
      <Card
        styles={{ body: { padding: 32 } }}
        style={{ boxShadow: '0 8px 24px rgba(11, 27, 43, 0.06)' }}
      >
        {error && <Alert type="error" message={error} showIcon style={{ marginBottom: 24 }} />}

        <Text
          style={{
            display: 'block',
            marginBottom: 16,
            color: token.colorTextSecondary,
            fontSize: 13,
          }}
        >
          {accountOrProfileChangeFlow
            ? accountOrProfileChallenge?.message
            : registrationFlow
              ? 'Enter the six digit code we sent by SMS to your registered mobile number.'
              : 'Open your authenticator app and enter the current six digit code.'}
        </Text>

        <div className="otp-boxes">
          <Input.OTP
            length={OTP_LENGTH}
            value={otp}
            onInput={(cells) => setOtp(cells.join(''))}
            formatter={(value) => value.replace(/\D/g, '')}
            size="large"
            disabled={submitting}
            autoFocus
          />
        </div>

        <Flex style={{ marginTop: 20, marginBottom: 24 }}>
          {registrationFlow ? (
            <>
              <Text style={{ color: token.colorTextSecondary }}>Didn't get a code?&nbsp;</Text>
              {isFinished ? (
                <Link onClick={handleResend} style={{ color: token.colorPrimary }}>
                  Resend code
                </Link>
              ) : (
                <Text style={{ color: token.colorTextTertiary }}>
                  Resend in {formatCountdown(secondsLeft)}
                </Text>
              )}
            </>
          ) : accountOrProfileChangeFlow ? (
            <>
              <Text style={{ color: token.colorTextSecondary }}>Code not working?&nbsp;</Text>
              <Link
                onClick={() => navigate(changeFlowBackPath, { replace: true })}
                style={{ color: token.colorPrimary }}
              >
                Go back
              </Link>
            </>
          ) : (
            <Text style={{ color: token.colorTextSecondary }}>
              The code changes every 30 seconds. Wait for a fresh one if it is about to expire.
            </Text>
          )}
        </Flex>

        <Button
          type="primary"
          block
          loading={submitting}
          disabled={otp.length !== OTP_LENGTH}
          onClick={handleSubmit}
          style={{ fontWeight: 600 }}
        >
          Verify and continue
        </Button>
      </Card>

      <TrustIndicator
        text={
          accountOrProfileChangeFlow
            ? accountLinkFlow || creditCardLinkFlow
              ? 'SecureBank confirms account ownership with your authenticator app.'
              : 'SecureBank protects account changes with your authenticator app.'
            : registrationFlow
              ? 'This device will be remembered for 30 days'
              : 'SecureBank signs you in with your authenticator app.'
        }
      />
    </AuthLayout>
  );
};

export default OtpVerification;
