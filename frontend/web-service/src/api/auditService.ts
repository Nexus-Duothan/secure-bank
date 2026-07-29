import { createApiClient } from './createApiClient';

export const auditService = createApiClient('/api/v1/audit');

export default auditService;
