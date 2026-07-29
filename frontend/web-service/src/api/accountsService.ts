import { createApiClient } from './createApiClient';

export const accountsService = createApiClient('/api/v1/accounts');

export default accountsService;
