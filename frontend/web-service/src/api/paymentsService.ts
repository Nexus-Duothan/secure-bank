import { createApiClient } from './createApiClient';

export const paymentsService = createApiClient('/api/v1/payments');

export default paymentsService;
