import { createApiClient } from './createApiClient';

export const lendingService = createApiClient('/api/v1/loans');

export default lendingService;
